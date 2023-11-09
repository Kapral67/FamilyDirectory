package org.familydirectory.sdk.adminclient.events.model;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.PhoneType;
import org.familydirectory.assets.ddb.enums.SuffixType;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.lambda.function.helper.LambdaFunctionHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import static java.lang.System.getenv;
import static java.util.Collections.singletonMap;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public
interface EventHelper extends LambdaFunctionHelper {
    String ROOT_ID = getenv("ORG_FAMILYDIRECTORY_ROOT_MEMBER_ID");

    @Override
    @NotNull
    default
    LambdaLogger getLogger () {
        throw new UnsupportedOperationException("Admin Client Does Not Implement LambdaLogger");
    }

    @Override
    @NotNull
    default
    String getPdfS3Key () {
        throw new UnsupportedOperationException("Admin Client Does Not Implement GET Requests");
    }

    @NotNull
    default
    MemberRecord buildMemberRecord (final @NotNull UUID memberId) {
        final Member.Builder memberBuilder = Member.builder();
        System.out.println("Please Standby to Build This Member...");
        boolean breakLoop = false;
        for (final MemberTableParameter param : MemberTableParameter.values()) {
            if (breakLoop) {
                break;
            }
            switch (param) {
                case FIRST_NAME -> {
                    System.out.printf("[Required] Please Enter %s:%n", param.jsonFieldName());
                    memberBuilder.firstName(this.scanner()
                                                .nextLine());
                }
                case MIDDLE_NAME -> {
                    System.out.printf("[Optional (Enter to skip)] Please Enter %s:%n", param.jsonFieldName());
                    final String middleName = this.scanner()
                                                  .nextLine()
                                                  .trim();
                    if (!middleName.isEmpty()) {
                        memberBuilder.middleName(middleName);
                    }
                }
                case LAST_NAME -> {
                    System.out.printf("[Required] Please Enter %s:%n", param.jsonFieldName());
                    memberBuilder.lastName(this.scanner()
                                               .nextLine());
                }
                case SUFFIX -> {
                    int ordinal = -1;
                    final SuffixType suffix;
                    while (ordinal < 0 || ordinal > SuffixType.values().length) {
                        System.out.println("[Optional] Please Choose A Suffix:");
                        for (final SuffixType sfx : SuffixType.values()) {
                            System.out.printf("%d) %s%n", sfx.ordinal(), sfx.name());
                        }
                        System.out.printf("%d) Skip%n", SuffixType.values().length);
                        ordinal = this.scanner()
                                      .nextInt();
                        this.scanner()
                            .nextLine();
                        if (ordinal < 0 || ordinal > SuffixType.values().length) {
                            System.err.println("[ERROR] Invalid Suffix");
                        }
                    }
                    if (ordinal != SuffixType.values().length) {
                        memberBuilder.suffix(SuffixType.values()[ordinal]);
                    }
                }
                case BIRTHDAY -> {
                    System.out.printf("[Required (Format is yyyy-MM-dd, e.g. 1970-12-31 -> Dec. 31, 1970)] Please Enter %s:%n", param.jsonFieldName());
                    memberBuilder.birthday(Member.convertStringToDate(this.scanner()
                                                                          .nextLine()
                                                                          .trim()));
                }
                case DEATHDAY -> {
                    System.out.printf("[Optional (Enter to skip) (Format is yyyy-MM-dd, e.g. 1970-12-31 -> Dec. 31, 1970)] Please Enter %s:%n", param.jsonFieldName());
                    final String deathDay = this.scanner()
                                                .nextLine()
                                                .trim();
                    if (!deathDay.isEmpty()) {
                        memberBuilder.deathday(Member.convertStringToDate(deathDay));
                        breakLoop = true;
                    }
                }
                case EMAIL -> {
                    System.out.printf("[Optional (Enter to skip)] Please Enter %s:%n", param.jsonFieldName());
                    final String email = this.scanner()
                                             .nextLine()
                                             .trim();
                    if (!email.isEmpty()) {
                        memberBuilder.email(email);
                    }
                }
                case PHONES -> {
                    System.out.printf("[Optional (y/N)] Add %s to this Member?%n", param.jsonFieldName());
                    final String addPhone = this.scanner()
                                                .nextLine()
                                                .trim();
                    if (addPhone.equalsIgnoreCase("y")) {
                        System.out.printf("Phone Numbers:%n\tFor US numbers:%n\t\t- Do Not Include + or Country Code%n\tFor International numbers:%n\t\t- + and Country Code are required%n%n");
                        final Map<PhoneType, String> phones = new HashMap<>();
                        for (final PhoneType phoneType : PhoneType.values()) {
                            System.out.printf("[Optional (Enter to skip)] Please Enter %s Phone Number:%n", phoneType.name());
                            final String phone = this.scanner()
                                                     .nextLine()
                                                     .trim();
                            if (!phone.isEmpty()) {
                                phones.put(phoneType, phone);
                            }
                        }
                        if (!phones.isEmpty()) {
                            memberBuilder.phones(phones);
                        }
                    }
                }
                case ADDRESS -> {
                    final List<String> addressLines = new ArrayList<>();
                    for (int i = 1; i <= Member.REQ_NON_NULL_ADDRESS_SIZE; ++i) {
                        System.out.printf("[Optional (Enter to skip)] Please Enter %s line %d:%n", param.jsonFieldName(), i);
                        final String addressLineText = this.scanner()
                                                           .nextLine()
                                                           .trim();
                        if (addressLineText.isEmpty()) {
                            break;
                        }
                        addressLines.add(addressLineText);
                    }
                    if (!addressLines.isEmpty()) {
                        memberBuilder.address(addressLines);
                    }
                }
                default -> {
                }
            }
        }
        return new MemberRecord(memberId, memberBuilder.build());
    }

