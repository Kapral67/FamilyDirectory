package org.familydirectory.assets.ddb.enums;

import java.util.List;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.models.DdbTableParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Arrays.asList;

public
enum DdbTable {
    COGNITO("CognitoTable", asList(CognitoTableParameter.values()), false),
    FAMILY("FamilyTable", asList(FamilyTableParameter.values()), false),
    MEMBER("MemberTable", asList(MemberTableParameter.values()), true);

    @NotNull
    private final String arnExportName;
    @NotNull
    private final List<? extends DdbTableParameter> parameters;

    private final boolean hasStream;

    DdbTable (final @NotNull String arnExportName, final @NotNull List<? extends DdbTableParameter> parameters, final boolean hasStream) {
        this.arnExportName = arnExportName;
        this.parameters = parameters;
        this.hasStream = hasStream;
    }

    @NotNull
    public final
    String arnExportName () {
        return "%sArn".formatted(this.arnExportName);
    }

    @NotNull
    public final
    List<? extends DdbTableParameter> parameters () {
        return this.parameters;
    }

    public final
    boolean hasStream () {
        return this.hasStream;
    }

    @Nullable
    public final
    String streamArnExportName () {
        return (this.hasStream)
                ? "%sStreamArn".formatted(this.arnExportName)
                : null;
    }
}
