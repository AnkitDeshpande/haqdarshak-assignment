package org.haqdarshak.publishing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class EvidenceDto {

    private String text;

    private String location;

    @JsonProperty("source_url")
    private String sourceUrl;

    @JsonProperty("captured_date")
    private String capturedDate;

    public EvidenceDto() {
    }

    public EvidenceDto(String text, String location, String sourceUrl, String capturedDate) {
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
}
