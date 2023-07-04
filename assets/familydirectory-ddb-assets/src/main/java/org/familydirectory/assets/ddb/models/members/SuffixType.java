package org.familydirectory.assets.ddb.models.members;

public enum SuffixType {
    JR("Jr"),
    SR("Sr");

    private final String value;

    SuffixType(final String value) {
        this.value = value;
    }

    public final String value() { return this.value; }
}
