package org.familydirectory.sdk.adminclient;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
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
import org.familydirectory.assets.Constants;
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
import org.familydirectory.sdk.adminclient.utility.CanceledException;
import org.familydirectory.sdk.adminclient.utility.Logger;
import org.familydirectory.sdk.adminclient.utility.SdkClientProvider;
import org.familydirectory.sdk.adminclient.utility.lanterna.FamilyDirectoryBackground;
import org.familydirectory.sdk.adminclient.utility.pickers.CognitoUserPicker;
import org.familydirectory.sdk.adminclient.utility.pickers.MemberPicker;
import org.familydirectory.sdk.adminclient.utility.pickers.SpousePicker;
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
    public static final String README = "https://github.com/Kapral67/FamilyDirectory/blob/main/README.md";
    @NotNull
    public static final String AWS_REGION = requireNonNull(getenv("AWS_REGION"), README);
    @NotNull
    public static final String AWS_ACCOUNT_ID = requireNonNull(getenv("AWS_ACCOUNT_ID"), README);
    @NotNull
    private static final String HELP_MSG = """
                                           Family Directory AdminClient v%s
                                             -b | --background  COLOR  background color
                                             -d | --debug              show debug messages
                                             -h | --help               show this message
                                             -t | --text        COLOR  background text color
                                             -v | --version            show this message
                                             -w | --window             prefer windowed mode
                                                                                      
                                           COLOR:
                                             [ BLACK, BLUE, CYAN, GREEN, MAGENTA, RED, WHITE, YELLOW ]""".formatted(Constants.VERSION_STR);
    private static boolean DEBUG = false;

    private
    AdminClientTui () {
        super();
    }

    public static
    void main (final String[] args) {
        final List<String> arguments = Arrays.asList(args);
        DEBUG = arguments.contains("-d") || arguments.contains("--debug");
        if (arguments.contains("-h") || arguments.contains("--help") || arguments.contains("-v") || arguments.contains("--version")) {
            System.out.println(HELP_MSG);
            return;
        }
        setLanternaSttyPropertyKey();
        try (final SdkClientProvider ignored = SdkClientProvider.getSdkClientProvider()) {
            try (final MemberPicker memberPicker = new MemberPicker(); final CognitoUserPicker cognitoPicker = new CognitoUserPicker(); final SpousePicker spousePicker = new SpousePicker()) {
                memberPicker.start();
                cognitoPicker.start();
                spousePicker.start();

                final DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory().setPreferTerminalEmulator(arguments.contains("-w") || arguments.contains("--window"));
                try (final Screen screen = terminalFactory.createScreen()) {
                    screen.startScreen();
                    final WindowBasedTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new FamilyDirectoryBackground(getColorFromCmdLineArgs(arguments, List.of("-t", "--text")),
                                                                                                                                            getColorFromCmdLineArgs(arguments,
                                                                                                                                                                    List.of("-b", "--background"))));
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
                                                                                                                      .setCanCancel(true)
                                                                                                                      .setExtraWindowHints(EXTRA_WINDOW_HINTS)
                                                                                                                      .addListItems(options)
                                                                                                                      .build();
                            option = optionsListDialog.showDialog(gui);
                            if (isNull(option)) {
                                continue;
                            }
                        }

                        try (final EventHelper runner = switch (cmd) {
                            case CREATE -> new CreateEvent(gui, (CreateOptions) requireNonNull(option), memberPicker, spousePicker);
                            case UPDATE -> new UpdateEvent(gui, memberPicker, cognitoPicker, spousePicker);
                            case DELETE -> new DeleteEvent(gui, memberPicker, cognitoPicker, spousePicker);
                            case TOGGLE_PDF_GENERATOR -> new TogglePdfGeneratorEvent(gui);
                            case COGNITO_MANAGEMENT -> new CognitoManagementEvent(gui, (CognitoManagementOptions) requireNonNull(option), cognitoPicker);
                            case TOOLKIT_CLEANER -> new ToolkitCleanerEvent(gui);
                            case EXIT -> null;
                        })
                        {
                            if (isNull(runner)) {
                                return;
                            }
                            try {
                                runner.run();
                            } catch (final CanceledException ignored1) {
                            }
                        }
                    }
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
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
            Logger.trace(stringWriter.toString());
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
        if (nonNull(PATH)) {
            try {
                for (final String dir : PATH.split(File.pathSeparator)) {
                    final Path path = Path.of(dir, "stty");
                    if (Files.exists(path)) {
                        return path.toString();
                    }
                }
            } catch (final Exception e) {
                if (DEBUG) {
                    Logger.debug(e.getMessage());
                }
            }
        }
        return null;
    }

    @Nullable
    private static
    TextColor.ANSI getColorFromCmdLineArgs (final @NotNull List<String> arguments, final @NotNull List<String> argPrefixes) {
        TextColor.ANSI color = null;
        for (int i = 0; i < arguments.size() - 1; ++i) {
            if (argPrefixes.contains(arguments.get(i))) {
                try {
                    color = TextColor.ANSI.valueOf(arguments.get(i + 1)
                                                            .trim()
                                                            .toUpperCase());
                    break;
                } catch (final IllegalArgumentException ignored) {
                }
            }
        }
        return color;
    }

    public static
    boolean isDEBUG () {
        return DEBUG;
    }
}
