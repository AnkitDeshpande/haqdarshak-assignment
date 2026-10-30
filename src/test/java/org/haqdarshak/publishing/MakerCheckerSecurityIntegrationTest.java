package org.haqdarshak.publishing;

import org.haqdarshak.publishing.domain.entity.Scheme;
import org.haqdarshak.publishing.domain.entity.SchemeVersion;
import org.haqdarshak.publishing.domain.enums.VersionStatus;
import org.haqdarshak.publishing.dto.request.CreateSchemeRequest;
import org.haqdarshak.publishing.dto.request.ReviewDecisionRequest;
import org.haqdarshak.publishing.dto.response.SchemeResponse;
import org.haqdarshak.publishing.dto.response.SchemeVersionResponse;
import org.haqdarshak.publishing.exception.MakerCheckerViolationException;
import org.haqdarshak.publishing.repository.SchemeRepository;
import org.haqdarshak.publishing.repository.SchemeVersionRepository;
import org.haqdarshak.publishing.service.SchemePublishingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class MakerCheckerSecurityIntegrationTest {

    @Autowired
    private SchemePublishingService schemePublishingService;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeVersionRepository schemeVersionRepository;

    @BeforeEach
    void setUp() {
        schemeRepository.deleteAll();
    }

    @Test
    @DisplayName("Test 1: Maker-Checker rule - Creator cannot approve/publish their own draft")
    void testMakerCheckerViolationWhenCreatorAttemptsApproval() {
        String creatorUsername = "state_reviewer_rahul";
        String differentReviewer = "central_reviewer_priya";

        // Step 1: Maker creates a new scheme (which initializes Draft Version 1)
        CreateSchemeRequest createRequest = new CreateSchemeRequest(
                "SCH_MAKER_TEST", "State Farmer Welfare Grant", "Agriculture", "Maharashtra"
        );
        SchemeResponse scheme = schemePublishingService.createScheme(createRequest, creatorUsername);
        Long schemeId = scheme.getId();
        Long version1Id = scheme.getVersionIds().get(0);

        // Step 2: Maker submits draft for review (DRAFT -> IN_REVIEW)
        schemePublishingService.submitDraftForReview(schemeId, version1Id, creatorUsername);

        // Verify status is now IN_REVIEW
        SchemeVersion inReviewVersion = schemeVersionRepository.findById(version1Id).orElseThrow();
        assertEquals(VersionStatus.IN_REVIEW, inReviewVersion.getStatus());
        assertEquals(creatorUsername, inReviewVersion.getCreatedBy());

        // Step 3: Maker attempts to approve/publish their own draft (even if pretending to have central reviewer role)
        ReviewDecisionRequest approveDecision = new ReviewDecisionRequest("APPROVE", "Self-approving my own scheme draft");

        SecurityException exception = assertThrows(
                MakerCheckerViolationException.class,
                () -> schemePublishingService.reviewAndDecide(schemeId, version1Id, approveDecision, creatorUsername),
                "Should throw MakerCheckerViolationException / SecurityException when maker attempts to approve"
        );

        assertTrue(exception.getMessage().contains("Maker-Checker Violation"));
        assertTrue(exception.getMessage().contains(creatorUsername));

        // Verify that the version status was NOT modified to PUBLISHED
        SchemeVersion stillInReview = schemeVersionRepository.findById(version1Id).orElseThrow();
        assertEquals(VersionStatus.IN_REVIEW, stillInReview.getStatus());
        assertNull(stillInReview.getApprovedBy());

        // Step 4: A DIFFERENT central reviewer approves the draft -> MUST SUCCEED
        SchemeVersionResponse approvedResponse = schemePublishingService.reviewAndDecide(
                schemeId, version1Id, approveDecision, differentReviewer
        );

        assertNotNull(approvedResponse);
        assertEquals(VersionStatus.PUBLISHED, approvedResponse.getStatus());
        assertEquals(differentReviewer, approvedResponse.getApprovedBy());

        // Verify scheme now points to this published version
        Scheme updatedScheme = schemeRepository.findById(schemeId).orElseThrow();
        assertEquals(version1Id, updatedScheme.getCurrentPublishedVersionId());
    }
}
