package org.familydirectory.assets.lambda.function.toolkitcleaner.enums;

import java.util.List;
import org.familydirectory.assets.lambda.function.models.LambdaFunctionModel;
import org.jetbrains.annotations.NotNull;
import static java.util.Objects.requireNonNull;

public
enum ToolkitCleanerFunction implements LambdaFunctionModel {
    TOOLKIT_CLEANER("ToolkitCleaner",
                    List.of("s3:ListAllMyBuckets", "s3:ListBucket", "s3:ListBucketVersions", "s3:GetObject", "s3:DeleteObject", "cloudformation:DescribeStacks", "cloudformation:ListStacks",
                            "cloudformation:GetTemplate"));

    @NotNull
    private final String functionName;
    @NotNull
    private final List<String> globalActions;

    ToolkitCleanerFunction (final @NotNull String functionName, final @NotNull List<String> globalActions) {
        this.functionName = "%sLambda".formatted(requireNonNull(functionName));
        this.globalActions = requireNonNull(globalActions);
    }

    @Override
    @NotNull
    public
    String handler () {
        return "org.familydirectory.assets.lambda.function.toolkitcleaner.%s::handleRequest".formatted(this.functionName);
    }

    @Override
    @NotNull
    public
    String functionName () {
        return this.functionName;
    }

    @NotNull
    public
    List<String> globalActions () {
        return this.globalActions;
    }
}
