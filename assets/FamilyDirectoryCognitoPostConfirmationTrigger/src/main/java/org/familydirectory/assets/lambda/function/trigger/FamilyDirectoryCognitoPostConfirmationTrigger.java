package org.familydirectory.assets.lambda.function.trigger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostConfirmationEvent;
import java.util.Map;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.ERROR;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.FATAL;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public
class FamilyDirectoryCognitoPostConfirmationTrigger implements RequestHandler<CognitoUserPoolPostConfirmationEvent, CognitoUserPoolPostConfirmationEvent> {

    @Override
    public final @NotNull
    CognitoUserPoolPostConfirmationEvent handleRequest (final @NotNull CognitoUserPoolPostConfirmationEvent event, final @NotNull Context context)
    {
        try (final CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.create(); final DynamoDbClient dynamoDbClient = DynamoDbClient.create()) {
            final LambdaLogger logger = context.getLogger();
            final String email = ofNullable(event.getRequest()
                                                 .getUserAttributes()
                                                 .get("email")).filter(s -> s.contains("@"))
                                                               .map(String::toLowerCase)
                                                               .orElseThrow(() -> {
                                                                   logger.log("Cognito <USER,`%s`> Email Not Found".formatted(event.getUserName()), ERROR);
                                                                   final IllegalStateException e = new IllegalStateException();
                                                                   adminDisableUser(cognitoClient, logger, event.getUserPoolId(), event.getUserName(), null, e);
                                                                   return e;
                                                               });
            final String sub = ofNullable(event.getRequest()
                                               .getUserAttributes()
                                               .get("sub")).filter(Predicate.not(String::isBlank))
                                                           .orElseThrow(() -> {
                                                               logger.log("Cognito <EMAIL,`%s`> Sub Not Found".formatted(email), ERROR);
                                                               final IllegalStateException e = new IllegalStateException();
                                                               adminDisableUser(cognitoClient, logger, event.getUserPoolId(), event.getUserName(), email, e);
                                                               return e;
                                                           });

            //  Check if Entry in Cognito Ddb Already Exists
            try {
                if (!dynamoDbClient.getItem(GetItemRequest.builder()
                                                          .tableName(DdbTable.COGNITO.name())
                                                          .key(singletonMap(CognitoTableParameter.ID.jsonFieldName(), AttributeValue.fromS(sub)))
                                                          .build())
                                   .item()
                                   .isEmpty())
                {
//              If There Is a PreExisting Entry, Then The User is Just Changing Their Email For Cognito
                    return getValidEvent(event, email);
                }
            } catch (final Throwable e) {
                adminDisableUser(cognitoClient, logger, event.getUserPoolId(), event.getUserName(), email, e);
                LambdaUtils.logTrace(logger, e, ERROR);
                throw e;
            }

            //  Find Member By Email
            final QueryRequest memberEmailQueryRequest = QueryRequest.builder()
                                                                     .tableName(DdbTable.MEMBER.name())
                                                                     .indexName(requireNonNull(MemberTableParameter.EMAIL.gsiProps()).getIndexName())
                                                                     .keyConditionExpression("%s = :email".formatted(MemberTableParameter.EMAIL.gsiProps()
                                                                                                                                               .getPartitionKey()
                                                                                                                                               .getName()))
                                                                     .expressionAttributeValues(singletonMap(":email", AttributeValue.fromS(email)))
                                                                     .limit(2)
                                                                     .build();
            final QueryResponse memberEmailQueryResponse = dynamoDbClient.query(memberEmailQueryRequest);
            if (memberEmailQueryResponse.items()
                                        .isEmpty())
            {
                logger.log("No Member Found for <EMAIL,`%s`>".formatted(email), ERROR);
                final IllegalStateException e = new IllegalStateException();
                adminDisableUser(cognitoClient, logger, event.getUserPoolId(), event.getUserName(), email, e);
                throw e;
            } else if (memberEmailQueryResponse.items()
                                               .size() > 1)

            {
                logger.log("Multiple Members Found for <EMAIL,`%s`>".formatted(email), ERROR);
                final IllegalStateException e = new IllegalStateException();
                adminDisableUser(cognitoClient, logger, event.getUserPoolId(), event.getUserName(), email, e);
                throw e;
            }
            final String memberId = ofNullable(memberEmailQueryResponse.items()
                                                                       .getFirst()
                                                                       .get(MemberTableParameter.ID.jsonFieldName())).map(AttributeValue::s)
                                                                                                                     .filter(Predicate.not(String::isBlank))
                                                                                                                     .orElseThrow();
            //  Map Cognito Sub -> Member Id
            try {
                dynamoDbClient.putItem(PutItemRequest.builder()
                                                     .tableName(DdbTable.COGNITO.name())
                                                     .item(Map.of(CognitoTableParameter.ID.jsonFieldName(), AttributeValue.fromS(sub), CognitoTableParameter.MEMBER.jsonFieldName(),
                                                                  AttributeValue.fromS(memberId)))
                                                     .build());
            } catch (final Throwable e) {
                adminDisableUser(cognitoClient, logger, event.getUserPoolId(), event.getUserName(), email, e);
                LambdaUtils.logTrace(logger, e, ERROR);
                throw e;
            }

            return getValidEvent(event, email);
        } catch (final Throwable e) {
            throw new Error("Request Denied", e);
        }
    }

    private static @NotNull
    CognitoUserPoolPostConfirmationEvent getValidEvent (final @NotNull CognitoUserPoolPostConfirmationEvent event, final @NotNull String email) {
        return CognitoUserPoolPostConfirmationEvent.builder()
                                                   .withVersion(event.getVersion())
                                                   .withTriggerSource(event.getTriggerSource())
                                                   .withRegion(event.getRegion())
                                                   .withUserPoolId(event.getUserPoolId())
                                                   .withUserName(event.getUserName())
                                                   .withCallerContext(event.getCallerContext())
                                                   .withRequest(CognitoUserPoolPostConfirmationEvent.Request.builder()
                                                                                                            .withClientMetadata(event.getRequest()
                                                                                                                                     .getClientMetadata())
                                                                                                            .withUserAttributes(Map.of("email", email, "email_verified", "true"))
                                                                                                            .build())
                                                   .build();
    }

    private static
    void adminDisableUser (final @NotNull CognitoIdentityProviderClient cognitoClient, final @NotNull LambdaLogger logger, final @NotNull String userPoolId, final @NotNull String userName,
                           final @Nullable String email, final @Nullable Throwable e)
    {
        try {
            try {
                cognitoClient.adminDisableUser(AdminDisableUserRequest.builder()
                                                                      .userPoolId(userPoolId)
                                                                      .username(userName)
                                                                      .build());
            } catch (final Throwable x) {
                logger.log("Failed to Disable User <USERNAME,`%s`>".formatted(userName), FATAL);
                throw x;
            }
            ofNullable(email).ifPresent(em -> {
                final String body = "Your Account Has Been Suspended. Please Contact Your Administrator For Assistance.";
                final String emailId = LambdaUtils.sendEmail(singletonList(em), "Notice of Account Suspension", body);
                logger.log("Sent Account Suspension Notice To <EMAIL,`%s`>: <MESSAGE_ID,`%s`>".formatted(em, emailId), INFO);
            });
        } catch (final Throwable x) {
            ofNullable(e).ifPresent(t -> t.addSuppressed(x));
            LambdaUtils.logTrace(logger, x, ERROR);
        }
    }
}
