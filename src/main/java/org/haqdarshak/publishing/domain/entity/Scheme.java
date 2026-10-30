package org.haqdarshak.publishing.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "schemes", indexes = {
    @Index(name = "idx_scheme_ref", columnList = "scheme_ref", unique = true)
})
public class Scheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheme_ref", nullable = false, unique = true, length = 50)
    private String schemeRef;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "current_published_version_id")
    private Long currentPublishedVersionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SchemeVersion> versions = new ArrayList<>();

    public Scheme() {
    }

    public Scheme(String schemeRef, String name, String category, String state) {
        this.schemeRef = schemeRef;
        this.name = name;
        this.category = category;
        this.state = state;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    public void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addVersion(SchemeVersion version) {
        versions.add(version);
        version.setScheme(this);
    }

    public Long getId() {
        return id;
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

    public List<SchemeVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<SchemeVersion> versions) {
        this.versions = versions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Scheme scheme)) return false;
        return Objects.equals(schemeRef, scheme.schemeRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemeRef);
    }
}
