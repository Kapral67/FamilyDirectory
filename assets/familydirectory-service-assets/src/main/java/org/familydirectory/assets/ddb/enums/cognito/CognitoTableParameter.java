package org.familydirectory.assets.ddb.enums.cognito;

import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.DdbType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import static software.amazon.awscdk.services.dynamodb.AttributeType.STRING;
import static software.amazon.awscdk.services.dynamodb.ProjectionType.KEYS_ONLY;

public
enum CognitoTableParameter {
    ID(DdbType.STR, DdbTable.PK.getName(), null),
    MEMBER(DdbType.STR, "member", GlobalSecondaryIndexProps.builder()
                                                           .indexName("CognitoMember")
                                                           .partitionKey(Attribute.builder()
                                                                                  .name("member")
                                                                                  .type(STRING)
                                                                                  .build())
                                                           .projectionType(KEYS_ONLY)
                                                           .build());

    @NotNull
    private final DdbType ddbType;

    @NotNull
    private final String jsonFieldName;

    @Nullable
    private final GlobalSecondaryIndexProps gsiProps;

    CognitoTableParameter (final @NotNull DdbType ddbType, final @NotNull String jsonFieldName, final @Nullable GlobalSecondaryIndexProps gsiProps) {
        this.ddbType = ddbType;
        this.jsonFieldName = jsonFieldName;
        this.gsiProps = gsiProps;
    }

    @NotNull
    public final
    DdbType ddbType () {
        return this.ddbType;
    }

    @NotNull
    public final
    String jsonFieldName () {
        return this.jsonFieldName;
    }

    @Nullable
    public final
    GlobalSecondaryIndexProps gsiProps () {
        return this.gsiProps;
    }
}
