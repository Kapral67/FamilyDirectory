package org.familydirectory.assets.lambda.function.toolkitcleaner;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.familydirectory.assets.lambda.function.toolkitcleaner.helper.ToolkitCleanerHelper;
import org.familydirectory.assets.lambda.function.toolkitcleaner.records.ToolkitCleanerResponse;
import org.jetbrains.annotations.NotNull;

public
class ToolkitCleanerLambda implements RequestHandler<Void, ToolkitCleanerResponse> {
    @Override
    public
    ToolkitCleanerResponse handleRequest (Void unused, @NotNull Context context) {
        try (final ToolkitCleanerHelper toolkitCleanerHelper = new ToolkitCleanerHelper(context.getLogger())) {
            return toolkitCleanerHelper.cleanObjects(toolkitCleanerHelper.extractTemplateHashes(toolkitCleanerHelper.getStackNames()));
        }
    }
}
