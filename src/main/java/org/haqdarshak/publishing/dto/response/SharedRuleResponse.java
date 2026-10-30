package org.haqdarshak.publishing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class SharedRuleResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("rule_code")
    private String ruleCode;

    @JsonProperty("rule_name")
    private String ruleName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("criteria_json")
    private String criteriaJson;

    @JsonProperty("rule_version")
    private Integer ruleVersion;

    @JsonProperty("updated_by")
    private String updatedBy;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    public SharedRuleResponse() {
    }

    public SharedRuleResponse(Long id, String ruleCode, String ruleName, String description,
                              String criteriaJson, Integer ruleVersion, String updatedBy, Instant updatedAt) {
        this.id = id;
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.description = description;
        this.criteriaJson = criteriaJson;
        this.ruleVersion = ruleVersion;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
