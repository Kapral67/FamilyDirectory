package org.familydirectory.assets.ddb.models;

import org.familydirectory.assets.ddb.enums.DdbType;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import static software.amazon.awscdk.services.dynamodb.AttributeType.STRING;

public
interface DdbTableParameter {

    @NotNull Attribute PK = Attribute.builder()
                                     .name(DdbUtils.PK)
                                     .type(STRING)
                                     .build();

    @NotNull
    DdbType ddbType ();

    @NotNull
    String jsonFieldName ();

    @Nullable
    GlobalSecondaryIndexProps gsiProps ();
}
