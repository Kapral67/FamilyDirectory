package org.familydirectory.sdk.adminclient.events.stream;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.assets.lambda.function.stream.enums.StreamFunction;
import org.familydirectory.sdk.adminclient.enums.Commands;
import org.familydirectory.sdk.adminclient.utility.SdkClientProvider;
import org.familydirectory.sdk.adminclient.utility.dialogs.EnhancedWaitingDialog;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.EventSourceMappingConfiguration;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.ListEventSourceMappingsRequest;
import software.amazon.awssdk.services.lambda.model.UpdateEventSourceMappingRequest;
import static java.util.Objects.requireNonNull;

public final
class TogglePdfGeneratorEvent implements Runnable {
    private static final String ENABLED = "Enabled";
    private static final String DISABLED = "Disabled";
    private final @NotNull WindowBasedTextGUI gui;

    public
    TogglePdfGeneratorEvent (final @NotNull WindowBasedTextGUI gui) {
        super();
        this.gui = requireNonNull(gui);
    }

    @Override
    public
    void run () {
        final LambdaClient lambdaClient = SdkClientProvider.getSdkClientProvider()
                                                           .getSdkClient(LambdaClient.class);
        final String functionName = lambdaClient.listFunctions()
                                                .functions()
                                                .stream()
                                                .filter(fc -> fc.handler()
                                                                .equals(StreamFunction.PDF_GENERATOR.handler()))
                                                .map(FunctionConfiguration::functionName)
                                                .findFirst()
                                                .orElseThrow();
        final EventSourceMappingConfiguration eventSourceMapping = getEventSourceMappingConfig(functionName);

        final boolean isPdfGeneratorEnabled = eventSourceMapping.state()
                                                                .equalsIgnoreCase(ENABLED);

        final MessageDialog switchPromptDialog = new MessageDialogBuilder().setTitle(Commands.TOGGLE_PDF_GENERATOR.name())
                                                                           .setText("SWITCH %s?".formatted(isPdfGeneratorEnabled
                                                                                                                   ? "OFF"
                                                                                                                   : "ON"))
                                                                           .addButton(MessageDialogButton.Yes)
                                                                           .addButton(MessageDialogButton.No)
                                                                           .build();
        if (switchPromptDialog.showDialog(this.gui)
                              .equals(MessageDialogButton.Yes))
        {
            final long sleepSec = DdbUtils.DDB_STREAM_MAX_RECORD_AGE_SECONDS.longValue();

            if (!isPdfGeneratorEnabled && sleepSec > 0L) {
                new EnhancedWaitingDialog(Commands.TOGGLE_PDF_GENERATOR.name(), sleepSec).showDialog(this.gui);
                lambdaClient.invoke(InvokeRequest.builder()
                                                 .functionName(functionName)
                                                 .invocationType(InvocationType.EVENT)
                                                 .build());
            }

            lambdaClient.updateEventSourceMapping(UpdateEventSourceMappingRequest.builder()
                                                                                 .uuid(eventSourceMapping.uuid())
                                                                                 .enabled(!isPdfGeneratorEnabled)
                                                                                 .build());
        }
    }

    @NotNull
    private static
    EventSourceMappingConfiguration getEventSourceMappingConfig (final @NotNull String functionName) {
        final AtomicReference<EventSourceMappingConfiguration> eventSourceMappingRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        try (final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor()) {
            executorService.scheduleAtFixedRate(() -> {
                EventSourceMappingConfiguration eventSourceMapping = SdkClientProvider.getSdkClientProvider()
                                                                                      .getSdkClient(LambdaClient.class)
                                                                                      .listEventSourceMappings(ListEventSourceMappingsRequest.builder()
                                                                                                                                             .functionName(requireNonNull(functionName))
                                                                                                                                             .build())
                                                                                      .eventSourceMappings()
                                                                                      .stream()
                                                                                      .findFirst()
                                                                                      .orElseThrow();

                if (eventSourceMapping.state()
                                      .equalsIgnoreCase(ENABLED) || eventSourceMapping.state()
                                                                                      .equalsIgnoreCase(DISABLED))
                {
                    eventSourceMappingRef.set(eventSourceMapping);
                    latch.countDown();
                }
            }, 0L, 100L, TimeUnit.MILLISECONDS);
            latch.await();
        } catch (final InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            throw new RuntimeException(e);
        }
        return eventSourceMappingRef.get();
    }
}
