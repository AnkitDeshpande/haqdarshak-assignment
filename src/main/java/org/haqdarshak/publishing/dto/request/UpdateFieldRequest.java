package org.haqdarshak.publishing.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import org.haqdarshak.publishing.dto.EvidenceDto;

public class UpdateFieldRequest {

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

    @JsonProperty("source_reference")
    private String sourceReference;

    public UpdateFieldRequest() {
    }

    public UpdateFieldRequest(String field, String value, EvidenceDto evidence,
                              String confidence, String status, String note, String sourceReference) {
        this.field = field;
        this.value = value;
        this.evidence = evidence;
        this.confidence = confidence;
        this.status = status;
        this.note = note;
        this.sourceReference = sourceReference;
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

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }
}
