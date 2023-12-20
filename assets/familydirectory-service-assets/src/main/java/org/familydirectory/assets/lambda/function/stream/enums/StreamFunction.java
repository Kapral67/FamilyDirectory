package org.familydirectory.assets.lambda.function.stream.enums;

import java.util.List;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.assets.lambda.function.models.LambdaFunctionModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public
enum StreamFunction implements LambdaFunctionModel {
    PDF_GENERATOR("PdfGenerator", singletonList(DdbTable.MEMBER), Map.of(DdbTable.MEMBER, singletonList("dynamodb:GetItem"), DdbTable.FAMILY, singletonList("dynamodb:GetItem")),
                  singletonList("s3:PutObject"));

    @NotNull
    private final String functionName;
    @NotNull
    private final List<DdbTable> streamEventSources;
    @Nullable
    private final Map<DdbTable, List<String>> ddbActions;
    @Nullable
    private final List<String> sssActions;

    StreamFunction (final @NotNull String functionName, final @NotNull List<DdbTable> streamEventSources, final @Nullable Map<DdbTable, List<String>> ddbActions,
                    final @Nullable List<String> sssActions)
    {
        this.functionName = "FamilyDirectory%sLambda".formatted(requireNonNull(functionName));
        this.streamEventSources = requireNonNull(streamEventSources);
        this.ddbActions = ddbActions;
        this.sssActions = sssActions;
    }

    @Override
    public @NotNull
    String handler () {
        return "org.familydirectory.assets.lambda.function.stream.%s::handleRequest".formatted(this.functionName);
    }

    @Override
    public @Nullable
    Map<DdbTable, List<String>> ddbActions () {
        return this.ddbActions;
    }

    @Override
    public @Nullable
    List<String> cognitoActions () {
        return null;
    }

    @Override
    public @Nullable
    List<String> sesActions () {
        return null;
    }

    @Override
    public @Nullable
    List<String> sssActions () {
        return this.sssActions;
    }

    @Override
    public @NotNull
    String functionName () {
        return this.functionName;
    }

    @Override
    public @NotNull
    Number timeoutSeconds () {
        return DdbUtils.DDB_STREAM_MAX_RECORD_AGE_SECONDS;
    }

    public @NotNull
    List<DdbTable> streamEventSources () {
        return this.streamEventSources;
    }
}
