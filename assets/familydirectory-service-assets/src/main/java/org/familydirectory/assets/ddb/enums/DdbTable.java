package org.familydirectory.assets.ddb.enums;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.services.dynamodb.Attribute;
import static software.amazon.awscdk.services.dynamodb.AttributeType.STRING;

public
enum DdbTable {
    COGNITO("CognitoTableArn"),
    MEMBER("MemberTableArn"),
    FAMILY("FamilyTableArn");

    public static final Attribute PK = Attribute.builder()
                                                .name("id")
                                                .type(STRING)
                                                .build();
    @NotNull
    private final String arnExportName;

    DdbTable (final @NotNull String arnExportName) {
        this.arnExportName = arnExportName;
    }

    @NotNull
    public final
    String arnExportName () {
        return this.arnExportName;
    }
}
