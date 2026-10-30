package org.haqdarshak.publishing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class SchemeFieldDto {

    @JsonProperty("scheme_ref")
    private String schemeRef;

    @NotBlank(message = "Field name is required")
    @JsonProperty("field")
    private String field;

    @JsonProperty("value")
    private String value;

    @JsonProperty("evidence")
    private EvidenceDto evidence;

    @JsonProperty("confidence")
    private String confidence;

    @JsonProperty("status")
    private String status;

    @JsonProperty("note")
    private String note;

    public SchemeFieldDto() {
    }

    public SchemeFieldDto(String schemeRef, String field, String value,
                          EvidenceDto evidence, String confidence, String status, String note) {
        this.schemeRef = schemeRef;
        this.field = field;
        this.value = value;
        this.evidence = evidence;
        this.confidence = confidence;
        this.status = status;
        this.note = note;
    }

    public String getSchemeRef() {
        return schemeRef;
    }

    public void setSchemeRef(String schemeRef) {
        this.schemeRef = schemeRef;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public EvidenceDto getEvidence() {
        return evidence;
    }

    public void setEvidence(EvidenceDto evidence) {
        this.evidence = evidence;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
