package org.haqdarshak.publishing.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class ReviewDecisionRequest {

    /**
     * Decision action: "APPROVE" (publishes) or "REQUEST_CHANGES" (sends back to DRAFT) or "REJECT".
     */
    @NotBlank(message = "Action is required (APPROVE or REQUEST_CHANGES)")
    @JsonProperty("action")
    private String action;

    @JsonProperty("notes")
    private String notes;

    public ReviewDecisionRequest() {
    }

    public ReviewDecisionRequest(String action, String notes) {
        this.action = action;
        this.notes = notes;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
