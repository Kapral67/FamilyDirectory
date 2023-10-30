package org.familydirectory.cdk.sss;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

public
class FamilyDirectorySssStack extends Stack {
    public
    FamilyDirectorySssStack (final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);
    }
}
