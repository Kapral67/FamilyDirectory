package org.familydirectory.assets.ddb;

import org.familydirectory.assets.ddb.models.members.MembersModel;
import software.amazon.awscdk.services.dynamodb.Attribute;

import static software.amazon.awscdk.services.dynamodb.Attribute.builder;
import static software.amazon.awscdk.services.dynamodb.AttributeType.STRING;

public enum DdbTable {
    MEMBERS("MembersTableArn"), FAMILIES("FamiliesTableArn");

    /**
     * For {@link DdbTable#MEMBERS}: This is the sha256Hex hash of a Members fullName & birthdayString, concatenated.
     * See {@link MembersModel} for how fullName and birthdayString are computed
     * <br><br>
     * For {@link DdbTable#FAMILIES}: This is the familial side, head member's `PK` from the MEMBERS table
     */
    public static final Attribute PK = builder().name("id").type(STRING).build();
    private final String arnExportName;

    DdbTable(final String arnExportName) {
        this.arnExportName = arnExportName;
    }

    public final String arnExportName() {
        return this.arnExportName;
    }
}
