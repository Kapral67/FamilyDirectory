package org.familydirectory.assets.lambda.function.api.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.models.DdbTableParameter;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.WARN;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;

public final
class GetHelper extends ApiHelper {
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();

    public
    GetHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) {
        super(logger, requestEvent);
    }

    @NotNull
    public
    String getResponseBody (final @NotNull Caller caller) throws JsonProcessingException {
        final Map<String, Object> responseBodyMap = new HashMap<>();
        final String queryStringId = Optional.ofNullable(this.requestEvent.getQueryStringParameters())
                                             .map(m -> m.get(DdbTableParameter.PK.getName()))
                                             .orElse(null);
        final @NotNull MemberRecord memberRecord;
        final @NotNull Map<String, AttributeValue> family;
        if (isNull(queryStringId) || queryStringId.isBlank()) {
            memberRecord = caller.caller();
            family = requireNonNull(this.getDdbItem(caller.caller().familyId().toString(), DdbTable.FAMILY));
        } else {
            try {
                memberRecord = MemberRecord.convertDdbMap(requireNonNull(this.getDdbItem(queryStringId, DdbTable.MEMBER)));
            } catch (final RuntimeException e) {
                this.logger.log("<MEMBER,`%s`> Requested <MEMBER,`%s`> NOT FOUND".formatted(caller.caller().member().toString(), queryStringId), WARN);
                throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_NOT_FOUND));
            }
            family = requireNonNull(this.getDdbItem(memberRecord.familyId().toString(), DdbTable.FAMILY));
        }
        final Map<String, Object> memberObject = this.getResponseObject(memberRecord);

        responseBodyMap.put("ancestor", family.get(FamilyTableParameter.ANCESTOR.jsonFieldName())
                                              .s());
        responseBodyMap.put("member", memberObject);

        for (final FamilyTableParameter param : FamilyTableParameter.values()) {
            switch (param) {
                case ID, ANCESTOR -> {
                }
                case SPOUSE -> {
                    if (memberObject.get(MemberTableParameter.ID.jsonFieldName())
                                    .equals(memberObject.get(MemberTableParameter.FAMILY_ID.jsonFieldName())))
                    {
                        Optional.ofNullable(family.get(param.jsonFieldName()))
                                .map(AttributeValue::s)
                                .ifPresent(s -> responseBodyMap.put(param.jsonFieldName(), this.getResponseObject(MemberRecord.convertDdbMap(requireNonNull(this.getDdbItem(s, DdbTable.MEMBER))))));
                    } else {
                        responseBodyMap.put(param.jsonFieldName(), this.getResponseObject(MemberRecord.convertDdbMap(requireNonNull(this.getDdbItem(family.get(FamilyTableParameter.ID.jsonFieldName())
                                                                                                                                                          .s(), DdbTable.MEMBER)))));
                    }
                }
                case DESCENDANTS -> Optional.ofNullable(family.get(param.jsonFieldName())).map(AttributeValue::ss)
                                                                                 .filter(Predicate.not(List::isEmpty))
                                                                                 .ifPresent(l -> responseBodyMap.put(param.jsonFieldName(), this.getDescendantsObject(l)));
                default -> this.logger.log("FamilyTableParameter `%s` Not Handled in GET_MEMBER".formatted(param.name()), WARN);
            }
        }

        responseBodyMap.put("memberIsAdmin", this.isMemberAdmin(memberRecord.id().toString()));

        return this.objectMapper.writeValueAsString(responseBodyMap);
    }

    @NotNull
    @UnmodifiableView
    private
    Map<String, Object> getResponseObject (final @NotNull MemberRecord memberRecord) {
        final Map<String, Object> responseObject = new HashMap<>();
        for (final MemberTableParameter param : MemberTableParameter.values()) {
            switch (param) {
                case VCARD, ETAG -> {}
                case ID -> responseObject.put(param.jsonFieldName(), memberRecord.id().toString());
                case FAMILY_ID -> responseObject.put(param.jsonFieldName(), memberRecord.familyId().toString());
                case FIRST_NAME -> responseObject.put(param.jsonFieldName(), memberRecord.member().getFirstName());
                case MIDDLE_NAME -> Optional.ofNullable(memberRecord.member().getMiddleName())
                                            .ifPresent(s -> responseObject.put(param.jsonFieldName(), s));
                case LAST_NAME -> responseObject.put(param.jsonFieldName(), memberRecord.member().getLastName());
                case SUFFIX -> Optional.ofNullable(memberRecord.member().getSuffix())
                                       .ifPresent(t -> responseObject.put(param.jsonFieldName(), t.value()));
                case BIRTHDAY -> responseObject.put(param.jsonFieldName(), memberRecord.member().getBirthdayString());
                case DEATHDAY -> Optional.ofNullable(memberRecord.member().getDeathdayString())
                                         .ifPresent(s -> responseObject.put(param.jsonFieldName(), s));
                case EMAIL -> Optional.ofNullable(memberRecord.member().getEmail())
                                      .ifPresent(s -> responseObject.put(param.jsonFieldName(), s));
                case PHONES -> Optional.ofNullable(memberRecord.member().getPhones())
                                       .ifPresent(m -> responseObject.put(param.jsonFieldName(), m.entrySet()
                                                                                                                       .stream()
                                                                                                                       .collect(Collectors.toUnmodifiableMap(
                                                                                                                           entry -> entry.getKey().getJson(),
                                                                                                                           Map.Entry::getValue
                                                                                                                       ))));
                case ADDRESS -> Optional.ofNullable(memberRecord.member().getAddress()).ifPresent(l -> responseObject.put(param.jsonFieldName(), l));
                default -> this.logger.log("MemberTableParameter `%s` Not Handled in GET_MEMBER".formatted(param.name()), WARN);
            }
        }
        return Collections.unmodifiableMap(responseObject);
    }

    @NotNull
    @UnmodifiableView
    private
    List<Map<String, Object>> getDescendantsObject (final @NotNull List<String> descendantIds) {
        final Comparator<Map<String, AttributeValue>> comparator = Comparator.comparing(entry -> Member.convertStringToDate(entry.get(MemberTableParameter.BIRTHDAY.jsonFieldName())
                                                                                                                                 .s()));
        final List<Map<String, AttributeValue>> descendantDdbMap = new ArrayList<>();
        descendantIds.stream()
                     .filter(Predicate.not(String::isBlank))
                     .forEach(s -> descendantDdbMap.add(requireNonNull(this.getDdbItem(s, DdbTable.MEMBER))));
        descendantDdbMap.sort(comparator);
        final List<Map<String, Object>> descendantsObject = new ArrayList<>();
        descendantDdbMap.forEach(ddb -> descendantsObject.add(this.getResponseObject(MemberRecord.convertDdbMap(ddb))));
        return Collections.unmodifiableList(descendantsObject);
    }
}
