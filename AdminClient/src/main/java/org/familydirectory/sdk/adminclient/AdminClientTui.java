package org.familydirectory.sdk.adminclient;

import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.ListSelectDialog;
import com.googlecode.lanterna.gui2.dialogs.ListSelectDialogBuilder;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.familydirectory.sdk.adminclient.enums.Commands;
import org.familydirectory.sdk.adminclient.enums.cognito.CognitoManagementOptions;
import org.familydirectory.sdk.adminclient.enums.create.CreateOptions;
import org.familydirectory.sdk.adminclient.events.cognito.CognitoManagementEvent;
import org.familydirectory.sdk.adminclient.events.create.CreateEvent;
import org.familydirectory.sdk.adminclient.events.delete.DeleteEvent;
import org.familydirectory.sdk.adminclient.events.model.EventHelper;
import org.familydirectory.sdk.adminclient.events.stream.TogglePdfGeneratorEvent;
import org.familydirectory.sdk.adminclient.events.toolkitcleaner.ToolkitCleanerEvent;
import org.familydirectory.sdk.adminclient.events.update.UpdateEvent;
import org.familydirectory.sdk.adminclient.utility.Logger;
import org.familydirectory.sdk.adminclient.utility.SdkClientProvider;
import org.familydirectory.sdk.adminclient.utility.pickers.CognitoUserPicker;
import org.familydirectory.sdk.adminclient.utility.pickers.MemberPicker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.lang.System.getenv;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

public final
class AdminClientTui {
    @NotNull
    public static final Set<Window.Hint> EXTRA_WINDOW_HINTS = Set.of(Window.Hint.MODAL, Window.Hint.CENTERED, Window.Hint.FIT_TERMINAL_WINDOW);
    @NotNull
    public static final String LANTERNA_STTY_PROPERTY_KEY = "com.googlecode.lanterna.terminal.UnixTerminal.sttyCommand";
    @NotNull
    private static final String AWS_REGION = requireNonNull(getenv("AWS_REGION"));
    @NotNull
    private static final String AWS_ACCOUNT_ID = requireNonNull(getenv("AWS_ACCOUNT_ID"));
    private static volatile boolean DEBUG = false;

    private
    AdminClientTui () {
        super();
    }

    public static
    void main (final String[] args) {
        final List<String> arguments = Arrays.asList(args);
        DEBUG = arguments.contains("-d") || arguments.contains("--debug");
        setLanternaSttyPropertyKey();
        try (final SdkClientProvider ignored = SdkClientProvider.getSdkClientProvider(); final MemberPicker memberPicker = new MemberPicker();
             final CognitoUserPicker cognitoPicker = new CognitoUserPicker())
        {
            memberPicker.start();
            cognitoPicker.start();

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

                    try (final EventHelper runner = switch (cmd) {
                        case CREATE -> new CreateEvent(gui, (CreateOptions) requireNonNull(option), memberPicker);
                        case UPDATE -> new UpdateEvent(gui, memberPicker);
                        case DELETE -> new DeleteEvent(gui, memberPicker);
                        case TOGGLE_PDF_GENERATOR -> new TogglePdfGeneratorEvent(gui);
                        case COGNITO_MANAGEMENT -> new CognitoManagementEvent(gui, (CognitoManagementOptions) requireNonNull(option), cognitoPicker);
                        case TOOLKIT_CLEANER -> new ToolkitCleanerEvent(gui);
                        case EXIT -> null;
                    })
                    {
                        if (isNull(runner)) {
                            return;
                        }
                        runner.run();
                        if (cmd.equals(Commands.DELETE)) {
                            cognitoPicker.refresh();
                        }
                    }
                }
//      DO NOT PLACE ANY CODE HERE
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
//      DO NOT PLACE ANY CODE HERE
        } catch (final Throwable e) {
            catchAll(e);
        }
    }

    public static
    void catchAll (final @NotNull Throwable e) {
        Logger.error(e.getMessage());
        if (DEBUG) {
            final StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            Logger.debug(stringWriter.toString());
        }
    }

    private static
    void setLanternaSttyPropertyKey () {
        final String sttyPath = getSttyPath();
        if (nonNull(sttyPath)) {
            System.setProperty(LANTERNA_STTY_PROPERTY_KEY, sttyPath);
        }
    }

    @Nullable
    private static
    String getSttyPath () {
        final String PATH = System.getenv("PATH");
        if (isNull(PATH)) {
            return null;
        }
        try {
            for (final String dir : PATH.split(File.pathSeparator)) {
                final Path path = Path.of(dir + File.separatorChar + "stty");
                if (Files.exists(path)) {
                    return path.toString();
                }
            }
        } catch (final Exception e) {
            if (DEBUG) {
                Logger.debug(e.getMessage());
            }
        }
        return null;
    }
}
