package org.familydirectory.cdk.ddb;

import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;

public enum DdbTable {
    MEMBERS("MembersTableArn", Attribute.builder().name("fullName").type(AttributeType.STRING).build()),
    FAMILIES("FamiliesTableArn", Attribute.builder().name("commonName").type(AttributeType.STRING).build());

    public static final Attribute PK = Attribute.builder().name("id").type(AttributeType.NUMBER).build();

    public final String arnExportName() { return this.arnExportName; }
    public final Attribute sortKey() { return this.sortKey; }

    DdbTable(final String arnExportName, final Attribute sortKey) {
        this.arnExportName = arnExportName;
        this.sortKey = sortKey;
    }

    private final String arnExportName;
    private final Attribute sortKey;
}
