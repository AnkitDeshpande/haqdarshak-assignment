package org.haqdarshak.publishing.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.haqdarshak.publishing.domain.enums.ConfidenceLevel;
import org.haqdarshak.publishing.domain.enums.FieldStatus;

import java.util.Objects;

@Entity
@Table(name = "scheme_fields", indexes = {
    @Index(name = "idx_field_version_id", columnList = "scheme_version_id"),
    @Index(name = "idx_field_name", columnList = "field_name")
})
public class SchemeField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_version_id", nullable = false)
    private SchemeVersion schemeVersion;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "field_value", columnDefinition = "TEXT")
    private String fieldValue;

    @Embedded
    private Evidence evidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence", nullable = false, length = 20)
    private ConfidenceLevel confidence = ConfidenceLevel.HIGH;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FieldStatus status = FieldStatus.FOUND;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    public SchemeField() {
    }

    public SchemeField(String fieldName, String fieldValue, Evidence evidence,
                       ConfidenceLevel confidence, FieldStatus status, String note) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.evidence = evidence;
        this.confidence = confidence != null ? confidence : ConfidenceLevel.HIGH;
        this.status = status != null ? status : FieldStatus.FOUND;
        this.note = note;
    }

    // Helper to clone a field into a new draft version
    public SchemeField copyForNewVersion(SchemeVersion newVersion) {
        SchemeField copy = new SchemeField();
        copy.setSchemeVersion(newVersion);
        copy.setFieldName(this.fieldName);
        copy.setFieldValue(this.fieldValue);
        if (this.evidence != null) {
            copy.setEvidence(new Evidence(
                    this.evidence.getText(),
                    this.evidence.getLocation(),
                    this.evidence.getSourceUrl(),
                    this.evidence.getCapturedDate()
            ));
        }
        copy.setConfidence(this.confidence);
        copy.setStatus(this.status);
        copy.setNote(this.note);
        return copy;
    }

    public Long getId() {
        return id;
    }

    public SchemeVersion getSchemeVersion() {
        return schemeVersion;
    }

    public void setSchemeVersion(SchemeVersion schemeVersion) {
        this.schemeVersion = schemeVersion;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    public Evidence getEvidence() {
        return evidence;
    }

    public void setEvidence(Evidence evidence) {
        this.evidence = evidence;
    }

    public ConfidenceLevel getConfidence() {
        return confidence;
    }

    public void setConfidence(ConfidenceLevel confidence) {
        this.confidence = confidence;
    }

    public FieldStatus getStatus() {
        return status;
    }

    public void setStatus(FieldStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SchemeField that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
