package org.haqdarshak.publishing.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.haqdarshak.publishing.domain.enums.VersionStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "scheme_versions", indexes = {
    @Index(name = "idx_version_scheme_id", columnList = "scheme_id"),
    @Index(name = "idx_version_status", columnList = "status")
})
public class SchemeVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id", nullable = false)
    private Scheme scheme;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VersionStatus status = VersionStatus.DRAFT;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "flagged_for_re_review", nullable = false)
    private boolean flaggedForReReview = false;

    @Column(name = "re_review_reason", columnDefinition = "TEXT")
    private String reReviewReason;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @OneToMany(mappedBy = "schemeVersion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SchemeField> fields = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "scheme_version_rules",
        joinColumns = @JoinColumn(name = "scheme_version_id"),
        inverseJoinColumns = @JoinColumn(name = "rule_id")
    )
    private Set<SharedEligibilityRule> sharedRules = new HashSet<>();

    public SchemeVersion() {
    }

    public SchemeVersion(Scheme scheme, Integer versionNumber, String createdBy) {
        this.scheme = scheme;
        this.versionNumber = versionNumber;
        this.createdBy = createdBy;
        this.lastModifiedBy = createdBy;
        this.status = VersionStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    public void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addField(SchemeField field) {
        fields.add(field);
        field.setSchemeVersion(this);
    }

    public void removeField(SchemeField field) {
        fields.remove(field);
        field.setSchemeVersion(null);
    }

    public void addSharedRule(SharedEligibilityRule rule) {
        this.sharedRules.add(rule);
        rule.getSchemeVersions().add(this);
    }

    public void removeSharedRule(SharedEligibilityRule rule) {
        this.sharedRules.remove(rule);
        rule.getSchemeVersions().remove(this);
    }

    public Long getId() {
        return id;
    }

    public Scheme getScheme() {
        return scheme;
    }

    public void setScheme(Scheme scheme) {
        this.scheme = scheme;
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

    public List<SchemeField> getFields() {
        return fields;
    }

    public void setFields(List<SchemeField> fields) {
        this.fields = fields;
    }

    public Set<SharedEligibilityRule> getSharedRules() {
        return sharedRules;
    }

    public void setSharedRules(Set<SharedEligibilityRule> sharedRules) {
        this.sharedRules = sharedRules;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SchemeVersion that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
