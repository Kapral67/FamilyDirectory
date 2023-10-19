package org.familydirectory.assets.lambda.function.models;

import java.util.List;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public
interface LambdaFunctionModel {
    @NotNull
    String handler ();

    @Nullable
    Map<DdbTable, List<String>> ddbActions ();

    @Nullable
    List<String> cognitoActions ();

    @Nullable
    List<String> sesActions ();

    default @NotNull
    String arnExportName () {
        return "%sArn".formatted(this.functionName());
    }

    @NotNull
    String functionName ();
}
