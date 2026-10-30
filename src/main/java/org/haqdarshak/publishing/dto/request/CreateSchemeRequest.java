package org.haqdarshak.publishing.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import org.haqdarshak.publishing.dto.SchemeFieldDto;

import java.util.ArrayList;
import java.util.List;

public class CreateSchemeRequest {

    @NotBlank(message = "scheme_ref is required")
    @JsonProperty("scheme_ref")
    private String schemeRef;

    @NotBlank(message = "name is required")
    @JsonProperty("name")
    private String name;

    @JsonProperty("category")
    private String category;

    @JsonProperty("state")
    private String state;

    @JsonProperty("fields")
    private List<SchemeFieldDto> fields = new ArrayList<>();

    @JsonProperty("shared_rule_codes")
    private List<String> sharedRuleCodes = new ArrayList<>();

    public CreateSchemeRequest() {
    }

    public CreateSchemeRequest(String schemeRef, String name, String category, String state) {
        this.schemeRef = schemeRef;
        this.name = name;
        this.category = category;
        this.state = state;
    }

    public String getSchemeRef() {
        return schemeRef;
    }

    public void setSchemeRef(String schemeRef) {
        this.schemeRef = schemeRef;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<SchemeFieldDto> getFields() {
        return fields;
    }

    public void setFields(List<SchemeFieldDto> fields) {
        this.fields = fields;
    }

    public List<String> getSharedRuleCodes() {
        return sharedRuleCodes;
    }

    public void setSharedRuleCodes(List<String> sharedRuleCodes) {
        this.sharedRuleCodes = sharedRuleCodes;
    }
}
