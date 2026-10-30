package org.haqdarshak.publishing.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "shared_eligibility_rules", indexes = {
    @Index(name = "idx_rule_code", columnList = "rule_code", unique = true)
})
public class SharedEligibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, unique = true, length = 100)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "criteria_json", columnDefinition = "TEXT")
    private String criteriaJson;

    @Column(name = "rule_version", nullable = false)
    private Integer ruleVersion = 1;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @JsonIgnore
    @ManyToMany(mappedBy = "sharedRules", fetch = FetchType.LAZY)
    private Set<SchemeVersion> schemeVersions = new HashSet<>();

    public SharedEligibilityRule() {
    }

    public SharedEligibilityRule(String ruleCode, String ruleName, String description, String criteriaJson) {
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.description = description;
        this.criteriaJson = criteriaJson;
        this.ruleVersion = 1;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCriteriaJson() {
        return criteriaJson;
    }

    public void setCriteriaJson(String criteriaJson) {
        this.criteriaJson = criteriaJson;
    }

    public Integer getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(Integer ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<SchemeVersion> getSchemeVersions() {
        return schemeVersions;
    }

    public void setSchemeVersions(Set<SchemeVersion> schemeVersions) {
        this.schemeVersions = schemeVersions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SharedEligibilityRule that)) return false;
        return Objects.equals(ruleCode, that.ruleCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleCode);
    }
}
