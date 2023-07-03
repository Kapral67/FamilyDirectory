package org.familydirectory.cdk.ddb;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.dynamodb.*;
import software.constructs.Construct;

import static java.lang.Boolean.TRUE;
import static org.familydirectory.cdk.ddb.DdbTable.PK;
import static software.amazon.awscdk.services.dynamodb.BillingMode.PAY_PER_REQUEST;
import static software.amazon.awscdk.services.dynamodb.TableEncryption.AWS_MANAGED;

public class FamilyDirectoryCdkDynamoDbStack extends Stack {
    public FamilyDirectoryCdkDynamoDbStack(final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        for(final DdbTable ddbtable : DdbTable.values()) {
            final TableProps tableProps = TableProps.builder()
                    .tableName(ddbtable.name())
                    .partitionKey(PK)
                    .sortKey(ddbtable.sortKey())
                    .billingMode(PAY_PER_REQUEST)
                    .encryption(AWS_MANAGED)
                    .pointInTimeRecovery(TRUE)
                    .deletionProtection(TRUE)
                    .build();
            Table table = new Table(this, ddbtable.name(), tableProps);
            new CfnOutput(this, ddbtable.arnExportName(), CfnOutputProps.builder()
                    .value(table.getTableArn())
                    .exportName(ddbtable.arnExportName())
                    .build());
        }
    }
}
