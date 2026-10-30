package org.haqdarshak.publishing.service;

import org.haqdarshak.publishing.domain.entity.Evidence;
import org.haqdarshak.publishing.domain.entity.Scheme;
import org.haqdarshak.publishing.domain.entity.SchemeField;
import org.haqdarshak.publishing.domain.entity.SchemeVersion;
import org.haqdarshak.publishing.domain.entity.SharedEligibilityRule;
import org.haqdarshak.publishing.domain.enums.ConfidenceLevel;
import org.haqdarshak.publishing.domain.enums.FieldStatus;
import org.haqdarshak.publishing.domain.enums.VersionStatus;
import org.haqdarshak.publishing.dto.EvidenceDto;
import org.haqdarshak.publishing.dto.SchemeFieldDto;
import org.haqdarshak.publishing.dto.request.CreateSchemeRequest;
import org.haqdarshak.publishing.dto.request.ReviewDecisionRequest;
import org.haqdarshak.publishing.dto.request.UpdateFieldRequest;
import org.haqdarshak.publishing.dto.response.SchemeResponse;
import org.haqdarshak.publishing.dto.response.SchemeVersionResponse;
import org.haqdarshak.publishing.dto.response.SharedRuleResponse;
import org.haqdarshak.publishing.exception.InvalidStateTransitionException;
import org.haqdarshak.publishing.exception.MakerCheckerViolationException;
import org.haqdarshak.publishing.exception.ResourceNotFoundException;
import org.haqdarshak.publishing.repository.SchemeFieldRepository;
import org.haqdarshak.publishing.repository.SchemeRepository;
import org.haqdarshak.publishing.repository.SchemeVersionRepository;
import org.haqdarshak.publishing.repository.SharedEligibilityRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchemePublishingServiceImpl implements SchemePublishingService {

    private static final Logger log = LoggerFactory.getLogger(SchemePublishingServiceImpl.class);

    private final SchemeRepository schemeRepository;
    private final SchemeVersionRepository schemeVersionRepository;
    private final SchemeFieldRepository schemeFieldRepository;
    private final SharedEligibilityRuleRepository sharedRuleRepository;
    private final AuditService auditService;

    public SchemePublishingServiceImpl(SchemeRepository schemeRepository,
                                      SchemeVersionRepository schemeVersionRepository,
                                      SchemeFieldRepository schemeFieldRepository,
                                      SharedEligibilityRuleRepository sharedRuleRepository,
                                      AuditService auditService) {
        this.schemeRepository = schemeRepository;
        this.schemeVersionRepository = schemeVersionRepository;
        this.schemeFieldRepository = schemeFieldRepository;
        this.sharedRuleRepository = sharedRuleRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public SchemeResponse createScheme(CreateSchemeRequest request, String currentUsername) {
        if (schemeRepository.existsBySchemeRef(request.getSchemeRef())) {
            throw new IllegalArgumentException("Scheme with ref '" + request.getSchemeRef() + "' already exists");
        }

        Scheme scheme = new Scheme(
                request.getSchemeRef(),
                request.getName(),
                request.getCategory(),
                request.getState()
        );

        // Initial draft version (v1)
        SchemeVersion initialDraft = new SchemeVersion(scheme, 1, currentUsername);
        scheme.addVersion(initialDraft);

        // Attach shared rules if specified
        if (request.getSharedRuleCodes() != null && !request.getSharedRuleCodes().isEmpty()) {
            for (String ruleCode : request.getSharedRuleCodes()) {
                sharedRuleRepository.findByRuleCode(ruleCode)
                        .ifPresent(initialDraft::addSharedRule);
            }
        }

        // Add initial fields
        if (request.getFields() != null) {
            for (SchemeFieldDto fieldDto : request.getFields()) {
                SchemeField field = toEntity(fieldDto);
                initialDraft.addField(field);
            }
        }

        Scheme savedScheme = schemeRepository.save(scheme);

        // Record audit logs for initial fields
        for (SchemeField field : initialDraft.getFields()) {
            auditService.recordChange(
                    initialDraft.getId(),
                    savedScheme.getSchemeRef(),
                    field.getFieldName(),
                    null,
                    field.getFieldValue(),
                    currentUsername,
                    "Initial draft version creation"
            );
        }

        log.info("Created new scheme '{}' with initial DRAFT version 1 by '{}'",
                savedScheme.getSchemeRef(), currentUsername);
        return toSchemeResponse(savedScheme);
    }

    @Override
    @Transactional
    public SchemeVersionResponse updateFieldInDraft(Long schemeId, UpdateFieldRequest request, String currentUsername) {
        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with ID: " + schemeId));

        // Find existing draft version or prepare a new draft from the latest published version
        List<SchemeVersion> drafts = schemeVersionRepository.findBySchemeIdAndStatus(schemeId, VersionStatus.DRAFT);
        SchemeVersion draftVersion;

        if (!drafts.isEmpty()) {
            // Edit existing draft
            draftVersion = drafts.get(0);
        } else {
            // Check if there is an in-review version
            List<SchemeVersion> inReviewVersions = schemeVersionRepository.findBySchemeIdAndStatus(schemeId, VersionStatus.IN_REVIEW);
            if (!inReviewVersions.isEmpty()) {
                throw new InvalidStateTransitionException(
                        "Cannot edit scheme while version " + inReviewVersions.get(0).getVersionNumber() +
                        " is currently IN_REVIEW. It must be approved or sent back to DRAFT first.");
            }

            // Create a new DRAFT version branched from current published version
            Integer maxVersion = schemeVersionRepository.findMaxVersionNumberBySchemeId(schemeId);
            int newVersionNumber = (maxVersion != null ? maxVersion : 0) + 1;

            draftVersion = new SchemeVersion(scheme, newVersionNumber, currentUsername);

            // If a published version exists, copy over fields and shared rules
            if (scheme.getCurrentPublishedVersionId() != null) {
                SchemeVersion publishedVersion = schemeVersionRepository.findById(scheme.getCurrentPublishedVersionId())
                        .orElse(null);
                if (publishedVersion != null) {
                    for (SchemeField field : publishedVersion.getFields()) {
                        draftVersion.addField(field.copyForNewVersion(draftVersion));
                    }
                    for (SharedEligibilityRule rule : publishedVersion.getSharedRules()) {
                        draftVersion.addSharedRule(rule);
                    }
                }
            }

            scheme.addVersion(draftVersion);
            draftVersion = schemeVersionRepository.save(draftVersion);
            log.info("Branched new SchemeVersion {} (DRAFT) for scheme '{}' by '{}'",
                    newVersionNumber, scheme.getSchemeRef(), currentUsername);
        }

        // Apply field update
        String oldValue = null;
        Optional<SchemeField> existingFieldOpt = draftVersion.getFields().stream()
                .filter(f -> f.getFieldName().equalsIgnoreCase(request.getField()))
                .findFirst();

        SchemeField targetField;
        if (existingFieldOpt.isPresent()) {
            targetField = existingFieldOpt.get();
            oldValue = targetField.getFieldValue();
            targetField.setFieldValue(request.getValue());
            if (request.getEvidence() != null) {
                targetField.setEvidence(toEvidence(request.getEvidence()));
            }
            if (request.getConfidence() != null) {
                targetField.setConfidence(ConfidenceLevel.fromString(request.getConfidence()));
            }
            if (request.getStatus() != null) {
                targetField.setStatus(FieldStatus.fromString(request.getStatus()));
            }
            if (request.getNote() != null) {
                targetField.setNote(request.getNote());
            }
        } else {
            targetField = new SchemeField(
                    request.getField(),
                    request.getValue(),
                    request.getEvidence() != null ? toEvidence(request.getEvidence()) : null,
                    request.getConfidence() != null ? ConfidenceLevel.fromString(request.getConfidence()) : ConfidenceLevel.HIGH,
                    request.getStatus() != null ? FieldStatus.fromString(request.getStatus()) : FieldStatus.FOUND,
                    request.getNote()
            );
            draftVersion.addField(targetField);
        }

        draftVersion.setLastModifiedBy(currentUsername);
        draftVersion.setUpdatedAt(Instant.now());
        SchemeVersion savedVersion = schemeVersionRepository.save(draftVersion);

        // Record audit diff
        auditService.recordChange(
                savedVersion.getId(),
                scheme.getSchemeRef(),
                request.getField(),
                oldValue,
                request.getValue(),
                currentUsername,
                request.getSourceReference() != null ? request.getSourceReference() : "Direct draft modification"
        );

        return toVersionResponse(savedVersion);
    }

    @Override
    @Transactional
    public SchemeVersionResponse submitDraftForReview(Long schemeId, Long versionId, String currentUsername) {
        SchemeVersion version = getVersionAndValidateBelongsToScheme(schemeId, versionId);

        if (version.getStatus() != VersionStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Only DRAFT versions can be submitted for review. Current status: " + version.getStatus());
        }

        version.setStatus(VersionStatus.IN_REVIEW);
        version.setLastModifiedBy(currentUsername);
        version.setUpdatedAt(Instant.now());

        SchemeVersion saved = schemeVersionRepository.save(version);
        log.info("SchemeVersion {} of scheme '{}' submitted for review by '{}'",
                saved.getVersionNumber(), saved.getScheme().getSchemeRef(), currentUsername);
        return toVersionResponse(saved);
    }

    @Override
    @Transactional
    public SchemeVersionResponse reviewAndDecide(Long schemeId, Long versionId,
                                                ReviewDecisionRequest request, String currentUsername) {
        SchemeVersion version = getVersionAndValidateBelongsToScheme(schemeId, versionId);

        if (version.getStatus() != VersionStatus.IN_REVIEW) {
            throw new InvalidStateTransitionException(
                    "Only IN_REVIEW versions can be approved or rejected. Current status: " + version.getStatus());
        }

        // HARD MAKER-CHECKER RULE ENFORCEMENT:
        // The user who created or edited the draft CANNOT approve or publish it
        if (currentUsername.equalsIgnoreCase(version.getCreatedBy()) ||
            (version.getLastModifiedBy() != null && currentUsername.equalsIgnoreCase(version.getLastModifiedBy()))) {
            log.error("Maker-Checker Violation: User '{}' created/modified version {} of scheme '{}' and attempted approval.",
                    currentUsername, version.getVersionNumber(), version.getScheme().getSchemeRef());
            throw new MakerCheckerViolationException(
                    "Maker-Checker Violation: User '" + currentUsername +
                    "' created or last modified draft version " + version.getVersionNumber() +
                    ". A different CENTRAL_REVIEWER must review and publish this scheme.");
        }

        String action = request.getAction().trim().toUpperCase();
        if ("APPROVE".equals(action)) {
            // State Machine Transition: IN_REVIEW -> PUBLISHED
            version.setStatus(VersionStatus.PUBLISHED);
            version.setApprovedBy(currentUsername);
            version.setPublishedAt(Instant.now());
            version.setReviewNotes(request.getNotes());
            version.setFlaggedForReReview(false);
            version.setReReviewReason(null);

            // Update Scheme's current_published_version_id pointer
            Scheme scheme = version.getScheme();
            scheme.setCurrentPublishedVersionId(version.getId());
            schemeRepository.save(scheme);

            SchemeVersion saved = schemeVersionRepository.save(version);
            log.info("SchemeVersion {} of scheme '{}' APPROVED and PUBLISHED by '{}'",
                    saved.getVersionNumber(), scheme.getSchemeRef(), currentUsername);
            return toVersionResponse(saved);

        } else if ("REQUEST_CHANGES".equals(action) || "REJECT".equals(action)) {
            // State Machine Transition: IN_REVIEW -> DRAFT
            version.setStatus(VersionStatus.DRAFT);
            version.setReviewNotes(request.getNotes());
            version.setLastModifiedBy(currentUsername);
            version.setUpdatedAt(Instant.now());

            SchemeVersion saved = schemeVersionRepository.save(version);
            log.info("SchemeVersion {} of scheme '{}' sent back to DRAFT with review notes by '{}'",
                    saved.getVersionNumber(), saved.getScheme().getSchemeRef(), currentUsername);
            return toVersionResponse(saved);
        } else {
            throw new IllegalArgumentException("Unknown review action: " + request.getAction() +
                    ". Allowed actions are: APPROVE, REQUEST_CHANGES");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SchemeVersionResponse getLivePublishedScheme(Long schemeId) {
        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with ID: " + schemeId));

        Long publishedVersionId = scheme.getCurrentPublishedVersionId();
        if (publishedVersionId == null) {
            throw new ResourceNotFoundException(
                    "No published version exists yet for scheme ref: " + scheme.getSchemeRef());
        }

        SchemeVersion liveVersion = schemeVersionRepository.findById(publishedVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("Published version with ID " + publishedVersionId + " not found"));

        return toVersionResponse(liveVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public SchemeVersionResponse getLivePublishedSchemeByRef(String schemeRef) {
        Scheme scheme = schemeRepository.findBySchemeRef(schemeRef)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with ref: " + schemeRef));

        return getLivePublishedScheme(scheme.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public SchemeVersionResponse getSchemeVersion(Long schemeId, Long versionId) {
        SchemeVersion version = getVersionAndValidateBelongsToScheme(schemeId, versionId);
        return toVersionResponse(version);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchemeVersionResponse> getAllVersionsForScheme(Long schemeId) {
        if (!schemeRepository.existsById(schemeId)) {
            throw new ResourceNotFoundException("Scheme not found with ID: " + schemeId);
        }
        return schemeVersionRepository.findBySchemeId(schemeId).stream()
                .map(this::toVersionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SchemeResponse getSchemeById(Long schemeId) {
        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with ID: " + schemeId));
        return toSchemeResponse(scheme);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchemeResponse> getAllSchemes() {
        return schemeRepository.findAll().stream()
                .map(this::toSchemeResponse)
                .collect(Collectors.toList());
    }

    private SchemeVersion getVersionAndValidateBelongsToScheme(Long schemeId, Long versionId) {
        SchemeVersion version = schemeVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme version not found with ID: " + versionId));

        if (!version.getScheme().getId().equals(schemeId)) {
            throw new IllegalArgumentException(
                    "Version ID " + versionId + " does not belong to scheme ID " + schemeId);
        }
        return version;
    }

    private SchemeResponse toSchemeResponse(Scheme scheme) {
        SchemeResponse response = new SchemeResponse();
        response.setId(scheme.getId());
        response.setSchemeRef(scheme.getSchemeRef());
        response.setName(scheme.getName());
        response.setCategory(scheme.getCategory());
        response.setState(scheme.getState());
        response.setCurrentPublishedVersionId(scheme.getCurrentPublishedVersionId());
        response.setCreatedAt(scheme.getCreatedAt());
        response.setUpdatedAt(scheme.getUpdatedAt());

        if (scheme.getVersions() != null) {
            response.setVersionIds(
                    scheme.getVersions().stream().map(SchemeVersion::getId).collect(Collectors.toList())
            );
        }
        return response;
    }

    private SchemeVersionResponse toVersionResponse(SchemeVersion version) {
        SchemeVersionResponse response = new SchemeVersionResponse();
        response.setId(version.getId());
        response.setSchemeRef(version.getScheme().getSchemeRef());
        response.setSchemeName(version.getScheme().getName());
        response.setVersionNumber(version.getVersionNumber());
        response.setStatus(version.getStatus());
        response.setReviewNotes(version.getReviewNotes());
        response.setFlaggedForReReview(version.isFlaggedForReReview());
        response.setReReviewReason(version.getReReviewReason());
        response.setCreatedBy(version.getCreatedBy());
        response.setLastModifiedBy(version.getLastModifiedBy());
        response.setApprovedBy(version.getApprovedBy());
        response.setCreatedAt(version.getCreatedAt());
        response.setUpdatedAt(version.getUpdatedAt());
        response.setPublishedAt(version.getPublishedAt());

        // Map fields strictly adhering to JSON schema
        if (version.getFields() != null) {
            response.setFields(
                    version.getFields().stream().map(f -> toFieldDto(f, version.getScheme().getSchemeRef()))
                            .collect(Collectors.toList())
            );
        }

        // Map shared rules
        if (version.getSharedRules() != null) {
            response.setSharedRules(
                    version.getSharedRules().stream().map(this::toRuleResponse)
                            .collect(Collectors.toList())
            );
        }

        return response;
    }

    private SchemeFieldDto toFieldDto(SchemeField field, String schemeRef) {
        EvidenceDto evidenceDto = null;
        if (field.getEvidence() != null) {
            evidenceDto = new EvidenceDto(
                    field.getEvidence().getText(),
                    field.getEvidence().getLocation(),
                    field.getEvidence().getSourceUrl(),
                    field.getEvidence().getCapturedDate()
            );
        }

        return new SchemeFieldDto(
                schemeRef,
                field.getFieldName(),
                field.getFieldValue(),
                evidenceDto,
                field.getConfidence() != null ? field.getConfidence().getValue() : null,
                field.getStatus() != null ? field.getStatus().getValue() : null,
                field.getNote()
        );
    }

    private SchemeField toEntity(SchemeFieldDto dto) {
        Evidence evidence = null;
        if (dto.getEvidence() != null) {
            evidence = new Evidence(
                    dto.getEvidence().getText(),
                    dto.getEvidence().getLocation(),
                    dto.getEvidence().getSourceUrl(),
                    dto.getEvidence().getCapturedDate()
            );
        }

        ConfidenceLevel confidence = dto.getConfidence() != null ?
                ConfidenceLevel.fromString(dto.getConfidence()) : ConfidenceLevel.HIGH;

        FieldStatus status = dto.getStatus() != null ?
                FieldStatus.fromString(dto.getStatus()) : FieldStatus.FOUND;

        return new SchemeField(
                dto.getField(),
                dto.getValue(),
                evidence,
                confidence,
                status,
                dto.getNote()
        );
    }

    private Evidence toEvidence(EvidenceDto dto) {
        return new Evidence(
                dto.getText(),
                dto.getLocation(),
                dto.getSourceUrl(),
                dto.getCapturedDate()
        );
    }

    private SharedRuleResponse toRuleResponse(SharedEligibilityRule rule) {
        return new SharedRuleResponse(
                rule.getId(),
                rule.getRuleCode(),
                rule.getRuleName(),
                rule.getDescription(),
                rule.getCriteriaJson(),
                rule.getRuleVersion(),
                rule.getUpdatedBy(),
                rule.getUpdatedAt()
        );
    }
}
