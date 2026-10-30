package org.haqdarshak.publishing;

import org.haqdarshak.publishing.domain.entity.Scheme;
import org.haqdarshak.publishing.domain.enums.VersionStatus;
import org.haqdarshak.publishing.dto.EvidenceDto;
import org.haqdarshak.publishing.dto.SchemeFieldDto;
import org.haqdarshak.publishing.dto.request.CreateSchemeRequest;
import org.haqdarshak.publishing.dto.request.ReviewDecisionRequest;
import org.haqdarshak.publishing.dto.request.UpdateFieldRequest;
import org.haqdarshak.publishing.dto.response.SchemeResponse;
import org.haqdarshak.publishing.dto.response.SchemeVersionResponse;
import org.haqdarshak.publishing.repository.SchemeRepository;
import org.haqdarshak.publishing.service.SchemePublishingService;
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
public class ImmutableReadVersionIsolationTest {

    @Autowired
    private SchemePublishingService schemePublishingService;

    @Autowired
    private SchemeRepository schemeRepository;

    @BeforeEach
    void setUp() {
        schemeRepository.deleteAll();
    }

    @Test
    @DisplayName("Test 2: Field agents observe uninterrupted published version while draft is edited")
    void testFieldAgentsObserveUninterruptedPublishedVersionWhileDraftEdited() {
        String makerUser = "state_maker_1";
        String checkerUser = "central_checker_1";
        String editorUser = "state_maker_2";

        // Step 1: Create Scheme with benefit_amount = 6000
        CreateSchemeRequest createRequest = new CreateSchemeRequest(
                "SCH_LIVE_TEST", "National Krishi Support", "Agriculture", "Central"
        );
        SchemeFieldDto initialField = new SchemeFieldDto(
                "SCH_LIVE_TEST", "benefit_amount", "6000",
                new EvidenceDto("Rs. 6000 per annum", "Sec 1", "https://gov.in", "2026-10-30"),
                "high", "found", "Official gazette"
        );
        createRequest.setFields(Collections.singletonList(initialField));

        SchemeResponse created = schemePublishingService.createScheme(createRequest, makerUser);
        Long schemeId = created.getId();
        Long v1Id = created.getVersionIds().get(0);

        // Step 2: Maker submits v1 and Checker approves it -> Version 1 is PUBLISHED
        schemePublishingService.submitDraftForReview(schemeId, v1Id, makerUser);
        schemePublishingService.reviewAndDecide(schemeId, v1Id, new ReviewDecisionRequest("APPROVE", "Looks good"), checkerUser);

        // Step 3: Verify Field Agent live read sees Version 1 with value = 6000
        SchemeVersionResponse liveBeforeEdit = schemePublishingService.getLivePublishedScheme(schemeId);
        assertNotNull(liveBeforeEdit);
        assertEquals(VersionStatus.PUBLISHED, liveBeforeEdit.getStatus());
        assertEquals(1, liveBeforeEdit.getVersionNumber());
        assertEquals("6000", liveBeforeEdit.getFields().stream()
                .filter(f -> f.getField().equals("benefit_amount"))
                .findFirst().orElseThrow().getValue());

        // Step 4: Another state reviewer begins editing the scheme (updates benefit_amount to 9000)
        // This triggers the versioning branch: creating a new SchemeVersion (v2) in DRAFT state
        UpdateFieldRequest updateRequest = new UpdateFieldRequest(
                "benefit_amount", "9000",
                new EvidenceDto("Revised to Rs. 9000 per annum", "Sec 2.4", "https://gov.in/revised", "2026-11-01"),
                "high", "found", "Cabinet amendment", "Cabinet memo #44"
        );
        SchemeVersionResponse draftV2 = schemePublishingService.updateFieldInDraft(schemeId, updateRequest, editorUser);

        // Verify Draft V2 was created with new value 9000
        assertNotNull(draftV2);
        assertEquals(2, draftV2.getVersionNumber());
        assertEquals(VersionStatus.DRAFT, draftV2.getStatus());
        assertEquals("9000", draftV2.getFields().stream()
                .filter(f -> f.getField().equals("benefit_amount"))
                .findFirst().orElseThrow().getValue());

        // Step 5: CRITICAL CHECK - Field Agent queries GET /api/schemes/{id}/live
        // MUST STILL return Version 1 with benefit_amount = 6000! Uninterrupted read isolation!
        SchemeVersionResponse liveDuringDraftEdit = schemePublishingService.getLivePublishedScheme(schemeId);
        assertNotNull(liveDuringDraftEdit);
        assertEquals(1, liveDuringDraftEdit.getVersionNumber());
        assertEquals(VersionStatus.PUBLISHED, liveDuringDraftEdit.getStatus());
        assertEquals("6000", liveDuringDraftEdit.getFields().stream()
                .filter(f -> f.getField().equals("benefit_amount"))
                .findFirst().orElseThrow().getValue());

        // Step 6: Draft v2 is submitted for review
        schemePublishingService.submitDraftForReview(schemeId, draftV2.getId(), editorUser);

        // Live read still untouched during IN_REVIEW state
        SchemeVersionResponse liveDuringInReview = schemePublishingService.getLivePublishedScheme(schemeId);
        assertEquals(1, liveDuringInReview.getVersionNumber());
        assertEquals("6000", liveDuringInReview.getFields().stream()
                .filter(f -> f.getField().equals("benefit_amount"))
                .findFirst().orElseThrow().getValue());

        // Step 7: Central Reviewer approves Draft V2 -> Now live pointer updates to v2
        schemePublishingService.reviewAndDecide(schemeId, draftV2.getId(), new ReviewDecisionRequest("APPROVE", "Approved revision"), checkerUser);

        SchemeVersionResponse liveAfterApproval = schemePublishingService.getLivePublishedScheme(schemeId);
        assertEquals(2, liveAfterApproval.getVersionNumber());
        assertEquals(VersionStatus.PUBLISHED, liveAfterApproval.getStatus());
        assertEquals("9000", liveAfterApproval.getFields().stream()
                .filter(f -> f.getField().equals("benefit_amount"))
                .findFirst().orElseThrow().getValue());
    }
}
