package org.haqdarshak.publishing.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_version_id", columnList = "scheme_version_id"),
    @Index(name = "idx_audit_scheme_ref", columnList = "scheme_ref")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheme_version_id", nullable = false)
    private Long schemeVersionId;

    @Column(name = "scheme_ref", nullable = false, length = 50)
    private String schemeRef;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "source_reference", length = 1000)
    private String sourceReference;

    public AuditLog() {
    }

    public AuditLog(Long schemeVersionId, String schemeRef, String fieldName,
                    String oldValue, String newValue, String changedBy,
                    Instant changedAt, String sourceReference) {
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
