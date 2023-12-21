package org.familydirectory.cdk.toolkitcleaner;

import org.familydirectory.cdk.constructs.toolkitcleaner.ToolkitCleaner;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

public
class CdkGarbageCollectionStack extends Stack {
    private static final String TOOLKIT_CLEANER_RESOURCE_ID = "ToolkitCleaner";

    public
    CdkGarbageCollectionStack (final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        new ToolkitCleaner(this, TOOLKIT_CLEANER_RESOURCE_ID);
    }
}
