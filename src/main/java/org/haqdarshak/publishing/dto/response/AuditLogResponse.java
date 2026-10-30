package org.haqdarshak.publishing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class AuditLogResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("scheme_version_id")
    private Long schemeVersionId;

    @JsonProperty("scheme_ref")
    private String schemeRef;

    @JsonProperty("field_name")
    private String fieldName;

    @JsonProperty("old_value")
    private String oldValue;

    @JsonProperty("new_value")
    private String newValue;

    @JsonProperty("changed_by")
    private String changedBy;

    @JsonProperty("changed_at")
    private Instant changedAt;

    @JsonProperty("source_reference")
    private String sourceReference;

    public AuditLogResponse() {
    }

    public AuditLogResponse(Long id, Long schemeVersionId, String schemeRef, String fieldName,
                            String oldValue, String newValue, String changedBy,
                            Instant changedAt, String sourceReference) {
        this.id = id;
        this.schemeVersionId = schemeVersionId;
        this.schemeRef = schemeRef;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.sourceReference = sourceReference;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSchemeVersionId() {
        return schemeVersionId;
    }

    public void setSchemeVersionId(Long schemeVersionId) {
        this.schemeVersionId = schemeVersionId;
    }

    public String getSchemeRef() {
        return schemeRef;
    }

    public void setSchemeRef(String schemeRef) {
        this.schemeRef = schemeRef;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }
}
