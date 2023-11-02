package org.familydirectory.assets.lambda.function.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.util.Map;
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
import software.amazon.awssdk.utils.SdkAutoCloseable;
import static java.lang.System.getenv;
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;

public
interface LambdaFunctionHelper extends SdkAutoCloseable {
    @NotNull
    LambdaLogger getLogger ();

    @NotNull
    default
    String getPdfS3Key () {
        final String rootMemberSurname = ofNullable(this.getDdbItem(getenv(LambdaUtils.EnvVar.ROOT_ID.name()), DdbTable.MEMBER)).map(m -> m.get(MemberTableParameter.LAST_NAME.jsonFieldName()))
                                                                                                                                .map(AttributeValue::s)
                                                                                                                                .filter(Predicate.not(String::isBlank))
                                                                                                                                .orElseThrow();
        return "%sFamilyDirectory.pdf".formatted(rootMemberSurname);
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

    @NotNull
    DynamoDbClient getDynamoDbClient ();

    @Override
    default
    void close () {
        this.getDynamoDbClient()
            .close();
    }
}
