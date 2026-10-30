package org.haqdarshak.publishing.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConfidenceLevel {
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low");

    private final String value;

    ConfidenceLevel(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ConfidenceLevel fromString(String key) {
        if (key == null) return null;
        for (ConfidenceLevel level : ConfidenceLevel.values()) {
            if (level.value.equalsIgnoreCase(key) || level.name().equalsIgnoreCase(key)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown confidence level: " + key);
    }
}
