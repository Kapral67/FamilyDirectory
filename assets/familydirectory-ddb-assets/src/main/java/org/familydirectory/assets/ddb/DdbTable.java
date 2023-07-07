package org.familydirectory.assets.ddb;

import software.amazon.awscdk.services.dynamodb.Attribute;

import static software.amazon.awscdk.services.dynamodb.AttributeType.STRING;

public enum DdbTable {
    MEMBERS(
            "MembersTableArn",
            Attribute.builder().name("fullName").type(STRING).build()
    ),
    FAMILIES("FamiliesTableArn",
            Attribute.builder().name("commonName").type(STRING).build()
    );

    /**
     * For MEMBERS: This is the sha256Hex hash of a Members fullName & birthdayString, concatenated. See
     * {@link org.familydirectory.assets.ddb.models.members.MembersModel} for how fullName and birthdayString are
     * computed
     */
    public static final Attribute PK = Attribute.builder().name("id").type(STRING).build();
    private final String arnExportName;
    private final Attribute sortKey;

    DdbTable(final String arnExportName, final Attribute sortKey) {
        this.arnExportName = arnExportName;
        this.sortKey = sortKey;
    }

    public final String arnExportName() {
        return this.arnExportName;
    }

    public final Attribute sortKey() {
        return this.sortKey;
    }
}
