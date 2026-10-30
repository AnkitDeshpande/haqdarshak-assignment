package org.haqdarshak.publishing.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class SharedRuleRequest {

    @NotBlank(message = "rule_code is required")
    @JsonProperty("rule_code")
    private String ruleCode;

    @NotBlank(message = "rule_name is required")
    @JsonProperty("rule_name")
    private String ruleName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("criteria_json")
    private String criteriaJson;

    public SharedRuleRequest() {
    }

    public SharedRuleRequest(String ruleCode, String ruleName, String description, String criteriaJson) {
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.description = description;
        this.criteriaJson = criteriaJson;
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
}
