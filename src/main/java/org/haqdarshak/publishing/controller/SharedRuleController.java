package org.haqdarshak.publishing.controller;

import jakarta.validation.Valid;
import org.haqdarshak.publishing.dto.request.SharedRuleRequest;
import org.haqdarshak.publishing.dto.response.ApiResponse;
import org.haqdarshak.publishing.dto.response.SharedRuleResponse;
import org.haqdarshak.publishing.service.SharedRuleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
public class SharedRuleController {

    private final SharedRuleService sharedRuleService;

    public SharedRuleController(SharedRuleService sharedRuleService) {
        this.sharedRuleService = sharedRuleService;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "anonymous";
    }

    /**
     * Create a new shared eligibility rule.
     * Central reviewer role required.
     */
    @PostMapping
    @PreAuthorize("hasRole('CENTRAL_REVIEWER')")
    public ResponseEntity<ApiResponse<SharedRuleResponse>> createRule(
            @Valid @RequestBody SharedRuleRequest request) {
        String username = getCurrentUsername();
        SharedRuleResponse response = sharedRuleService.createRule(request, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Shared eligibility rule created", response));
    }

    /**
     * Update an existing shared eligibility rule.
     * Flags all linked scheme versions for re-review without modifying live published data.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CENTRAL_REVIEWER')")
    public ResponseEntity<ApiResponse<SharedRuleResponse>> updateRule(
            @PathVariable("id") Long ruleId,
            @Valid @RequestBody SharedRuleRequest request) {
        String username = getCurrentUsername();
        SharedRuleResponse response = sharedRuleService.updateRule(ruleId, request, username);
        return ResponseEntity.ok(
                ApiResponse.ok("Shared rule updated. Associated scheme versions flagged for re-review.", response)
        );
    }

    /**
     * List all shared eligibility rules.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('FIELD_AGENT', 'STATE_REVIEWER', 'CENTRAL_REVIEWER')")
    public ResponseEntity<ApiResponse<List<SharedRuleResponse>>> getAllRules() {
        List<SharedRuleResponse> rules = sharedRuleService.getAllRules();
        return ResponseEntity.ok(ApiResponse.ok(rules));
    }

    /**
     * Get rule details by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FIELD_AGENT', 'STATE_REVIEWER', 'CENTRAL_REVIEWER')")
    public ResponseEntity<ApiResponse<SharedRuleResponse>> getRuleById(@PathVariable("id") Long ruleId) {
        SharedRuleResponse rule = sharedRuleService.getRuleById(ruleId);
        return ResponseEntity.ok(ApiResponse.ok(rule));
    }
}
