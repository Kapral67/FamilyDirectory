package org.familydirectory.assets.ddb.enums.family;

import org.familydirectory.assets.ddb.enums.DdbType;
import org.familydirectory.assets.ddb.models.DdbTableParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;

public
enum FamilyTableParameter implements DdbTableParameter {
    ID(DdbType.STR, DdbTableParameter.PK.getName()),
    ANCESTOR(DdbType.STR, "ancestor"),
    SPOUSE(DdbType.STR, "spouse"),
    DESCENDANTS(DdbType.STR_SET, "descendants");

    @NotNull
    private final DdbType ddbType;
    @NotNull
    private final String jsonFieldName;

    FamilyTableParameter (final @NotNull DdbType ddbType, final @NotNull String jsonFieldName) {
        this.ddbType = ddbType;
        this.jsonFieldName = jsonFieldName;
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
        return null;
    }
}
