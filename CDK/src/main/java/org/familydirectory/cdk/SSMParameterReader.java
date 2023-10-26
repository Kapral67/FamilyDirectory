package org.familydirectory.cdk;

import java.time.Instant;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.customresources.AwsCustomResource;
import software.amazon.awscdk.customresources.AwsCustomResourcePolicy;
import software.amazon.awscdk.customresources.AwsCustomResourceProps;
import software.amazon.awscdk.customresources.AwsSdkCall;
import software.amazon.awscdk.customresources.PhysicalResourceId;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awssdk.regions.Region;
import software.constructs.Construct;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

public
class SSMParameterReader extends AwsCustomResource {

    public static final String SSM_PARAMETER_READER_RESOURCE_ID = "SSMParameterReader";

    public
    SSMParameterReader (final @NotNull Construct scope, final @NotNull String id, final @NotNull String parameterName, final @NotNull Region region)
    {
        super(scope, id, AwsCustomResourceProps.builder()
                                               .onCreate(getAwsSdkCall(parameterName, region))
                                               .onUpdate(getAwsSdkCall(parameterName, region))
                                               .policy(AwsCustomResourcePolicy.fromStatements(singletonList(PolicyStatement.Builder.create()
                                                                                                                                   .actions(singletonList("ssm:GetParameter"))
                                                                                                                                   .effect(Effect.ALLOW)
                                                                                                                                   .resources(singletonList("*"))
                                                                                                                                   .build())))
                                               .build());
    }

    @Contract("_, _ -> new")
    @NotNull
    private static
    AwsSdkCall getAwsSdkCall (final @NotNull String parameterName, final @NotNull Region region) {
        return AwsSdkCall.builder()
                         .service("SSM")
                         .action("getParameter")
                         .parameters(singletonMap("Name", parameterName))
                         .physicalResourceId(PhysicalResourceId.of(String.valueOf(Instant.now()
                                                                                         .getEpochSecond())))
                         .region(region.toString())
                         .build();
    }

    @Override
    @NotNull
    public final
    String toString () {
        return this.getResponseField("Parameter.Value");
    }
}
