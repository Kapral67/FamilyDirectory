package org.familydirectory.assets.ddb.enums.sync;

import org.familydirectory.assets.ddb.enums.DdbType;
import org.familydirectory.assets.ddb.models.DdbTableParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;

public
enum SyncTableParameter implements DdbTableParameter {
    ID(DdbType.STR, DdbTableParameter.PK.getName()),
    PREV(DdbType.STR, "prev"),
    NEXT(DdbType.STR, "next"),
    MEMBERS(DdbType.STR_SET, "members");

    @NotNull private final DdbType ddbType;
    @NotNull
    private final String jsonFieldName;

    SyncTableParameter (final @NotNull DdbType ddbType, final @NotNull String jsonFieldName) {
        this.ddbType = ddbType;
        this.jsonFieldName = jsonFieldName;
    }

    @Override
    public @NotNull
    DdbType ddbType () {
        return this.ddbType;
    }

    @Override
    public @NotNull
    String jsonFieldName () {
        return this.jsonFieldName;
    }

    @Override
    public @Nullable
    GlobalSecondaryIndexProps gsiProps () {
        return null;
    }
}
