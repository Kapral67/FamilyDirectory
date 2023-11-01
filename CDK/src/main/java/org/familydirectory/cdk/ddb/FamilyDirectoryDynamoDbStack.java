package org.familydirectory.cdk.ddb;

import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.models.DdbTableParameter;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.StreamViewType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.constructs.Construct;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static software.amazon.awscdk.services.dynamodb.BillingMode.PAY_PER_REQUEST;
import static software.amazon.awscdk.services.dynamodb.TableEncryption.AWS_MANAGED;

public
class FamilyDirectoryDynamoDbStack extends Stack {
    public
    FamilyDirectoryDynamoDbStack (final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        for (final DdbTable ddbtable : DdbTable.values()) {
            final TableProps.Builder tablePropsBuilder = TableProps.builder()
                                                                   .tableName(ddbtable.name())
                                                                   .partitionKey(DdbTableParameter.PK)
                                                                   .billingMode(PAY_PER_REQUEST)
                                                                   .encryption(AWS_MANAGED)
                                                                   .pointInTimeRecovery(TRUE)
                                                                   .deletionProtection(TRUE);
            if (ddbtable.hasStream()) {
                tablePropsBuilder.stream(StreamViewType.KEYS_ONLY);
            }
            final TableProps tableProps = tablePropsBuilder.build();
            final Table table = new Table(this, ddbtable.name(), tableProps);
            for (final DdbTableParameter param : ddbtable.parameters()) {
                ofNullable(param.gsiProps()).ifPresent(table::addGlobalSecondaryIndex);
            }
            new CfnOutput(this, ddbtable.arnExportName(), CfnOutputProps.builder()
                                                                        .value(table.getTableArn())
                                                                        .exportName(ddbtable.arnExportName())
                                                                        .build());
            if (ddbtable.hasStream()) {
                new CfnOutput(this, requireNonNull(ddbtable.streamArnExportName()), CfnOutputProps.builder()
                                                                                                  .value(table.getTableStreamArn())
                                                                                                  .exportName(ddbtable.streamArnExportName())
                                                                                                  .build());
            }
        }
    }
}
