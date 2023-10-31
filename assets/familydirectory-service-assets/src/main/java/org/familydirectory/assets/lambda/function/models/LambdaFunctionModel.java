package org.familydirectory.assets.lambda.function.models;

import java.util.List;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.Duration;
import static software.amazon.awscdk.Duration.seconds;

public
interface LambdaFunctionModel {
    Duration DEFAULT_TIMEOUT = seconds(60);
    Number DEFAULT_MEMORY_SIZE = 1024;

    @NotNull
    String handler ();

    @Nullable
    Map<DdbTable, List<String>> ddbActions ();

    @Nullable
    List<String> cognitoActions ();

    @Nullable
    List<String> sesActions ();

    @Nullable
    List<String> sssActions ();

    default @NotNull
    String arnExportName () {
        return "%sArn".formatted(this.functionName());
    }

    @NotNull
    String functionName ();

    default @NotNull
    String roleArnExportName () {
        return "%sRoleArn".formatted(this.functionName());
    }

    @NotNull
    Duration timeout ();

    @NotNull
    Number memorySize ();
}
