package org.familydirectory.sdk.adminclient.events.stream;

import io.leego.banana.Ansi;
import org.familydirectory.assets.lambda.function.stream.enums.StreamFunction;
import org.familydirectory.sdk.adminclient.events.model.Executable;
import org.familydirectory.sdk.adminclient.utility.Logger;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.EventSourceMappingConfiguration;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.ListEventSourceMappingsRequest;
import software.amazon.awssdk.services.lambda.model.UpdateEventSourceMappingRequest;

public final
class TogglePdfGeneratorEvent implements Executable {
    private final @NotNull LambdaClient lambdaClient = LambdaClient.create();

    public
    TogglePdfGeneratorEvent () {
        super();
    }

    @Override
    public
    void execute () {
        final String functionName = this.lambdaClient.listFunctions()
                                                     .functions()
                                                     .stream()
                                                     .filter(fc -> fc.handler()
                                                                     .equals(StreamFunction.PDF_GENERATOR.handler()))
                                                     .map(FunctionConfiguration::functionName)
                                                     .findFirst()
                                                     .orElseThrow();

        final EventSourceMappingConfiguration eventSourceMapping = this.lambdaClient.listEventSourceMappings(ListEventSourceMappingsRequest.builder()
                                                                                                                                           .functionName(functionName)
                                                                                                                                           .build())
                                                                                    .eventSourceMappings()
                                                                                    .stream()
                                                                                    .findFirst()
                                                                                    .orElseThrow();

        final boolean isPdfGeneratorEnabled = eventSourceMapping.state()
                                                                .equalsIgnoreCase("Enabled");

        if (!(isPdfGeneratorEnabled || eventSourceMapping.state()
                                                         .equalsIgnoreCase("Disabled")))
        {
            throw new IllegalStateException("Toggle Pdf Generator State Needs Time to Transition, Please Try Again Later");
        }

        this.lambdaClient.updateEventSourceMapping(UpdateEventSourceMappingRequest.builder()
                                                                                  .uuid(eventSourceMapping.uuid())
                                                                                  .enabled(!isPdfGeneratorEnabled)
                                                                                  .build());

        if (isPdfGeneratorEnabled) {
            Logger.custom("SWITCHED OFF", Ansi.BOLD, Ansi.RED);
        } else {
            Logger.custom("SWITCHED ON", Ansi.BOLD, Ansi.GREEN);
        }
        System.out.println();
    }

    @Override
    public
    void close () {
        this.lambdaClient.close();
    }
}