    @NotNull
    Scanner scanner ();

    default
    void validateMemberEmailIsUnique (final @Nullable String memberEmail) {
        if (nonNull(memberEmail) && !memberEmail.isBlank()) {
            final QueryRequest emailRequest = QueryRequest.builder()
                                                          .tableName(DdbTable.MEMBER.name())
                                                          .indexName(requireNonNull(MemberTableParameter.EMAIL.gsiProps()).getIndexName())
                                                          .keyConditionExpression("%s = :email".formatted(MemberTableParameter.EMAIL.gsiProps()
                                                                                                                                    .getPartitionKey()
                                                                                                                                    .getName()))
                                                          .expressionAttributeValues(singletonMap(":email", AttributeValue.fromS(memberEmail)))
                                                          .limit(1)
                                                          .build();
            final QueryResponse emailResponse = this.getDynamoDbClient()
                                                    .query(emailRequest);
            if (!emailResponse.items()
                              .isEmpty())
            {
                final String emailResponseMemberId = emailResponse.items()
                                                                  .iterator()
                                                                  .next()
                                                                  .get(MemberTableParameter.ID.jsonFieldName())
                                                                  .s();

                throw new IllegalStateException("EMAIL %s already claimed by Existing Member".formatted(memberEmail));
            }
        }
    }

    @NotNull
    default
    Map<String, AttributeValue> buildMember (final @NotNull MemberRecord memberRecord, final @NotNull String familyId) {
        final Map<String, AttributeValue> member = new HashMap<>();

        for (final MemberTableParameter field : MemberTableParameter.values()) {
            switch (field) {
                case ID -> member.put(field.jsonFieldName(), AttributeValue.fromS(memberRecord.id()
                                                                                              .toString()));
                case FIRST_NAME -> member.put(field.jsonFieldName(), AttributeValue.fromS(memberRecord.member()
                                                                                                      .getFirstName()));
                case MIDDLE_NAME -> ofNullable(memberRecord.member()
                                                           .getMiddleName()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.fromS(s)));
                case LAST_NAME -> member.put(field.jsonFieldName(), AttributeValue.fromS(memberRecord.member()
                                                                                                     .getLastName()));
                case SUFFIX -> ofNullable(memberRecord.member()
                                                      .getSuffix()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.fromS(s.value())));
                case BIRTHDAY -> member.put(field.jsonFieldName(), AttributeValue.fromS(memberRecord.member()
                                                                                                    .getBirthdayString()));
                case DEATHDAY -> ofNullable(memberRecord.member()
                                                        .getDeathdayString()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.fromS(s)));
                case EMAIL -> ofNullable(memberRecord.member()
                                                     .getEmail()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.fromS(s)));
                case PHONES -> ofNullable(memberRecord.member()
                                                      .getPhonesDdbMap()).ifPresent(m -> member.put(field.jsonFieldName(), AttributeValue.fromM(m)));
                case ADDRESS -> ofNullable(memberRecord.member()
                                                       .getAddress()).ifPresent(ss -> member.put(field.jsonFieldName(), AttributeValue.fromSs(ss)));
                case FAMILY_ID -> member.put(field.jsonFieldName(), AttributeValue.fromS(familyId));
                default -> throw new IllegalStateException("Unhandled Member Parameter: `%s`".formatted(field.jsonFieldName()));
            }
        }

        return member;
    }

    void execute ();
}
