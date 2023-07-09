package org.familydirectory.assets.ddb.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PhoneType {
    MOBILE, LANDLINE, WORK;

    @JsonCreator
    public static PhoneType forValue(final String value) {
        for (final PhoneType phoneType : values()) {
            if (phoneType.getJson().equalsIgnoreCase(value)) {
                return phoneType;
            }
        }
        throw new IllegalArgumentException("Invalid PhoneType: " + value);
    }

    @JsonValue
    public String getJson() {
        return this.name();
    }
}
