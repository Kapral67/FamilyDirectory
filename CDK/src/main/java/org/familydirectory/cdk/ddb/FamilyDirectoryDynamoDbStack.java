package org.familydirectory.cdk.ddb;

import org.familydirectory.assets.ddb.enums.DdbTable;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.constructs.Construct;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.nonNull;
import static org.familydirectory.assets.ddb.enums.DdbTable.PK;
import static org.familydirectory.assets.ddb.enums.DdbTable.values;
import static software.amazon.awscdk.services.dynamodb.BillingMode.PAY_PER_REQUEST;
import static software.amazon.awscdk.services.dynamodb.TableEncryption.AWS_MANAGED;

public class FamilyDirectoryDynamoDbStack extends Stack {
    public FamilyDirectoryDynamoDbStack(final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        for (final DdbTable ddbtable : values()) {
            final TableProps tableProps = TableProps.builder().tableName(ddbtable.name()).partitionKey(PK)
                                                    .billingMode(PAY_PER_REQUEST).encryption(AWS_MANAGED)
                                                    .pointInTimeRecovery(TRUE).deletionProtection(TRUE).build();
            Table table = new Table(this, ddbtable.name(), tableProps);
            if (nonNull(ddbtable.globalSecondaryIndexProps())) {
                table.addGlobalSecondaryIndex(ddbtable.globalSecondaryIndexProps());
            }
            new CfnOutput(this, ddbtable.arnExportName(), CfnOutputProps.builder().value(table.getTableArn())
                                                                        .exportName(ddbtable.arnExportName()).build());
        }
    }
}
