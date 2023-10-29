package org.familydirectory.assets.ddb.enums;

import java.util.List;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.models.DdbTableParameter;
import org.jetbrains.annotations.NotNull;
import static java.util.Arrays.asList;

public
enum DdbTable {
    COGNITO("CognitoTableArn", asList(CognitoTableParameter.values())),
    FAMILY("FamilyTableArn", asList(FamilyTableParameter.values())),
    MEMBER("MemberTableArn", asList(MemberTableParameter.values()));

    @NotNull
    private final String arnExportName;

    @NotNull
    private final List<? extends DdbTableParameter> parameters;

    DdbTable (final @NotNull String arnExportName, final @NotNull List<? extends DdbTableParameter> parameters) {
        this.arnExportName = arnExportName;
        this.parameters = parameters;
    }

    @NotNull
    public final
    String arnExportName () {
        return this.arnExportName;
    }

    @NotNull
    public final
    List<? extends DdbTableParameter> parameters () {
        return this.parameters;
    }
}
