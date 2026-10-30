package org.haqdarshak.publishing.controller;

import jakarta.validation.Valid;
import org.haqdarshak.publishing.dto.request.CreateSchemeRequest;
import org.haqdarshak.publishing.dto.request.ReviewDecisionRequest;
import org.haqdarshak.publishing.dto.request.UpdateFieldRequest;
import org.haqdarshak.publishing.dto.response.ApiResponse;
import org.haqdarshak.publishing.dto.response.AuditLogResponse;
import org.haqdarshak.publishing.dto.response.SchemeResponse;
import org.haqdarshak.publishing.dto.response.SchemeVersionResponse;
import org.haqdarshak.publishing.service.AuditService;
import org.haqdarshak.publishing.service.SchemePublishingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schemes")
public class SchemeController {

    private final SchemePublishingService schemePublishingService;
    private final AuditService auditService;

    public SchemeController(SchemePublishingService schemePublishingService,
                            AuditService auditService) {
        this.schemePublishingService = schemePublishingService;
        this.auditService = auditService;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "anonymous";
    }

    /**
     * Create a new scheme with initial draft version (v1).
     * Only STATE_REVIEWER can create drafts.
     */
    @PostMapping
    @PreAuthorize("hasRole('STATE_REVIEWER')")
    public ResponseEntity<ApiResponse<SchemeResponse>> createScheme(
            @Valid @RequestBody CreateSchemeRequest request) {
        String username = getCurrentUsername();
        SchemeResponse response = schemePublishingService.createScheme(request, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Scheme created with initial draft version", response));
    }

    /**
     * Update or add a field in the scheme's active draft.
     * If the scheme is currently published and has no open draft, a new draft version is branched.
     * Only STATE_REVIEWER can modify draft versions.
     */
    @PutMapping("/{id}/fields")
    @PreAuthorize("hasRole('STATE_REVIEWER')")
    public ResponseEntity<ApiResponse<SchemeVersionResponse>> updateField(
            @PathVariable("id") Long schemeId,
            @Valid @RequestBody UpdateFieldRequest request) {
        String username = getCurrentUsername();
        SchemeVersionResponse response = schemePublishingService.updateFieldInDraft(schemeId, request, username);
        return ResponseEntity.ok(ApiResponse.ok("Draft field updated and audit log recorded", response));
    }

    /**
     * Submit draft version for review (DRAFT -> IN_REVIEW).
     * Only STATE_REVIEWER can submit drafts.
     */
    @PostMapping("/{id}/versions/{versionId}/submit")
    @PreAuthorize("hasRole('STATE_REVIEWER')")
    public ResponseEntity<ApiResponse<SchemeVersionResponse>> submitForReview(
            @PathVariable("id") Long schemeId,
            @PathVariable("versionId") Long versionId) {
        String username = getCurrentUsername();
        SchemeVersionResponse response = schemePublishingService.submitDraftForReview(schemeId, versionId, username);
        return ResponseEntity.ok(ApiResponse.ok("Draft version submitted for review", response));
    }

    /**
     * Central reviewer reviews draft version and decides:
     * - APPROVE: transitions to PUBLISHED and updates current_published_version_id.
     * - REQUEST_CHANGES: transitions back to DRAFT with review comments.
     * HARD MAKER-CHECKER RULE: Fails if creator attempts approval.
     */
    @PostMapping("/{id}/versions/{versionId}/review")
    @PreAuthorize("hasRole('CENTRAL_REVIEWER')")
    public ResponseEntity<ApiResponse<SchemeVersionResponse>> reviewAndDecide(
            @PathVariable("id") Long schemeId,
            @PathVariable("versionId") Long versionId,
            @Valid @RequestBody ReviewDecisionRequest request) {
        String username = getCurrentUsername();
        SchemeVersionResponse response = schemePublishingService.reviewAndDecide(schemeId, versionId, request, username);
        return ResponseEntity.ok(ApiResponse.ok("Review decision processed: " + request.getAction(), response));
    }

    /**
     * Immutable Read API for field agents and public consumers.
     * Continuously serves the current_published_version_id completely isolated from concurrent draft changes.
     */
    @GetMapping("/{id}/live")
    @PreAuthorize("hasAnyRole('FIELD_AGENT', 'STATE_REVIEWER', 'CENTRAL_REVIEWER')")
    public ResponseEntity<ApiResponse<SchemeVersionResponse>> getLiveScheme(
            @PathVariable("id") Long schemeId) {
        SchemeVersionResponse response = schemePublishingService.getLivePublishedScheme(schemeId);
        return ResponseEntity.ok(ApiResponse.ok("Current live published scheme version", response));
    }

    /**
     * Immutable Read API by Scheme Ref (e.g., SCH_1001).
     */
    @GetMapping("/ref/{schemeRef}/live")
    @PreAuthorize("hasAnyRole('FIELD_AGENT', 'STATE_REVIEWER', 'CENTRAL_REVIEWER')")
    public ResponseEntity<ApiResponse<SchemeVersionResponse>> getLiveSchemeByRef(
            @PathVariable("schemeRef") String schemeRef) {
        SchemeVersionResponse response = schemePublishingService.getLivePublishedSchemeByRef(schemeRef);
        return ResponseEntity.ok(ApiResponse.ok("Current live published scheme version", response));
    }

    /**
     * Get specific version details (Draft, In-Review, or Published).
     */
    @GetMapping("/{id}/versions/{versionId}")
    @PreAuthorize("hasAnyRole('STATE_REVIEWER', 'CENTRAL_REVIEWER')")
    public ResponseEntity<ApiResponse<SchemeVersionResponse>> getVersion(
            @PathVariable("id") Long schemeId,
            @PathVariable("versionId") Long versionId) {
        SchemeVersionResponse response = schemePublishingService.getSchemeVersion(schemeId, versionId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * List all versions of a scheme.
     */
    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('STATE_REVIEWER', 'CENTRAL_REVIEWER')")
    public ResponseEntity<ApiResponse<List<SchemeVersionResponse>>> getAllVersions(
            @PathVariable("id") Long schemeId) {
        List<SchemeVersionResponse> versions = schemePublishingService.getAllVersionsForScheme(schemeId);
        return ResponseEntity.ok(ApiResponse.ok(versions));
    }

    /**
     * Get scheme metadata summary.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FIELD_AGENT', 'STATE_REVIEWER', 'CENTRAL_REVIEWER')")
    public ResponseEntity<ApiResponse<SchemeResponse>> getScheme(@PathVariable("id") Long schemeId) {
        SchemeResponse response = schemePublishingService.getSchemeById(schemeId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * List all schemes.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('FIELD_AGENT', 'STATE_REVIEWER', 'CENTRAL_REVIEWER')")
    public ResponseEntity<ApiResponse<List<SchemeResponse>>> getAllSchemes() {
        List<SchemeResponse> list = schemePublishingService.getAllSchemes();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    /**
     * Get audit logs for a specific scheme version.
     */
    @GetMapping("/{id}/versions/{versionId}/audit")
    @PreAuthorize("hasAnyRole('STATE_REVIEWER', 'CENTRAL_REVIEWER')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getVersionAuditLogs(
            @PathVariable("id") Long schemeId,
            @PathVariable("versionId") Long versionId) {
        List<AuditLogResponse> logs = auditService.getAuditLogsForVersion(versionId);
        return ResponseEntity.ok(ApiResponse.ok("Audit logs for version " + versionId, logs));
    }

    /**
     * Get audit logs across all versions of a scheme.
     */
    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('STATE_REVIEWER', 'CENTRAL_REVIEWER')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getSchemeAuditLogs(
            @PathVariable("id") Long schemeId) {
        SchemeResponse scheme = schemePublishingService.getSchemeById(schemeId);
        List<AuditLogResponse> logs = auditService.getAuditLogsForScheme(scheme.getSchemeRef());
        return ResponseEntity.ok(ApiResponse.ok("Audit logs for scheme " + scheme.getSchemeRef(), logs));
    }
}
