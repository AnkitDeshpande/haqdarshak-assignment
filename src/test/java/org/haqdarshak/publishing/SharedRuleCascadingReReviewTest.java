package org.haqdarshak.publishing;

import org.haqdarshak.publishing.domain.entity.Scheme;
import org.haqdarshak.publishing.domain.entity.SchemeVersion;
import org.haqdarshak.publishing.domain.entity.SharedEligibilityRule;
import org.haqdarshak.publishing.domain.enums.VersionStatus;
import org.haqdarshak.publishing.dto.request.CreateSchemeRequest;
import org.haqdarshak.publishing.dto.request.ReviewDecisionRequest;
import org.haqdarshak.publishing.dto.request.SharedRuleRequest;
import org.haqdarshak.publishing.dto.response.SchemeResponse;
import org.haqdarshak.publishing.dto.response.SharedRuleResponse;
import org.haqdarshak.publishing.repository.SchemeRepository;
import org.haqdarshak.publishing.repository.SchemeVersionRepository;
import org.haqdarshak.publishing.repository.SharedEligibilityRuleRepository;
import org.haqdarshak.publishing.service.SchemePublishingService;
import org.haqdarshak.publishing.service.SharedRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class SharedRuleCascadingReReviewTest {

    @Autowired
    private SharedRuleService sharedRuleService;

    @Autowired
    private SchemePublishingService schemePublishingService;

    @Autowired
    private SharedEligibilityRuleRepository sharedRuleRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeVersionRepository schemeVersionRepository;

    @BeforeEach
    void setUp() {
        schemeRepository.deleteAll();
        sharedRuleRepository.deleteAll();
    }

    @Test
    @DisplayName("Test 3: Updating a shared eligibility rule flags associated scheme drafts for review without altering live data")
    void testUpdatingSharedEligibilityRuleFlagsAssociatedVersions() {
        String adminUser = "central_admin";
        String stateReviewer = "state_user_1";
        String centralReviewer = "central_reviewer_1";

        // Step 1: Create a shared eligibility rule
        SharedRuleRequest ruleRequest = new SharedRuleRequest(
                "RULE_FARMER_LAND",
                "Land Ceiling Rule",
                "Max land allowed for subsidy eligibility",
                "{\"max_hectares\": 2.0}"
        );
        SharedRuleResponse createdRule = sharedRuleService.createRule(ruleRequest, adminUser);
        assertNotNull(createdRule);
        assertEquals(1, createdRule.getRuleVersion());

        // Step 2: Create a Scheme associated with this shared rule
        CreateSchemeRequest schemeRequest = new CreateSchemeRequest(
                "SCH_SHARED_RULE_TEST", "Small Farmer Tractor Subsidy", "Agriculture", "Punjab"
        );
        schemeRequest.setSharedRuleCodes(Collections.singletonList("RULE_FARMER_LAND"));
        SchemeResponse scheme = schemePublishingService.createScheme(schemeRequest, stateReviewer);
        Long schemeId = scheme.getId();
        Long v1Id = scheme.getVersionIds().get(0);

        // Verify SchemeVersion 1 is linked to the rule and not flagged for re-review initially
        SchemeVersion initialVersion = schemeVersionRepository.findById(v1Id).orElseThrow();
        assertFalse(initialVersion.isFlaggedForReReview());
        assertEquals(1, initialVersion.getSharedRules().size());

        // Step 3: Publish Scheme Version 1
        schemePublishingService.submitDraftForReview(schemeId, v1Id, stateReviewer);
        schemePublishingService.reviewAndDecide(schemeId, v1Id, new ReviewDecisionRequest("APPROVE", "Initial approval"), centralReviewer);

        // Step 4: Central admin updates the shared eligibility rule (e.g., changes max_hectares to 2.5)
        SharedRuleRequest updateRuleRequest = new SharedRuleRequest(
                "RULE_FARMER_LAND",
                "Land Ceiling Rule - Expanded",
                "Increased land ceiling to include semi-medium farmers",
                "{\"max_hectares\": 2.5}"
        );
        SharedRuleResponse updatedRule = sharedRuleService.updateRule(createdRule.getId(), updateRuleRequest, adminUser);
        assertEquals(2, updatedRule.getRuleVersion());

        // Step 5: Verify cascading effect on linked scheme version
        SchemeVersion linkedVersionAfterRuleUpdate = schemeVersionRepository.findById(v1Id).orElseThrow();
        assertTrue(linkedVersionAfterRuleUpdate.isFlaggedForReReview(),
                "Linked scheme version must be flagged for re-review");
        assertNotNull(linkedVersionAfterRuleUpdate.getReReviewReason());
        assertTrue(linkedVersionAfterRuleUpdate.getReReviewReason().contains("RULE_FARMER_LAND"));
        assertTrue(linkedVersionAfterRuleUpdate.getReReviewReason().contains("v2"));

        // Step 6: Verify LIVE published version data was NOT silently mutated
        Scheme publishedScheme = schemeRepository.findById(schemeId).orElseThrow();
        assertEquals(v1Id, publishedScheme.getCurrentPublishedVersionId());
        assertEquals(VersionStatus.PUBLISHED, linkedVersionAfterRuleUpdate.getStatus());
    }
}
