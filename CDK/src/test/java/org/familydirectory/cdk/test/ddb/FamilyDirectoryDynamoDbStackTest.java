package org.familydirectory.cdk.test.ddb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.models.DdbTableParameter;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.ddb.FamilyDirectoryDynamoDbStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Template;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.awscdk.assertions.Match.objectLike;

public
class FamilyDirectoryDynamoDbStackTest {
    @Test
    public
    void testStack () {
        final App app = new App();

        final FamilyDirectoryDynamoDbStack stack = new FamilyDirectoryDynamoDbStack(app, FamilyDirectoryCdkApp.DDB_STACK_NAME, StackProps.builder()
                                                                                                                                         .env(FamilyDirectoryCdkApp.DEFAULT_ENV)
                                                                                                                                         .stackName(FamilyDirectoryCdkApp.DDB_STACK_NAME)
                                                                                                                                         .build());

        final Template template = Template.fromStack(stack);

        for (final DdbTable ddbTable : DdbTable.values()) {
            final List<Map<String, String>> attributeDefinitions = new ArrayList<>();
            attributeDefinitions.add(Map.of("AttributeName", DdbTableParameter.PK.getName(), "AttributeType", getAttributeType(DdbTableParameter.PK.getType())));
            final List<Map<String, Object>> globalSecondaryIndexes = new ArrayList<>();
            for (final DdbTableParameter param : ddbTable.parameters()) {
                ofNullable(param.gsiProps()).ifPresent(gsi -> {
                    attributeDefinitions.add(Map.of("AttributeName", gsi.getPartitionKey()
                                                                        .getName(), "AttributeType", getAttributeType(gsi.getPartitionKey()
                                                                                                                         .getType())));
                    globalSecondaryIndexes.add(Map.of("IndexName", gsi.getIndexName(), "KeySchema", singletonList(Map.of("AttributeName", gsi.getPartitionKey()
                                                                                                                                             .getName(), "KeyType", "HASH")), "Projection",
                                                      singletonMap("ProjectionType", requireNonNull(gsi.getProjectionType()).name())));
                });
            }
            final Map<String, Object> tableMapProperties = new HashMap<>();
            tableMapProperties.put("AttributeDefinitions", attributeDefinitions);
            tableMapProperties.put("BillingMode", "PAY_PER_REQUEST");
            tableMapProperties.put("DeletionProtectionEnabled", true);
            if (!globalSecondaryIndexes.isEmpty()) {
                tableMapProperties.put("GlobalSecondaryIndexes", globalSecondaryIndexes);
            }
            tableMapProperties.put("KeySchema", singletonList(Map.of("AttributeName", DdbTableParameter.PK.getName(), "KeyType", "HASH")));
            tableMapProperties.put("PointInTimeRecoverySpecification", singletonMap("PointInTimeRecoveryEnabled", true));
            tableMapProperties.put("SSESpecification", singletonMap("SSEEnabled", true));
            if (ddbTable.hasStream()) {
                tableMapProperties.put("StreamSpecification", singletonMap("StreamViewType", "KEYS_ONLY"));
            }
            tableMapProperties.put("TableName", ddbTable.name());
            final Map<String, Map<String, Object>> tableMap = template.findResources("AWS::DynamoDB::Table", objectLike(singletonMap("Properties", tableMapProperties)));
            assertEquals(1, tableMap.size());

            final String tableId = tableMap.entrySet()
                                           .iterator()
                                           .next()
                                           .getKey();
            template.hasOutput(ddbTable.arnExportName(), objectLike(Map.of("Value", singletonMap("Fn::GetAtt", List.of(tableId, "Arn")), "Export", singletonMap("Name", ddbTable.arnExportName()))));
            if (ddbTable.hasStream()) {
                template.hasOutput(requireNonNull(ddbTable.streamArnExportName()),
                                   objectLike(Map.of("Value", singletonMap("Fn::GetAtt", List.of(tableId, "StreamArn")), "Export", singletonMap("Name", ddbTable.streamArnExportName()))));
            }
        }
    }

    @NotNull
    private static
    String getAttributeType (final @NotNull AttributeType attr) {
        if (attr == AttributeType.STRING) {
            return "S";
        }
        throw new NotImplementedException();
    }
}
