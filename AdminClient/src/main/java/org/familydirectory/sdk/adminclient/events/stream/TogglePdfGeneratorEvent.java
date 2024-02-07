package org.familydirectory.sdk.adminclient.events.stream;

import io.leego.banana.Ansi;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.assets.lambda.function.stream.enums.StreamFunction;
import org.familydirectory.sdk.adminclient.utility.Logger;
import software.amazon.awssdk.services.lambda.model.EventSourceMappingConfiguration;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.ListEventSourceMappingsRequest;
import software.amazon.awssdk.services.lambda.model.UpdateEventSourceMappingRequest;

public final
class TogglePdfGeneratorEvent implements Runnable {

    public
    TogglePdfGeneratorEvent () {
        super();
    }

    @Override
    public
    void run () {
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

        displayCurrentState(isPdfGeneratorEnabled);
        System.out.println();

        Logger.customLine("SWITCH %s? (y/N)".formatted(isPdfGeneratorEnabled
                                                               ? "OFF"
                                                               : "ON"), Ansi.BOLD, Ansi.BLUE);

        final String input = this.scanner.nextLine()
                                         .trim();
        if (input.equalsIgnoreCase("y")) {
            System.out.println();

            int sleepSec = DdbUtils.DDB_STREAM_MAX_RECORD_AGE_SECONDS.intValue();
            final int maxSleepSecLength = digitCount(sleepSec);
            int maxCounterLength = 0;
            // Only Wait When Switching PdfGenerator From OFF -> ON
            while (!isPdfGeneratorEnabled && sleepSec > 0) {
                final String counter = "Please Wait For %d Seconds.%s".formatted(sleepSec, " ".repeat(maxSleepSecLength - digitCount(sleepSec)));
                if (maxCounterLength == 0 && digitCount(sleepSec) == maxSleepSecLength) {
                    maxCounterLength = counter.length();
                }
                Logger.custom("\r", counter, Ansi.BOLD, Ansi.CYAN);
                try {
                    sleep(MILLIS_IN_SEC);
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (--sleepSec <= 0L) {
                    System.out.printf("\r%s\r", " ".repeat(maxCounterLength));
                    this.lambdaClient.invoke(InvokeRequest.builder()
                                                          .functionName(functionName)
                                                          .invocationType(InvocationType.EVENT)
                                                          .build());
                }
            }

            this.lambdaClient.updateEventSourceMapping(UpdateEventSourceMappingRequest.builder()
                                                                                      .uuid(eventSourceMapping.uuid())
                                                                                      .enabled(!isPdfGeneratorEnabled)
                                                                                      .build());

            displayCurrentState(!isPdfGeneratorEnabled);
        }
        if (!input.isBlank()) {
            System.out.println();
        }
    }

    private static
    int digitCount (final int n) {
        return (int) (Math.log10(n) + 1.0);
    }

    private static
    void displayCurrentState (final boolean isOn) {
        if (isOn) {
            Logger.customLine("SWITCHED ON", Ansi.BOLD, Ansi.GREEN);
        } else {
            Logger.customLine("SWITCHED OFF", Ansi.BOLD, Ansi.RED);
        }
    }
}
