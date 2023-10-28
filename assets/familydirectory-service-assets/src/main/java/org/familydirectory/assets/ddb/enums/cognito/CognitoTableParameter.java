package org.familydirectory.assets.ddb.enums.cognito;

import org.familydirectory.assets.ddb.enums.DdbType;
import org.familydirectory.assets.ddb.models.DdbTableParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import static software.amazon.awscdk.services.dynamodb.AttributeType.STRING;
import static software.amazon.awscdk.services.dynamodb.ProjectionType.KEYS_ONLY;

public
enum CognitoTableParameter implements DdbTableParameter {
    ID(DdbType.STR, DdbTableParameter.PK.getName(), null),
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

    @Override
    @NotNull
    public final
    DdbType ddbType () {
        return this.ddbType;
    }

    @Override
    @NotNull
    public final
    String jsonFieldName () {
        return this.jsonFieldName;
    }

    @Override
    @Nullable
    public final
    GlobalSecondaryIndexProps gsiProps () {
        return this.gsiProps;
    }
}
