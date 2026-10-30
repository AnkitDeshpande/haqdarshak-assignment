package org.haqdarshak.publishing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.haqdarshak.publishing.domain.enums.VersionStatus;
import org.haqdarshak.publishing.dto.SchemeFieldDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SchemeVersionResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("scheme_ref")
    private String schemeRef;

    @JsonProperty("scheme_name")
    private String schemeName;

    @JsonProperty("version_number")
    private Integer versionNumber;

    @JsonProperty("status")
    private VersionStatus status;

    @JsonProperty("review_notes")
    private String reviewNotes;

    @JsonProperty("flagged_for_re_review")
    private boolean flaggedForReReview;

    @JsonProperty("re_review_reason")
    private String reReviewReason;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("last_modified_by")
    private String lastModifiedBy;

    @JsonProperty("approved_by")
    private String approvedBy;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("published_at")
    private Instant publishedAt;

    @JsonProperty("fields")
    private List<SchemeFieldDto> fields = new ArrayList<>();

    @JsonProperty("shared_rules")
    private List<SharedRuleResponse> sharedRules = new ArrayList<>();

    public SchemeVersionResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSchemeRef() {
        return schemeRef;
    }

    public void setSchemeRef(String schemeRef) {
        this.schemeRef = schemeRef;
    }

    public String getSchemeName() {
        return schemeName;
    }

    public void setSchemeName(String schemeName) {
        this.schemeName = schemeName;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public VersionStatus getStatus() {
        return status;
    }

    public void setStatus(VersionStatus status) {
        this.status = status;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public boolean isFlaggedForReReview() {
        return flaggedForReReview;
    }

    public void setFlaggedForReReview(boolean flaggedForReReview) {
        this.flaggedForReReview = flaggedForReReview;
    }

    public String getReReviewReason() {
        return reReviewReason;
    }

    public void setReReviewReason(String reReviewReason) {
        this.reReviewReason = reReviewReason;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public List<SchemeFieldDto> getFields() {
        return fields;
    }

    public void setFields(List<SchemeFieldDto> fields) {
        this.fields = fields;
    }

    public List<SharedRuleResponse> getSharedRules() {
        return sharedRules;
    }

    public void setSharedRules(List<SharedRuleResponse> sharedRules) {
        this.sharedRules = sharedRules;
    }
}
