package org.familydirectory.assets.ddb.enums;

import software.amazon.awscdk.services.dynamodb.Attribute;
import static software.amazon.awscdk.services.dynamodb.AttributeType.STRING;

public
enum DdbTable {
    COGNITO("CognitoTableArn"),
    MEMBERS("MembersTableArn"),
    FAMILIES("FamiliesTableArn");

    public static final Attribute PK = Attribute.builder()
                                                .name("id")
                                                .type(STRING)
                                                .build();
    private final String arnExportName;

    DdbTable (final String arnExportName) {
        this.arnExportName = arnExportName;
    }

    public final
    String arnExportName () {
        return this.arnExportName;
    }
}
