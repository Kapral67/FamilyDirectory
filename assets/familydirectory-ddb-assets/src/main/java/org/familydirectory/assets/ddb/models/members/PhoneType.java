package org.familydirectory.assets.ddb.models.members;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PhoneType {
    MOBILE,
    LANDLINE,
    WORK;

    @JsonValue
    public String getJson() {
        return this.name();
    }

    @JsonCreator
    public static PhoneType forValue(final String value) {
        for(final PhoneType phoneType : PhoneType.values()) {
            if(phoneType.getJson().equalsIgnoreCase(value)) {
                return phoneType;
            }
        }
        throw new IllegalArgumentException("Invalid PhoneType: " + value);
    }
}
