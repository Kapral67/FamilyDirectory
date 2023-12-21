package org.familydirectory.sdk.adminclient.events.toolkitcleaner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leego.banana.Ansi;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Predicate;
import org.familydirectory.sdk.adminclient.events.model.EventHelper;
import org.familydirectory.sdk.adminclient.utility.Logger;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartSyncExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StateMachineListItem;
import static java.util.Objects.requireNonNull;

public final
class ToolkitCleanerEvent implements EventHelper {
    private static final double BYTES_IN_GIGABYTE = Math.pow(1024.0, 3.0);
    private final @NotNull SfnClient sfnClient = SfnClient.create();
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull Scanner scanner;

    public
    ToolkitCleanerEvent (final @NotNull Scanner scanner) {
        super();
        this.scanner = requireNonNull(scanner);
    }

    @Override
    public @NotNull
    Scanner scanner () {
        return this.scanner;
    }

    @Override
    @Deprecated
    @NotNull
    public
    DynamoDbClient getDynamoDbClient () {
        throw new UnsupportedOperationException("DynamoDbClient Not Implemented");
    }

    @Override
    public
    void close () {
        this.sfnClient.close();
    }

    @Override
    public
    void execute () {
        Logger.customLine("Clean CDK S3 Assets? (Y/n)", Ansi.BOLD, Ansi.BLUE);
        final String choice = this.scanner.nextLine()
                                          .trim();
        if (!choice.isBlank()) {
            System.out.println();
            if (choice.equalsIgnoreCase("n")) {
                return;
            }
        }

        final String stateMachineArn = this.sfnClient.listStateMachines()
                                                     .stateMachines()
                                                     .stream()
                                                     .map(StateMachineListItem::stateMachineArn)
                                                     .filter(Predicate.not(String::isBlank))
                                                     .findFirst()
                                                     .orElseThrow();

        final Map<String, Number> outputMap;
        try {
            outputMap = this.objectMapper.readValue(requireNonNull(this.sfnClient.startSyncExecution(StartSyncExecutionRequest.builder()
                                                                                                                              .stateMachineArn(stateMachineArn)
                                                                                                                              .build())
                                                                                 .output(), "StateMachine Execution Failed"), new TypeReference<>() {
            });
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Logger.customLine("Deleted Items: %d | Reclaimed Space: %f GB".formatted(outputMap.get("Deleted")
                                                                                          .intValue(), outputMap.get("Reclaimed")
                                                                                                                .doubleValue() / BYTES_IN_GIGABYTE), Ansi.BOLD, Ansi.PURPLE);
        System.out.println();
    }
}
