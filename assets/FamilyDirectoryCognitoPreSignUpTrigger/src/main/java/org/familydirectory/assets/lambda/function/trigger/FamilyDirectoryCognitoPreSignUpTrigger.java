package org.familydirectory.assets.lambda.function.trigger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreSignUpEvent;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.ERROR;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.WARN;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public
class FamilyDirectoryCognitoPreSignUpTrigger implements RequestHandler<CognitoUserPoolPreSignUpEvent, CognitoUserPoolPreSignUpEvent> {

    private static final DynamoDbClient DDB_CLIENT = DynamoDbClient.create();

    @Override
    public
    CognitoUserPoolPreSignUpEvent handleRequest (CognitoUserPoolPreSignUpEvent event, Context context)
    {
        final LambdaLogger logger = context.getLogger();

        final String email = ofNullable(event.getRequest()
                                             .getUserAttributes()
                                             .get("email")).filter(Predicate.not(String::isBlank))
                                                           .orElseThrow();

        logger.log("PROCESS: PreSignUp Event for <EMAIL,`%s`>".formatted(email), INFO);

//  Find Member By Email
        final QueryRequest memberEmailQueryRequest = QueryRequest.builder()
                                                                 .tableName(DdbTable.MEMBER.name())
                                                                 .indexName(requireNonNull(MemberTableParameter.EMAIL.gsiProps()).getIndexName())
                                                                 .keyConditionExpression("%s = :email".formatted(MemberTableParameter.EMAIL.jsonFieldName()))
                                                                 .expressionAttributeValues(singletonMap(":email", AttributeValue.fromS(email)))
                                                                 .limit(2)
                                                                 .build();
        final QueryResponse memberEmailQueryResponse = DDB_CLIENT.query(memberEmailQueryRequest);
        if (!memberEmailQueryResponse.hasItems()) {
            logger.log("REJECT: PreSignUp Event for <EMAIL,`%s`> - No Member Found".formatted(email), WARN);
            throw new NoSuchElementException("No Member Found for Email: %s".formatted(email));
        } else if (memberEmailQueryResponse.items()
                                           .size() > 1)

        {
            logger.log("REJECT: PreSignUp Event for <EMAIL,`%s`> - Multiple Members Found".formatted(email), ERROR);
            throw new IllegalStateException("Multiple Members Found for Email: %s".formatted(email));
        }
        final String memberId = ofNullable(memberEmailQueryResponse.items()
                                                                   .get(0)
                                                                   .get(MemberTableParameter.ID.jsonFieldName())).map(AttributeValue::s)
                                                                                                                 .filter(Predicate.not(String::isBlank))
                                                                                                                 .orElseThrow();
        logger.log("PROCESS: Found <MEMBER,`%s`> for <EMAIL,`%s`>".formatted(memberId, email), INFO);

//  Check If Member Signed Up Previously
        final QueryRequest cognitoMemberQueryRequest = QueryRequest.builder()
                                                                   .tableName(DdbTable.COGNITO.name())
                                                                   .indexName(requireNonNull(CognitoTableParameter.MEMBER.gsiProps()).getIndexName())
                                                                   .keyConditionExpression("%s = :member".formatted(CognitoTableParameter.MEMBER.jsonFieldName()))
                                                                   .expressionAttributeValues(singletonMap(":member", AttributeValue.fromS(memberId)))
                                                                   .limit(1)
                                                                   .build();
        final QueryResponse cognitoMemberQueryResponse = DDB_CLIENT.query(cognitoMemberQueryRequest);
        if (cognitoMemberQueryResponse.hasItems()) {
            logger.log("REJECT: PreSignUp Event for <EMAIL,`%s`> - Member Already Signed Up".formatted(email), WARN);
            throw new IllegalStateException("Member Already Signed Up for Email: %s".formatted(email));
        }

        logger.log("ACCEPT: PreSignUp Event for <MEMBER,`%s`> with <EMAIL,`%s`>".formatted(memberId, email), INFO);

        return CognitoUserPoolPreSignUpEvent.builder()
                                            .withVersion(event.getVersion())
                                            .withTriggerSource(event.getTriggerSource())
                                            .withRegion(event.getRegion())
                                            .withUserPoolId(event.getUserPoolId())
                                            .withUserName(event.getUserName())
                                            .withCallerContext(event.getCallerContext())
                                            .withRequest(event.getRequest())
                                            .withResponse(CognitoUserPoolPreSignUpEvent.Response.builder()
                                                                                                .withAutoConfirmUser(false)
                                                                                                .withAutoVerifyEmail(false)
                                                                                                .withAutoVerifyPhone(false)
                                                                                                .build())
                                            .build();
    }
}
