package org.familydirectory.assets.ddb.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.NotNull;

public
enum PhoneType {
    MOBILE,
    LANDLINE,
    WORK;

    @JsonCreator
    @NotNull
    public static
    PhoneType forValue (final @NotNull String value) {
        for (final PhoneType phoneType : values()) {
            if (phoneType.getJson()
                         .equalsIgnoreCase(value))
            {
                return phoneType;
            }
        }
        throw new IllegalArgumentException("Invalid PhoneType: " + value);
    }

    @JsonValue
    @NotNull
    public
    String getJson () {
        return this.name();
    }
}
