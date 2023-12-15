package org.familydirectory.sdk.adminclient.events.model;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import io.leego.banana.Ansi;
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
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.assets.lambda.function.helper.LambdaFunctionHelper;
import org.familydirectory.sdk.adminclient.utility.Logger;
import org.familydirectory.sdk.adminclient.utility.MemberPicker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import static java.util.Collections.singletonMap;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public
interface EventHelper extends LambdaFunctionHelper, Executable {
    String ROOT_ID = DdbUtils.ROOT_MEMBER_ID;

    @NotNull
    static
    Map<String, AttributeValue> buildMember (final @NotNull MemberRecord memberRecord) {
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
                case FAMILY_ID -> member.put(field.jsonFieldName(), AttributeValue.fromS(memberRecord.familyId()
                                                                                                     .toString()));
                default -> throw new IllegalStateException("Unhandled Member Parameter: `%s`".formatted(field.jsonFieldName()));
            }
        }

        return member;
    }

    @Override
    @NotNull
    default
    LambdaLogger getLogger () {
        return new Logger();
    }

    @Override
    @NotNull
    @Deprecated
    default
    String getPdfS3Key () {
        throw new UnsupportedOperationException("Admin Client Does Not Implement GET Requests");
    }

    @NotNull
    default
    MemberRecord buildMemberRecord (final @NotNull UUID memberId, final @NotNull UUID familyId) {
        final Member.Builder memberBuilder = Member.builder();
        Logger.info("Please Standby to Build This Member...");
        Logger.info("[Required] attributes will error if skipped");
        Logger.info("[Optional] attributes can be skipped by pressing Enter");
        System.out.println();
        boolean breakLoop = false;
        for (final MemberTableParameter param : MemberTableParameter.values()) {
            if (breakLoop) {
                break;
            }
            switch (param) {
                case FIRST_NAME -> {
                    Logger.warn("%s may contain A-Z a-z - _ or '".formatted(param.jsonFieldName()));
                    Logger.info("_ & - characters result in the immediate succeeding character being capitalized");
                    Logger.info("_ characters are removed, useful for names like McDonald (input: mc_donald)");
                    Logger.customLine("[Required] Please Enter %s:".formatted(param.jsonFieldName()), Ansi.BOLD, Ansi.BLUE);
                    memberBuilder.firstName(this.scanner()
                                                .nextLine());
                    System.out.println();
                }
                case MIDDLE_NAME -> {
                    Logger.warn("%s may contain A-Z a-z - _ or '".formatted(param.jsonFieldName()));
                    Logger.info("_ & - characters result in the immediate succeeding character being capitalized");
                    Logger.info("_ characters are removed, useful for names like McDonald (input: mc_donald)");
                    Logger.customLine("[Optional] Please Enter %s:".formatted(param.jsonFieldName()), Ansi.BOLD, Ansi.BLUE);
                    final String middleName = this.scanner()
                                                  .nextLine()
                                                  .trim();
                    if (!middleName.isEmpty()) {
                        memberBuilder.middleName(middleName);
                        System.out.println();
                    }
                }
                case LAST_NAME -> {
                    Logger.warn("%s may contain A-Z a-z - _ or '".formatted(param.jsonFieldName()));
                    Logger.info("_ & - characters result in the immediate succeeding character being capitalized");
                    Logger.info("_ characters are removed, useful for names like McDonald (input: mc_donald)");
                    Logger.customLine("[Required] Please Enter %s:%n".formatted(param.jsonFieldName()), Ansi.BOLD, Ansi.BLUE);
                    memberBuilder.lastName(this.scanner()
                                               .nextLine());
                    System.out.println();
                }
                case SUFFIX -> {
                    int ordinal = -1;
                    final SuffixType suffix;
                    while (ordinal < 0 || ordinal >= SuffixType.values().length) {
                        Logger.customLine("[Optional] Please Choose A Suffix:", Ansi.BOLD, Ansi.BLUE);
                        for (final SuffixType sfx : SuffixType.values()) {
                            Logger.customLine("%d) %s".formatted(sfx.ordinal(), sfx.name()));
                        }
                        final String token = this.scanner()
                                                 .nextLine()
                                                 .trim();
                        try {
                            ordinal = Integer.parseInt(token);
                        } catch (final NumberFormatException ignored) {
                            if (!token.isEmpty()) {
                                ordinal = -1;
                            } else {
                                break;
                            }
                        }
                        if (ordinal < 0 || ordinal >= SuffixType.values().length) {
                            Logger.error("Invalid Suffix");
                            System.out.println();
                        } else {
                            memberBuilder.suffix(SuffixType.values()[ordinal]);
                            System.out.println();
                            break;
                        }
                    }
                }
                case BIRTHDAY -> {
                    Logger.warn("%s must be formatted like yyyy-MM-dd (e.g. 1970-12-31 -> Dec. 31, 1970)".formatted(param.jsonFieldName()));
                    Logger.customLine("[Required] Please Enter %s:".formatted(param.jsonFieldName()), Ansi.BOLD, Ansi.BLUE);
                    memberBuilder.birthday(Member.convertStringToDate(this.scanner()
                                                                          .nextLine()
                                                                          .trim()));
                    System.out.println();
                }
                case DEATHDAY -> {
                    Logger.warn("%s must be formatted like yyyy-MM-dd (e.g. 1970-12-31 -> Dec. 31, 1970)".formatted(param.jsonFieldName()));
                    Logger.customLine("[Optional] Please Enter %s:".formatted(param.jsonFieldName()), Ansi.BOLD, Ansi.BLUE);
                    final String deathDay = this.scanner()
                                                .nextLine()
                                                .trim();
                    if (!deathDay.isEmpty()) {
                        memberBuilder.deathday(Member.convertStringToDate(deathDay));
                        System.out.println();
                        breakLoop = true;
                    }
                }
                case EMAIL -> {
                    Logger.customLine("[Optional] Please Enter %s:".formatted(param.jsonFieldName()), Ansi.BOLD, Ansi.BLUE);
                    final String email = this.scanner()
                                             .nextLine()
                                             .trim();
                    if (!email.isEmpty()) {
                        memberBuilder.email(email);
                        System.out.println();
                    }
                }
                case PHONES -> {
                    Logger.customLine("[Optional] Add %s to this Member? (y/N)".formatted(param.jsonFieldName()), Ansi.BOLD, Ansi.BLUE);
                    final String addPhone = this.scanner()
                                                .nextLine()
                                                .trim();
                    System.out.println();
                    if (addPhone.equalsIgnoreCase("y")) {
                        Logger.warn("For US numbers: Do Not Include + or Country Code");
                        Logger.warn("For International numbers: + and Country Code are required");
                        final Map<PhoneType, String> phones = new HashMap<>();
                        for (final PhoneType phoneType : PhoneType.values()) {
                            Logger.customLine("[Optional] Please Enter %s Phone Number:".formatted(phoneType.name()), Ansi.BOLD, Ansi.BLUE);
                            final String phone = this.scanner()
                                                     .nextLine()
                                                     .trim();
                            if (!phone.isEmpty()) {
                                phones.put(phoneType, phone);
                                System.out.println();
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
                        if (i == 1) {
                            Logger.customLine("[Optional] Please Enter %s line %d:".formatted(param.jsonFieldName(), i), Ansi.BOLD, Ansi.BLUE);
                        } else {
                            Logger.customLine("[Required] Please Enter %s line %d:".formatted(param.jsonFieldName(), i), Ansi.BOLD, Ansi.BLUE);
                        }
                        final String addressLineText = this.scanner()
                                                           .nextLine()
                                                           .trim();
                        if (addressLineText.isEmpty()) {
                            break;
                        }
                        System.out.println();
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
        return new MemberRecord(memberId, memberBuilder.build(), familyId);
    }

    @NotNull
    Scanner scanner ();

    @NotNull
    default
    MemberRecord getExistingMember (final @NotNull String message) {
        final List<MemberRecord> records = MemberPicker.getEntries();
        int index = -1;
        while (index < 0 || index >= records.size()) {
            Logger.customLine(requireNonNull(message), Ansi.BOLD, Ansi.BLUE);
            for (int i = 0; i < records.size(); ++i) {
                Logger.customLine("%d) %s".formatted(i, records.get(i)
                                                               .member()
                                                               .getFullName()));
            }
            final String token = this.scanner()
                                     .nextLine()
                                     .trim();
            try {
                index = Integer.parseInt(token);
            } catch (final NumberFormatException ignored) {
                index = -1;
            }
            if (index < 0 || index >= records.size()) {
                Logger.error("Invalid Member");
            }
            System.out.println();
        }
        return records.get(index);
    }

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
}
