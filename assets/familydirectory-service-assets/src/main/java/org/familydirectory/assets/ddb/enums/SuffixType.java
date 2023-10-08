package org.familydirectory.assets.ddb.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SuffixType {
    JR("Jr"), SR("Sr");

    private final String value;

    SuffixType(final String value) {
        this.value = value;
    }

    @JsonCreator
    public static SuffixType forValue(final String value) {
        for (final SuffixType suffixType : values()) {
            if (suffixType.value().equalsIgnoreCase(value)) {
                return suffixType;
            }
        }
        throw new IllegalArgumentException("Invalid SuffixType: " + value);
    }

    @JsonValue
    public final String value() {
        return this.value;
    }
}
