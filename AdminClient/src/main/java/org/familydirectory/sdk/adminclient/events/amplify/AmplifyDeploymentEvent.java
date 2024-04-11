package org.familydirectory.sdk.adminclient.events.amplify;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.familydirectory.assets.amplify.utility.AmplifyUtils;
import org.familydirectory.sdk.adminclient.AdminClientTui;
import org.familydirectory.sdk.adminclient.enums.Commands;
import org.familydirectory.sdk.adminclient.events.model.EventHelper;
import org.familydirectory.sdk.adminclient.utility.SdkClientProvider;
import org.familydirectory.sdk.adminclient.utility.lanterna.WaitingDialog;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.amplify.AmplifyClient;
import static java.util.Objects.requireNonNull;

public final
class AmplifyDeploymentEvent implements EventHelper {
    private final @NotNull WindowBasedTextGUI gui;

    public
    AmplifyDeploymentEvent (final @NotNull WindowBasedTextGUI gui) {
        super();
        this.gui = requireNonNull(gui);
    }

    @Override
    public
    void run () {
        final MessageDialog msgDialog = new MessageDialogBuilder().setTitle(Commands.AMPLIFY_DEPLOYMENT.name())
                                                                  .setText("Rebuild Frontend?")
                                                                  .addButton(MessageDialogButton.Yes)
                                                                  .addButton(MessageDialogButton.No)
                                                                  .build();
        if (msgDialog.showDialog(this.gui)
                     .equals(MessageDialogButton.Yes))
        {
            final WaitingDialog waitDialog = WaitingDialog.createDialog(Commands.AMPLIFY_DEPLOYMENT.name(), "Please Wait...");
            waitDialog.setHints(AdminClientTui.EXTRA_WINDOW_HINTS);
            waitDialog.showDialog(requireNonNull(this.gui), false);
            final CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
                try {
                    AmplifyUtils.appDeployment(SdkClientProvider.getSdkClientProvider()
                                                                .getSdkClient(AmplifyClient.class), "AdminClient Requested", null, null);
                } finally {
                    waitDialog.close();
                }
            });
            waitDialog.waitUntilClosed();
            try {
                future.get();
            } catch (final ExecutionException e) {
                throw new RuntimeException(e);
            } catch (final InterruptedException e) {
                Thread.currentThread()
                      .interrupt();
                throw new RuntimeException(e);
            }

            new MessageDialogBuilder().setTitle(Commands.AMPLIFY_DEPLOYMENT.name())
                                      .setText("Frontend is building and will deploy when finished.")
                                      .addButton(MessageDialogButton.OK)
                                      .build()
                                      .showDialog(this.gui);
        }
    }
}
