package org.haqdarshak.publishing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Evidence {

    @Column(name = "evidence_text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "evidence_location")
    private String location;

    @Column(name = "evidence_source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "evidence_captured_date")
    private String capturedDate;

    public Evidence() {
    }

    public Evidence(String text, String location, String sourceUrl, String capturedDate) {
        this.text = text;
        this.location = location;
        this.sourceUrl = sourceUrl;
        this.capturedDate = capturedDate;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getCapturedDate() {
        return capturedDate;
    }

    public void setCapturedDate(String capturedDate) {
        this.capturedDate = capturedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Evidence evidence)) return false;
        return Objects.equals(text, evidence.text) &&
               Objects.equals(location, evidence.location) &&
               Objects.equals(sourceUrl, evidence.sourceUrl) &&
               Objects.equals(capturedDate, evidence.capturedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, location, sourceUrl, capturedDate);
    }

    @Override
    public String toString() {
        return "Evidence{" +
                "text='" + text + '\'' +
                ", location='" + location + '\'' +
                ", sourceUrl='" + sourceUrl + '\'' +
                ", capturedDate='" + capturedDate + '\'' +
                '}';
    }
}
