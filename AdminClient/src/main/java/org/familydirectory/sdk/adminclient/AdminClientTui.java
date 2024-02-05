package org.familydirectory.sdk.adminclient;

import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.ActionListDialogBuilder;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.familydirectory.sdk.adminclient.enums.Commands;
import org.familydirectory.sdk.adminclient.events.cognito.CognitoManagementEvent;
import org.familydirectory.sdk.adminclient.events.create.CreateEvent;
import org.familydirectory.sdk.adminclient.events.delete.DeleteEvent;
import org.familydirectory.sdk.adminclient.events.stream.TogglePdfGeneratorEvent;
import org.familydirectory.sdk.adminclient.events.toolkitcleaner.ToolkitCleanerEvent;
import org.familydirectory.sdk.adminclient.events.update.UpdateEvent;
import org.familydirectory.sdk.adminclient.utility.Logger;
import org.familydirectory.sdk.adminclient.utility.SdkClientProvider;
import org.familydirectory.sdk.adminclient.utility.pickers.CognitoUserPicker;
import org.familydirectory.sdk.adminclient.utility.pickers.MemberPicker;
import org.familydirectory.sdk.adminclient.utility.pickers.model.PickerModel;
import org.jetbrains.annotations.NotNull;
import static java.lang.System.getenv;
import static java.util.Objects.requireNonNull;

public final
class AdminClientTui {
    @NotNull
    private static final String AWS_REGION = requireNonNull(getenv("AWS_REGION"));
    @NotNull
    private static final String AWS_ACCOUNT_ID = requireNonNull(getenv("AWS_ACCOUNT_ID"));

    private
    AdminClientTui () {
        super();
    }

    public static
    void main (final String[] args) {
        final List<String> arguments = Arrays.asList(args);
        try (final SdkClientProvider sdkClientProvider = SdkClientProvider.getSdkClientProvider()) {
            final ThreadPicker<MemberPicker> memberThreadPicker = initializePicker(MemberPicker.class);
            final ThreadPicker<CognitoUserPicker> cognitoThreadPicker = initializePicker(CognitoUserPicker.class);

            final DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
            try (final Screen screen = terminalFactory.createScreen()) {
                screen.startScreen();
                // TODO: Investigate WindowManager & Component (background) for alternate MultiWindowTextGui ctor
                final WindowBasedTextGUI windowBasedTextGUI = new MultiWindowTextGUI(screen);

                while (true) {
                    final ActionListDialogBuilder commandListDialogBuilder = new ActionListDialogBuilder().setTitle("AdminClient")
                                                                                                          .setDescription("Choose a Command")
                                                                                                          .setCanCancel(false)
                                                                                                          .setExtraWindowHints(Set.of(Window.Hint.CENTERED, Window.Hint.MODAL));
                    for (final Commands cmd : Commands.values()) {
                        switch (cmd) {
                            case CREATE -> commandListDialogBuilder.addAction(cmd.name(), new CreateEvent());
                            case UPDATE -> commandListDialogBuilder.addAction(cmd.name(), new UpdateEvent());
                            case DELETE -> commandListDialogBuilder.addAction(cmd.name(), new DeleteEvent());
                            case TOGGLE_PDF_GENERATOR -> commandListDialogBuilder.addAction(cmd.name(), new TogglePdfGeneratorEvent());
                            case COGNITO_MANAGEMENT -> commandListDialogBuilder.addAction(cmd.name(), new CognitoManagementEvent());
                            case TOOLKIT_CLEANER -> commandListDialogBuilder.addAction(cmd.name(), new ToolkitCleanerEvent());
                            case EXIT -> commandListDialogBuilder.addAction(cmd.name(), () -> { /* EXIT */ });
                        }
                    }
                }

            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

        } catch (final Throwable e) {
            Logger.error(e.getMessage());
            if (arguments.contains("-d") || arguments.contains("--debug")) {
                final StringWriter stringWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stringWriter));
                Logger.trace(stringWriter.toString());
            }
        }
    }

    @NotNull
    private static
    <P extends PickerModel> ThreadPicker<P> initializePicker (final @NotNull Class<P> pickerClass)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        final P picker = requireNonNull(pickerClass).getDeclaredConstructor()
                                                    .newInstance();
        final Thread pickerThread = new Thread(picker);
        pickerThread.start();
        return new ThreadPicker<>(pickerThread, picker);
    }

    private
    record ThreadPicker<P extends PickerModel>(@NotNull Thread pickerThread, @NotNull P picker) {
    }
}
