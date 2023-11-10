package org.familydirectory.sdk.adminclient;

import java.util.Scanner;
import org.familydirectory.sdk.adminclient.enums.Commands;
import org.familydirectory.sdk.adminclient.enums.create.CreateOptions;
import org.familydirectory.sdk.adminclient.events.create.CreateEvent;
import org.familydirectory.sdk.adminclient.events.delete.DeleteEvent;
import org.familydirectory.sdk.adminclient.events.update.UpdateEvent;

public final
class AdminClient {
    private static final String BANNER_HEADER = """
                                                                                                                                                                     \s
                                                ███████╗ █████╗ ███╗   ███╗██╗██╗  ██╗   ██╗    ██████╗ ██╗██████╗ ███████╗ ██████╗████████╗ ██████╗ ██████╗ ██╗   ██╗
                                                ██╔════╝██╔══██╗████╗ ████║██║██║  ╚██╗ ██╔╝    ██╔══██╗██║██╔══██╗██╔════╝██╔════╝╚══██╔══╝██╔═══██╗██╔══██╗╚██╗ ██╔╝
                                                █████╗  ███████║██╔████╔██║██║██║   ╚████╔╝     ██║  ██║██║██████╔╝█████╗  ██║        ██║   ██║   ██║██████╔╝ ╚████╔╝\s
                                                ██╔══╝  ██╔══██║██║╚██╔╝██║██║██║    ╚██╔╝      ██║  ██║██║██╔══██╗██╔══╝  ██║        ██║   ██║   ██║██╔══██╗  ╚██╔╝ \s
                                                ██║     ██║  ██║██║ ╚═╝ ██║██║███████╗██║       ██████╔╝██║██║  ██║███████╗╚██████╗   ██║   ╚██████╔╝██║  ██║   ██║  \s
                                                ╚═╝     ╚═╝  ╚═╝╚═╝     ╚═╝╚═╝╚══════╝╚═╝       ╚═════╝ ╚═╝╚═╝  ╚═╝╚══════╝ ╚═════╝   ╚═╝    ╚═════╝ ╚═╝  ╚═╝   ╚═╝  \s
                                                 █████╗ ██████╗ ███╗   ███╗██╗███╗   ██╗     ██████╗██╗     ██╗███████╗███╗   ██╗████████╗                           \s
                                                ██╔══██╗██╔══██╗████╗ ████║██║████╗  ██║    ██╔════╝██║     ██║██╔════╝████╗  ██║╚══██╔══╝                           \s
                                                ███████║██║  ██║██╔████╔██║██║██╔██╗ ██║    ██║     ██║     ██║█████╗  ██╔██╗ ██║   ██║                              \s
                                                ██╔══██║██║  ██║██║╚██╔╝██║██║██║╚██╗██║    ██║     ██║     ██║██╔══╝  ██║╚██╗██║   ██║                              \s
                                                ██║  ██║██████╔╝██║ ╚═╝ ██║██║██║ ╚████║    ╚██████╗███████╗██║███████╗██║ ╚████║   ██║                              \s
                                                ╚═╝  ╚═╝╚═════╝ ╚═╝     ╚═╝╚═╝╚═╝  ╚═══╝     ╚═════╝╚══════╝╚═╝╚══════╝╚═╝  ╚═══╝   ╚═╝                              \s
                                                                                                                                                                     \s""";

    private
    AdminClient () {
        super();
    }

    public static
    void main (final String[] args) {
        try (final Scanner scanner = new Scanner(System.in)) {
            System.out.println(BANNER_HEADER);
            while (true) {
                int ordinal = -1;
                final Commands command;
                while (ordinal < 0 || ordinal >= Commands.values().length) {
                    System.out.println("Please Choose Your Command:");
                    for (final Commands cmd : Commands.values()) {
                        System.out.printf("%d) %s%n", cmd.ordinal(), cmd.name());
                    }
                    ordinal = scanner.nextInt();
                    scanner.nextLine();
                    if (ordinal < 0 || ordinal >= Commands.values().length) {
                        System.err.println("[ERROR] Invalid Command");
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
                        System.out.println("Please Choose An Option:");
                        command.options()
                               .forEach(e -> System.out.printf("%d) %s%n", e.ordinal(), e.name()));
                        ordinal = scanner.nextInt();
                        scanner.nextLine();
                        if (ordinal < 0 || ordinal >= command.options()
                                                             .size())
                        {
                            System.err.println("[ERROR] Invalid Option");
                        }
                        System.out.println();
                    }
                }

                switch (command) {
                    case CREATE -> {
                        try (final CreateEvent createEvent = new CreateEvent(scanner, CreateOptions.values()[ordinal])) {
                            createEvent.execute();
                        }
                    }
                    case UPDATE -> {
                        try (final UpdateEvent updateEvent = new UpdateEvent(scanner)) {
                            updateEvent.execute();
                        }
                    }
                    case DELETE -> {
                        try (final DeleteEvent deleteEvent = new DeleteEvent(scanner)) {
                            deleteEvent.execute();
                        }
                    }
                    case EXIT -> {
                        return;
                    }
                    default -> throw new IllegalStateException("Unhandled Command: %s".formatted(command.name()));
                }
            }
        } catch (final Throwable e) {
            System.err.printf("[ERROR] %s%n", e.getMessage());
        }
    }
}
