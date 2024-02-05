package org.familydirectory.sdk.adminclient;

import io.leego.banana.Ansi;
import io.leego.banana.BananaUtils;
import io.leego.banana.Font;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;
import org.familydirectory.sdk.adminclient.enums.Commands;
import org.familydirectory.sdk.adminclient.enums.cognito.CognitoManagementOptions;
import org.familydirectory.sdk.adminclient.enums.create.CreateOptions;
import org.familydirectory.sdk.adminclient.events.cognito.CognitoManagementEvent;
import org.familydirectory.sdk.adminclient.events.create.CreateEvent;
import org.familydirectory.sdk.adminclient.events.delete.DeleteEvent;
import org.familydirectory.sdk.adminclient.events.model.Executable;
import org.familydirectory.sdk.adminclient.events.stream.TogglePdfGeneratorEvent;
import org.familydirectory.sdk.adminclient.events.toolkitcleaner.ToolkitCleanerEvent;
import org.familydirectory.sdk.adminclient.events.update.UpdateEvent;
import org.familydirectory.sdk.adminclient.utility.Logger;
import static java.util.Objects.isNull;

public final
class AdminClient {
    private static final String BANNER_HEADER = "FamilyDirectory%nAdmin Client".formatted();

    private
    AdminClient () {
        super();
    }

    public static
    void main (final String[] args) {
        try (final Scanner scanner = new Scanner(System.in)) {
            System.out.println(BananaUtils.bananansi(BANNER_HEADER, Font.ANSI_SHADOW, Ansi.PURPLE));
            Logger.info("When Prompted With Numbered Lists, Please Press a Number and then Press Enter");
            System.out.println();
            while (true) {
                int ordinal = -1;
                final Commands command;
                while (ordinal < 0 || ordinal >= Commands.values().length) {
                    Logger.customLine("Please Choose A Command:", Ansi.BOLD, Ansi.BLUE);
                    for (final Commands cmd : Commands.values()) {
                        Logger.customLine("%d) %s".formatted(cmd.ordinal(), cmd.name()));
                    }
                    final String token = scanner.nextLine()
                                                .trim();
                    try {
                        ordinal = Integer.parseInt(token);
                    } catch (final NumberFormatException ignored) {
                        ordinal = -1;
                    }
                    if (ordinal < 0 || ordinal >= Commands.values().length) {
                        Logger.error("Invalid Command");
                    }
                    System.out.println();
                }

                command = Commands.values()[ordinal];

                if (!command.options()
                            .isEmpty())
                {
                    ordinal = -1;
                    while (ordinal < 0 || ordinal >= command.options()
                                                            .size()) {
                        Logger.customLine("Please Choose An Option:", Ansi.BOLD, Ansi.BLUE);
                        command.options()
                               .forEach(e -> Logger.customLine("%d) %s".formatted(e.ordinal(), e.name())));
                        final String token = scanner.nextLine()
                                                    .trim();
                        try {
                            ordinal = Integer.parseInt(token);
                        } catch (final NumberFormatException ignored) {
                            ordinal = -1;
                        }
                        if (ordinal < 0 || ordinal >= command.options()
                                                             .size())
                        {
                            Logger.error("Invalid Option");
                        }
                        System.out.println();
                    }
                }

                try (final Executable exec = switch (command) {
                    case CREATE -> new CreateEvent(scanner, CreateOptions.values()[ordinal]);
                    case UPDATE -> new UpdateEvent(scanner);
                    case DELETE -> new DeleteEvent(scanner);
                    case TOGGLE_PDF_GENERATOR -> new TogglePdfGeneratorEvent(scanner);
                    case COGNITO_MANAGEMENT -> new CognitoManagementEvent(scanner, CognitoManagementOptions.values()[ordinal]);
                    case TOOLKIT_CLEANER -> new ToolkitCleanerEvent(scanner);
                    case EXIT -> null;
                })
                {
                    if (isNull(exec)) {
                        return;
                    }
                    exec.run();
                }
            }
        } catch (final Throwable e) {
            Logger.error(e.getMessage());
            final StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            Logger.trace(stringWriter.toString());
        }
    }
}
