package org.familydirectory.assets.ddb.enums.sync;

import org.familydirectory.assets.ddb.enums.DdbType;
import org.familydirectory.assets.ddb.models.DdbTableParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;

public
enum SyncTableParameter implements DdbTableParameter {
    ID(DdbType.STR, DdbTableParameter.PK.getName()),
    NEXT(DdbType.STR, "next"),
    MEMBERS(DdbType.STR_SET, "members"),
    TTL(DdbType.NUM, "ttl");

    @NotNull private final DdbType ddbType;
    @NotNull
    private final String jsonFieldName;

    SyncTableParameter (final @NotNull DdbType ddbType, final @NotNull String jsonFieldName) {
        this.ddbType = ddbType;
        this.jsonFieldName = jsonFieldName;
    }

    @Override
    public final @NotNull
    DdbType ddbType () {
        return this.ddbType;
    }

    @Override
    public final @NotNull
    String jsonFieldName () {
        return this.jsonFieldName;
    }

    @Override
    public final @Nullable
    GlobalSecondaryIndexProps gsiProps () {
        return null;
    }
}
