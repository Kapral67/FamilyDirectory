package org.familydirectory.assets.lambda.function.models;

import java.util.List;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public
interface LambdaFunctionModel {
    Number DEFAULT_TIMEOUT_SECONDS = 30;
    Number NEW_ACCOUNT_MAX_MEMORY_SIZE = 3008;
    Number SINGLE_vCPU_MEMORY_SIZE = 1769;
    Number DEFAULT_MEMORY_SIZE = SINGLE_vCPU_MEMORY_SIZE;

    @NotNull
    String handler ();

    default @Nullable
    Map<DdbTable, List<String>> ddbActions () {
        return null;
    }

    default @Nullable
    List<String> cognitoActions () {
        return null;
    }

    default @Nullable
    List<String> sesActions () {
        return null;
    }

    default @Nullable
    List<String> sssActions () {
        return null;
    }

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

    default @NotNull
    Number timeoutSeconds () {
        return DEFAULT_TIMEOUT_SECONDS;
    }

    default @NotNull
    Number memorySize () {
        return DEFAULT_MEMORY_SIZE;
    }
}
