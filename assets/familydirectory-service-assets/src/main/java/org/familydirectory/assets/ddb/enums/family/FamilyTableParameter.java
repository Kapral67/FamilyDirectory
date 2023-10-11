package org.familydirectory.assets.ddb.enums.family;

import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.DdbType;
import org.jetbrains.annotations.NotNull;

public
enum FamilyTableParameter {
    ID(DdbType.STR, DdbTable.PK.getName()),
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
}
