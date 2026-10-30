package org.haqdarshak.publishing.service;

import org.haqdarshak.publishing.domain.entity.SchemeVersion;
import org.haqdarshak.publishing.domain.entity.SharedEligibilityRule;
import org.haqdarshak.publishing.dto.request.SharedRuleRequest;
import org.haqdarshak.publishing.dto.response.SharedRuleResponse;
import org.haqdarshak.publishing.exception.ResourceNotFoundException;
import org.haqdarshak.publishing.repository.SchemeVersionRepository;
import org.haqdarshak.publishing.repository.SharedEligibilityRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SharedRuleServiceImpl implements SharedRuleService {

    private static final Logger log = LoggerFactory.getLogger(SharedRuleServiceImpl.class);

    private final SharedEligibilityRuleRepository sharedRuleRepository;
    private final SchemeVersionRepository schemeVersionRepository;
    private final AuditService auditService;

    public SharedRuleServiceImpl(SharedEligibilityRuleRepository sharedRuleRepository,
                                 SchemeVersionRepository schemeVersionRepository,
                                 AuditService auditService) {
        this.sharedRuleRepository = sharedRuleRepository;
        this.schemeVersionRepository = schemeVersionRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public SharedRuleResponse createRule(SharedRuleRequest request, String currentUsername) {
        if (sharedRuleRepository.existsByRuleCode(request.getRuleCode())) {
            throw new IllegalArgumentException("Rule with code '" + request.getRuleCode() + "' already exists");
        }

        SharedEligibilityRule rule = new SharedEligibilityRule(
                request.getRuleCode(),
                request.getRuleName(),
                request.getDescription(),
                request.getCriteriaJson()
        );
        rule.setUpdatedBy(currentUsername);
        rule.setUpdatedAt(Instant.now());

        SharedEligibilityRule saved = sharedRuleRepository.save(rule);
        log.info("Created shared eligibility rule: {} by {}", saved.getRuleCode(), currentUsername);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SharedRuleResponse updateRule(Long ruleId, SharedRuleRequest request, String currentUsername) {
        SharedEligibilityRule rule = sharedRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Shared eligibility rule not found with ID: " + ruleId));

        String oldCriteria = rule.getCriteriaJson();
        int newVersion = rule.getRuleVersion() + 1;

        rule.setRuleName(request.getRuleName());
        rule.setDescription(request.getDescription());
        rule.setCriteriaJson(request.getCriteriaJson());
        rule.setRuleVersion(newVersion);
        rule.setUpdatedBy(currentUsername);
        rule.setUpdatedAt(Instant.now());

        sharedRuleRepository.save(rule);

        // Cascading re-review flag:
        // Find all linked scheme versions and flag them for re-review without silently altering live published data
        List<SchemeVersion> linkedVersions = schemeVersionRepository.findBySharedRuleId(rule.getId());
        for (SchemeVersion version : linkedVersions) {
            version.setFlaggedForReReview(true);
            version.setReReviewReason("Shared eligibility rule '" + rule.getRuleCode() +
                    "' was updated to v" + newVersion + " by " + currentUsername);
            schemeVersionRepository.save(version);

            auditService.recordChange(
                    version.getId(),
                    version.getScheme().getSchemeRef(),
                    "SHARED_RULE_" + rule.getRuleCode(),
                    oldCriteria,
                    request.getCriteriaJson(),
                    currentUsername,
                    "Rule v" + newVersion + " update triggered re-review flag"
            );
            log.info("Flagged SchemeVersion ID: {} (status: {}) for re-review due to rule update: {}",
                    version.getId(), version.getStatus(), rule.getRuleCode());
        }

        return toResponse(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SharedRuleResponse> getAllRules() {
        return sharedRuleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SharedRuleResponse getRuleById(Long ruleId) {
        SharedEligibilityRule rule = sharedRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Shared eligibility rule not found with ID: " + ruleId));
        return toResponse(rule);
    }

    private SharedRuleResponse toResponse(SharedEligibilityRule rule) {
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
