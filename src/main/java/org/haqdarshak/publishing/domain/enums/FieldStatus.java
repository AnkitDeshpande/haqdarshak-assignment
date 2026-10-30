package org.haqdarshak.publishing.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FieldStatus {
    FOUND("found"),
    NOT_FOUND("not found"),
    CONFLICT("conflict");

    private final String value;

    FieldStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static FieldStatus fromString(String key) {
        if (key == null) return null;
        String normalized = key.trim().toLowerCase().replace("_", " ");
        for (FieldStatus status : FieldStatus.values()) {
            if (status.value.equalsIgnoreCase(normalized) || status.name().equalsIgnoreCase(key)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown field status: " + key);
    }
}
