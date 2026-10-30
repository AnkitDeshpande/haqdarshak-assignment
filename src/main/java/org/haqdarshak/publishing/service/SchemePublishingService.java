package org.haqdarshak.publishing.service;

import org.haqdarshak.publishing.dto.request.CreateSchemeRequest;
import org.haqdarshak.publishing.dto.request.ReviewDecisionRequest;
import org.haqdarshak.publishing.dto.request.UpdateFieldRequest;
import org.haqdarshak.publishing.dto.response.SchemeResponse;
import org.haqdarshak.publishing.dto.response.SchemeVersionResponse;

import java.util.List;

public interface SchemePublishingService {

    SchemeResponse createScheme(CreateSchemeRequest request, String currentUsername);

    SchemeVersionResponse updateFieldInDraft(Long schemeId, UpdateFieldRequest request, String currentUsername);

    SchemeVersionResponse submitDraftForReview(Long schemeId, Long versionId, String currentUsername);

    SchemeVersionResponse reviewAndDecide(Long schemeId, Long versionId, ReviewDecisionRequest request, String currentUsername);

    SchemeVersionResponse getLivePublishedScheme(Long schemeId);

    SchemeVersionResponse getLivePublishedSchemeByRef(String schemeRef);

    SchemeVersionResponse getSchemeVersion(Long schemeId, Long versionId);

    List<SchemeVersionResponse> getAllVersionsForScheme(Long schemeId);

    SchemeResponse getSchemeById(Long schemeId);

    List<SchemeResponse> getAllSchemes();
}
