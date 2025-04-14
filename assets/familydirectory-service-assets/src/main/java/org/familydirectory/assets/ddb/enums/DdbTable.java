package org.familydirectory.assets.ddb.enums;

import java.util.List;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.enums.sync.SyncTableParameter;
import org.familydirectory.assets.ddb.models.DdbTableParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public
enum DdbTable {
    COGNITO("CognitoTable", List.of(CognitoTableParameter.values()), false, null),
    FAMILY("FamilyTable", List.of(FamilyTableParameter.values()), false, null),
    MEMBER("MemberTable", List.of(MemberTableParameter.values()), true, null),
    SYNC("SyncTable", List.of(SyncTableParameter.values()), false, SyncTableParameter.TTL.jsonFieldName());

    @NotNull
    private final String arnExportName;
    @NotNull
    private final List<? extends DdbTableParameter> parameters;
    private final boolean hasStream;
    @Nullable
    private final String ttlAttributeName;

    DdbTable (final @NotNull String arnExportName, final @NotNull List<? extends DdbTableParameter> parameters, final boolean hasStream, final @Nullable String ttlAttributeName) {
        this.arnExportName = arnExportName;
        this.parameters = parameters;
        this.hasStream = hasStream;
        this.ttlAttributeName = ttlAttributeName;
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
    String ttlAttributeName () {
        return this.ttlAttributeName;
    }

    @Nullable
    public final
    String streamArnExportName () {
        return (this.hasStream)
                ? "%sStreamArn".formatted(this.arnExportName)
                : null;
    }
}
