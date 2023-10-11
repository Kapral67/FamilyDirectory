package org.familydirectory.assets.ddb.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.NotNull;

public
enum SuffixType {
    JR("Jr"),
    SR("Sr");

    @NotNull
    private final String value;

    SuffixType (final @NotNull String value) {
        this.value = value;
    }

    @JsonCreator
    @NotNull
    public static
    SuffixType forValue (final @NotNull String value) {
        for (final SuffixType suffixType : values()) {
            if (suffixType.value()
                          .equalsIgnoreCase(value))
            {
                return suffixType;
            }
        }
        throw new IllegalArgumentException("Invalid SuffixType: " + value);
    }

    @JsonValue
    @NotNull
    public final
    String value () {
        return this.value;
    }
}
