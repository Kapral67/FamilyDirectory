package org.familydirectory.assets.ddb.enums.cognito;

import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.DdbType;

public
enum CognitoTableParameter {
    ID(DdbType.STR, DdbTable.PK.getName()),
    MEMBER(DdbType.STR, "member");
    private final DdbType ddbType;
    private final String jsonFieldName;

    CognitoTableParameter (final DdbType ddbType, final String jsonFieldName) {
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
