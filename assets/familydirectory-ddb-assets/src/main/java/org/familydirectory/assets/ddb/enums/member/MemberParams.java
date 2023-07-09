package org.familydirectory.assets.ddb.enums.member;

import org.familydirectory.assets.ddb.enums.DdbType;

import static org.familydirectory.assets.ddb.enums.DdbType.BOOL;
import static org.familydirectory.assets.ddb.enums.DdbType.MAP;
import static org.familydirectory.assets.ddb.enums.DdbType.STR;
import static org.familydirectory.assets.ddb.enums.DdbType.STR_SET;

public enum MemberParams {
    FIRST_NAME(STR, "firstName"), LAST_NAME(STR, "lastName"), SUFFIX(STR, "suffix"), BIRTHDAY(STR,
            "birthday"), DEATHDAY(STR, "deathday"), EMAIL(STR, "email"), PHONES(MAP, "phones"), ADDRESS(STR_SET,
            "address"), ANCESTOR(STR, "ancestor"), IS_ANCESTOR_SPOUSE(BOOL, "isAncestorSpouse");

    private final DdbType ddbType;
    private final String jsonFieldName;

    MemberParams(final DdbType ddbType, final String jsonFieldName) {
        this.ddbType = ddbType;
        this.jsonFieldName = jsonFieldName;
    }

    public final DdbType ddbType() {
        return this.ddbType;
    }

    public final String jsonFieldName() {
        return this.jsonFieldName;
    }
}
