package org.familydirectory.sdk.adminclient;

import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.ListSelectDialog;
import com.googlecode.lanterna.gui2.dialogs.ListSelectDialogBuilder;
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
import org.familydirectory.sdk.adminclient.enums.cognito.CognitoManagementOptions;
import org.familydirectory.sdk.adminclient.enums.create.CreateOptions;
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
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

public final
class AdminClientTui {
    @NotNull
    public static final Set<Window.Hint> EXTRA_WINDOW_HINTS = Set.of(Window.Hint.CENTERED, Window.Hint.MODAL);
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
                final WindowBasedTextGUI gui = new MultiWindowTextGUI(screen);

                while (true) {
                    final ListSelectDialog<Commands> cmdListDialog = new ListSelectDialogBuilder<Commands>().setTitle("AdminClient")
                                                                                                            .setDescription("Choose a Command")
                                                                                                            .setCanCancel(false)
                                                                                                            .setExtraWindowHints(EXTRA_WINDOW_HINTS)
                                                                                                            .addListItems(Commands.values())
                                                                                                            .build();
                    final Commands cmd = cmdListDialog.showDialog(gui);
                    final Enum<?>[] options = cmd.options();
                    Enum<?> option = null;
                    if (nonNull(options)) {
                        final ListSelectDialog<Enum<?>> optionsListDialog = new ListSelectDialogBuilder<Enum<?>>().setTitle(cmd.name())
                                                                                                                  .setDescription("Choose an Option")
                                                                                                                  .setCanCancel(false)
                                                                                                                  .setExtraWindowHints(EXTRA_WINDOW_HINTS)
                                                                                                                  .addListItems(options)
                                                                                                                  .build();
                        option = optionsListDialog.showDialog(gui);
                    }

                    try (final Runnable runner = switch (cmd) {
                        case CREATE -> {
                            if (isNull(option)) {
                                throw new IllegalStateException(); // control should not reach this
                            }
                            memberThreadPicker.thread()
                                              .join();
                            yield new CreateEvent(gui, (CreateOptions) option, memberThreadPicker.picker());
                        }
                        case UPDATE -> new UpdateEvent(scanner);
                        case DELETE -> new DeleteEvent(scanner);
                        case TOGGLE_PDF_GENERATOR -> new TogglePdfGeneratorEvent(scanner);
                        case COGNITO_MANAGEMENT -> new CognitoManagementEvent(scanner, CognitoManagementOptions.values()[ordinal]);
                        case TOOLKIT_CLEANER -> new ToolkitCleanerEvent(scanner);
                        case EXIT -> null;
                    })
                    {
                        if (isNull(runner)) {
                            return;
                        }
                        runner.run();
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
    record ThreadPicker<P extends PickerModel>(@NotNull Thread thread, @NotNull P picker) {
    }
}
