package org.familydirectory.assets.lambda.function.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.models.DdbTableParameter;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import static java.lang.System.getenv;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public
interface LambdaFunctionHelper extends SdkAutoCloseable {
    @NotNull
    LambdaLogger getLogger ();

    @NotNull
    default
    String getPdfS3Key (final @NotNull String rootMemberSurname) {
        return "%sFamily.zip".formatted(requireNonNull(rootMemberSurname));
    }

    @NotNull
    default
    String getRootMemberSurname () {
        return ofNullable(this.getDdbItem(requireNonNull(getenv(LambdaUtils.EnvVar.ROOT_ID.name())), DdbTable.MEMBER)).map(m -> m.get(MemberTableParameter.LAST_NAME.jsonFieldName()))
                                                                                                                      .map(AttributeValue::s)
                                                                                                                      .filter(Predicate.not(String::isBlank))
                                                                                                                      .orElseThrow();
    }

    @Nullable
    default
    Map<String, AttributeValue> getDdbItem (final @NotNull String primaryKey, final @NotNull DdbTable ddbTable) {
        final GetItemRequest request = GetItemRequest.builder()
                                                     .tableName(ddbTable.name())
                                                     .key(singletonMap(DdbTableParameter.PK.getName(), AttributeValue.fromS(primaryKey)))
                                                     .build();
        final GetItemResponse response = this.getDynamoDbClient()
                                             .getItem(request);
        return (response.item()
                        .isEmpty())
                ? null
                : response.item();
    }

    @Nullable
    default
    List<Map<String, AttributeValue>> queryGsi (final @NotNull Map.Entry<String, String> attribute, final @NotNull String indexName, final @NotNull DdbTable ddbTable) {
        final QueryRequest request = QueryRequest.builder()
                                                 .tableName(ddbTable.name())
                                                 .indexName(indexName)
                                                 .keyConditionExpression("#key = :val")
                                                 .expressionAttributeNames(singletonMap("#key", attribute.getKey()))
                                                 .expressionAttributeValues(singletonMap(":val", AttributeValue.fromS(attribute.getValue())))
                                                 .build();
        return Optional.ofNullable(this.getDynamoDbClient().query(request))
                       .map(QueryResponse::items)
                       .filter(Predicate.not(List::isEmpty))
                       .orElse(null);
    }

    @NotNull
    DynamoDbClient getDynamoDbClient ();

    @Override
    default
    void close () {
        this.getDynamoDbClient()
            .close();
    }
}
