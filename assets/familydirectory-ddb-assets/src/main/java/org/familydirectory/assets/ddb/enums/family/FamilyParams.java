package org.familydirectory.assets.ddb.enums.family;

import org.familydirectory.assets.ddb.enums.DdbType;
import static org.familydirectory.assets.ddb.enums.DdbType.STR;
import static org.familydirectory.assets.ddb.enums.DdbType.STR_SET;

public
enum FamilyParams {
    ANCESTOR(STR, "ancestor"),
    SPOUSE(STR, "spouse"),
    DESCENDANTS(STR_SET, "descendants");

    private final DdbType ddbType;
    private final String jsonFieldName;

    FamilyParams (final DdbType ddbType, final String jsonFieldName) {
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
