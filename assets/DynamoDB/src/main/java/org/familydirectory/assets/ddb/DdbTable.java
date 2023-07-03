package org.familydirectory.assets.ddb;

import software.amazon.awscdk.services.dynamodb.Attribute;

import static software.amazon.awscdk.services.dynamodb.AttributeType.NUMBER;
import static software.amazon.awscdk.services.dynamodb.AttributeType.STRING;

public enum DdbTable {
    MEMBERS("MembersTableArn", Attribute.builder().name("fullName").type(STRING).build()),
    FAMILIES("FamiliesTableArn", Attribute.builder().name("commonName").type(STRING).build());

    public static final Attribute PK = Attribute.builder().name("id").type(NUMBER).build();

    public final String arnExportName() { return this.arnExportName; }
    public final Attribute sortKey() { return this.sortKey; }

    DdbTable(final String arnExportName, final Attribute sortKey) {
        this.arnExportName = arnExportName;
        this.sortKey = sortKey;
    }

    private final String arnExportName;
    private final Attribute sortKey;
}
