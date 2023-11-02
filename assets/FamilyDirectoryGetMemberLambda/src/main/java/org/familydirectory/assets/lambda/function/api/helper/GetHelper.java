package org.familydirectory.assets.lambda.function.api.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.models.DdbTableParameter;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.WARN;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;

public final
class GetHelper extends ApiHelper {
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final @NotNull LambdaLogger logger;
    private final @NotNull APIGatewayProxyRequestEvent requestEvent;

    public
    GetHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) {
        super();
        this.logger = requireNonNull(logger);
        this.requestEvent = requireNonNull(requestEvent);
    }

    @NotNull
    public
    String getResponseBody (final @NotNull Caller caller) throws JsonProcessingException {
        final String queryStringId = ofNullable(this.requestEvent.getQueryStringParameters()).map(m -> m.get(DdbTableParameter.PK.getName()))
                                                                                             .orElse(null);
        final @NotNull Member member;
        final @NotNull Map<String, AttributeValue> memberMap;
        final @NotNull Map<String, AttributeValue> family;
        if (isNull(queryStringId) || queryStringId.isBlank()) {
            memberMap = caller.attributeMap();
            member = Member.convertDdbMap(memberMap);
            family = requireNonNull(this.getDdbItem(caller.familyId(), DdbTable.FAMILY));
        } else {
            memberMap = ofNullable(this.getDdbItem(queryStringId, DdbTable.MEMBER)).orElseThrow(() -> {
                this.logger.log("<MEMBER,`%s`> Requested <MEMBER,`%s`> NOT FOUND".formatted(caller.memberId(), queryStringId), WARN);
                return new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_NOT_FOUND));
            });
            member = Member.convertDdbMap(memberMap);
            family = requireNonNull(this.getDdbItem(memberMap.get(MemberTableParameter.FAMILY_ID.jsonFieldName())
                                                             .s(), DdbTable.FAMILY));
        }

        final Map<String, Object> memberResponseBody = new HashMap<>();
        for (final MemberTableParameter param : MemberTableParameter.values()) {
            switch (param) {
                case ID, KEY, FAMILY_ID -> memberResponseBody.put(param.jsonFieldName(), memberMap.get(param.jsonFieldName())
                                                                                                  .s());
                case FIRST_NAME -> memberResponseBody.put(param.jsonFieldName(), member.getFirstName());
                case MIDDLE_NAME -> ofNullable(member.getMiddleName()).ifPresent(s -> memberResponseBody.put(param.jsonFieldName(), s));
                case LAST_NAME -> memberResponseBody.put(param.jsonFieldName(), member.getLastName());
                case SUFFIX -> ofNullable(member.getSuffix()).ifPresent(t -> memberResponseBody.put(param.jsonFieldName(), t.value()));
                case BIRTHDAY -> memberResponseBody.put(param.jsonFieldName(), member.getBirthdayString());
                case DEATHDAY -> ofNullable(member.getDeathdayString()).ifPresent(s -> memberResponseBody.put(param.jsonFieldName(), s));
                case EMAIL -> ofNullable(member.getEmail()).ifPresent(s -> memberResponseBody.put(param.jsonFieldName(), s));
                case PHONES -> ofNullable(member.getPhones()).ifPresent(m -> memberResponseBody.put(param.jsonFieldName(), m.entrySet()
                                                                                                                            .stream()
                                                                                                                            .collect(Collectors.toUnmodifiableMap(entry -> entry.getKey()
                                                                                                                                                                                .getJson(),
                                                                                                                                                                  Map.Entry::getValue))));
                case ADDRESS -> ofNullable(member.getAddress()).ifPresent(l -> memberResponseBody.put(param.jsonFieldName(), l));
                default -> this.logger.log("MemberTableParameter `%s` Not Handled in GET_MEMBER".formatted(param.name()), WARN);
            }
        }

        final Map<String, Object> familyResponseBody = new HashMap<>();
        for (final FamilyTableParameter param : FamilyTableParameter.values()) {
            switch (param) {
                case ID, ANCESTOR -> familyResponseBody.put(param.jsonFieldName(), family.get(param.jsonFieldName())
                                                                                         .s());
                case SPOUSE -> ofNullable(family.get(param.jsonFieldName())).map(AttributeValue::s)
                                                                            .ifPresent(s -> familyResponseBody.put(param.jsonFieldName(), s));
                case DESCENDANTS -> ofNullable(family.get(param.jsonFieldName())).map(AttributeValue::ss)
                                                                                 .filter(Predicate.not(List::isEmpty))
                                                                                 .ifPresent(l -> familyResponseBody.put(param.jsonFieldName(), l));
                default -> this.logger.log("FamilyTableParameter `%s` Not Handled in GET_MEMBER".formatted(param.name()), WARN);
            }
        }

        return new ObjectMapper().writeValueAsString(Map.of("member", memberResponseBody, "family", familyResponseBody));
    }

    @Override
    public @NotNull
    APIGatewayProxyRequestEvent getRequestEvent () {
        return this.requestEvent;
    }

    @Override
    public @NotNull
    LambdaLogger getLogger () {
        return this.logger;
    }

    @Override
    public @NotNull
    DynamoDbClient getDynamoDbClient () {
        return this.dynamoDbClient;
    }
}
