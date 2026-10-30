package org.haqdarshak.publishing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SchemeResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("scheme_ref")
    private String schemeRef;

    @JsonProperty("name")
    private String name;

    @JsonProperty("category")
    private String category;

    @JsonProperty("state")
    private String state;

    @JsonProperty("current_published_version_id")
    private Long currentPublishedVersionId;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("version_ids")
    private List<Long> versionIds = new ArrayList<>();

    public SchemeResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getCurrentPublishedVersionId() {
        return currentPublishedVersionId;
    }

    public void setCurrentPublishedVersionId(Long currentPublishedVersionId) {
        this.currentPublishedVersionId = currentPublishedVersionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Long> getVersionIds() {
        return versionIds;
    }

    public void setVersionIds(List<Long> versionIds) {
        this.versionIds = versionIds;
    }
}
