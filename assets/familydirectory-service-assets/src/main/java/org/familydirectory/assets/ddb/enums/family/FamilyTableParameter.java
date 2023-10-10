package org.familydirectory.assets.ddb.enums.family;

import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.DdbType;

public
enum FamilyTableParameter {
    ID(DdbType.STR, DdbTable.PK.getName()),
    ANCESTOR(DdbType.STR, "ancestor"),
    SPOUSE(DdbType.STR, "spouse"),
    DESCENDANTS(DdbType.STR_SET, "descendants");

    private final DdbType ddbType;
    private final String jsonFieldName;

    FamilyTableParameter (final DdbType ddbType, final String jsonFieldName) {
        this.ddbType = ddbType;
        this.jsonFieldName = jsonFieldName;
    }

    public final
    DdbType ddbType () {
        return this.ddbType;
    }

    public final
    String jsonFieldName () {
        return this.jsonFieldName;
    }
}
