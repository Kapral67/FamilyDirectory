package org.familydirectory.assets.ddb.enums.cognito;

import org.familydirectory.assets.ddb.enums.DdbType;
import static org.familydirectory.assets.ddb.enums.DdbTable.PK;
import static org.familydirectory.assets.ddb.enums.DdbType.STR;

public
enum CognitoParams {
    ID(STR, PK.getName()),
    MEMBER(STR, "member");
    private final DdbType ddbType;
    private final String jsonFieldName;

    CognitoParams (final DdbType ddbType, final String jsonFieldName) {
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
