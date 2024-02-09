package org.familydirectory.sdk.adminclient.events.model;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialogResultValidator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.PhoneType;
import org.familydirectory.assets.ddb.enums.SuffixType;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.models.DdbTableParameter;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.sdk.adminclient.utility.PickerDialogRefreshUtility;
import org.familydirectory.sdk.adminclient.utility.SdkClientProvider;
import org.familydirectory.sdk.adminclient.utility.dialogs.SkippableListSelectDialog;
import org.familydirectory.sdk.adminclient.utility.dialogs.SkippableTextInputDialog;
import org.familydirectory.sdk.adminclient.utility.pickers.model.PickerModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import static java.lang.System.getenv;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public
interface MemberEventHelper extends EventHelper {
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

    static
    void deleteCognitoAccountAndNotify (final @NotNull String sub) {
        final SdkClientProvider sdkClientProvider = SdkClientProvider.getSdkClientProvider();
        sdkClientProvider.getSdkClient(DynamoDbClient.class)
                         .deleteItem(DeleteItemRequest.builder()
                                                      .tableName(DdbTable.COGNITO.name())
                                                      .key(singletonMap(CognitoTableParameter.ID.jsonFieldName(), AttributeValue.fromS(requireNonNull(sub))))
                                                      .build());
        final String userPoolId = getUserPoolId();
        final UserType cognitoUser = getCognitoUserType(userPoolId, sub);
        final String cognitoUserName = Optional.of(cognitoUser)
                                               .map(UserType::username)
                                               .filter(Predicate.not(String::isBlank))
                                               .orElseThrow();
        sdkClientProvider.getSdkClient(CognitoIdentityProviderClient.class)
                         .adminDeleteUser(AdminDeleteUserRequest.builder()
                                                                .userPoolId(userPoolId)
                                                                .username(cognitoUserName)
                                                                .build());
        final String cognitoEmail = Optional.of(cognitoUser)
                                            .map(UserType::attributes)
                                            .filter(Predicate.not(List::isEmpty))
                                            .orElseThrow()
                                            .stream()
                                            .filter(attr -> attr.name()
                                                                .equalsIgnoreCase("email"))
                                            .findFirst()
                                            .map(AttributeType::value)
                                            .filter(s -> s.contains("@"))
                                            .orElseThrow();
        sendDeletionNoticeEmail(singletonList(cognitoEmail));
    }

    static
    void sendDeletionNoticeEmail (final @NotNull List<String> addresses) {
        final Message message = Message.builder()
                                       .subject(Content.builder()
                                                       .data("Notice of Account Deletion")
                                                       .build())
                                       .body(Body.builder()
                                                 .text(Content.builder()
                                                              .data("Your account has been irreversibly deleted.")
                                                              .build())
                                                 .build())
                                       .build();
        SdkClientProvider.getSdkClientProvider()
                         .getSdkClient(SesV2Client.class)
                         .sendEmail(SendEmailRequest.builder()
                                                    .destination(Destination.builder()
                                                                            .toAddresses(requireNonNull(addresses))
                                                                            .build())
                                                    .content(EmailContent.builder()
                                                                         .simple(message)
                                                                         .build())
                                                    .fromEmailAddress("no-reply@%s".formatted(requireNonNull(getenv("ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME"))))
                                                    .build());
    }

    @NotNull
    static
    UserType getCognitoUserType (final @NotNull String userPoolId, final @NotNull String sub) {
        final ListUsersRequest listUsersRequest = ListUsersRequest.builder()
                                                                  .filter("sub = \"%s\"".formatted(requireNonNull(sub)))
                                                                  .limit(1)
                                                                  .userPoolId(requireNonNull(userPoolId))
                                                                  .build();
        return ofNullable(SdkClientProvider.getSdkClientProvider()
                                           .getSdkClient(CognitoIdentityProviderClient.class)
                                           .listUsers(listUsersRequest)).map(ListUsersResponse::users)
                                                                        .filter(Predicate.not(List::isEmpty))
                                                                        .map(List::getFirst)
                                                                        .orElseThrow();
    }

    @NotNull
    static
    String getUserPoolId () {
        return ofNullable(SdkClientProvider.getSdkClientProvider()
                                           .getSdkClient(CognitoIdentityProviderClient.class)
                                           .listUserPools(ListUserPoolsRequest.builder()
                                                                              .maxResults(1)
                                                                              .build())
                                           .userPools()).filter(Predicate.not(List::isEmpty))
                                                        .map(l -> l.getFirst()
                                                                   .id())
                                                        .orElseThrow();
    }

    static
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
            final QueryResponse emailResponse = SdkClientProvider.getSdkClientProvider()
                                                                 .getSdkClient(DynamoDbClient.class)
                                                                 .query(emailRequest);
            if (!emailResponse.items()
                              .isEmpty())
            {
                throw new IllegalStateException("EMAIL %s already claimed by Existing Member".formatted(memberEmail));
            }
        }
    }

    @Nullable
    static
    Map<String, AttributeValue> getDdbItem (final @NotNull String primaryKey, final @NotNull DdbTable ddbTable) {
        final GetItemRequest request = GetItemRequest.builder()
                                                     .tableName(ddbTable.name())
                                                     .key(singletonMap(DdbTableParameter.PK.getName(), AttributeValue.fromS(primaryKey)))
                                                     .build();
        final GetItemResponse response = SdkClientProvider.getSdkClientProvider()
                                                          .getSdkClient(DynamoDbClient.class)
                                                          .getItem(request);
        return (response.item()
                        .isEmpty())
                ? null
                : response.item();
    }

    @NotNull
    default
    MemberRecord buildMemberRecord (final @NotNull UUID memberId, final @NotNull UUID familyId) {
        final WindowBasedTextGUI gui = this.getGui();
        final Member.Builder memberBuilder = Member.builder();
        boolean breakLoop = false;
        LocalDate birthday = null;
        for (final MemberTableParameter param : MemberTableParameter.values()) {
            if (breakLoop) {
                break;
            }
            final LocalDate finalBirthday = birthday;
            switch (param) {
                case FIRST_NAME -> {
                    final String desc = "Must match pattern: ^A-Za-z\\-_'$%nMust NOT match pattern: ^['_-]+[A-Za-z\\-'_]*$%n_ & - chars result in the immediate succeeding char being " +
                                        "capitalized%n_ chars are removed, useful for names like McDonald (input: mc_donald)%n%n[Required] Please Enter %s:".formatted(param.jsonFieldName());
                    final TextInputDialogResultValidator validator = (content) -> {
                        try {
                            Member.builder()
                                  .firstName(content);
                            return null;
                        } catch (final RuntimeException e) {
                            return e.getMessage();
                        }
                    };
                    final SkippableTextInputDialog dialog = new SkippableTextInputDialog(param.jsonFieldName(), desc, false, validator);
                    memberBuilder.firstName(requireNonNull(dialog.showDialog(gui)));
                }
                case MIDDLE_NAME -> {
                    final String desc = "Must match pattern: ^A-Za-z\\-_'$%nMust NOT match pattern: ^['_-]+[A-Za-z\\-'_]*$%n_ & - chars result in the immediate succeeding char being " +
                                        "capitalized%n_ chars are removed, useful for names like McDonald (input: mc_donald)%n%n[Optional] Please Enter %s:".formatted(param.jsonFieldName());
                    final TextInputDialogResultValidator validator = (content) -> {
                        try {
                            Member.builder()
                                  .middleName(content);
                            return null;
                        } catch (final RuntimeException e) {
                            return e.getMessage();
                        }
                    };
                    final SkippableTextInputDialog dialog = new SkippableTextInputDialog(param.jsonFieldName(), desc, true, validator);
                    memberBuilder.middleName(dialog.showDialog(gui));
                }
                case LAST_NAME -> {
                    final String desc = "Must match pattern: ^A-Za-z\\-_'$%nMust NOT match pattern: ^['_-]+[A-Za-z\\-'_]*$%n_ & - chars result in the immediate succeeding char being " +
                                        "capitalized%n_ chars are removed, useful for names like McDonald (input: mc_donald)%n%n[Required] Please Enter %s:".formatted(param.jsonFieldName());
                    final TextInputDialogResultValidator validator = (content) -> {
                        try {
                            Member.builder()
                                  .lastName(content);
                            return null;
                        } catch (final RuntimeException e) {
                            return e.getMessage();
                        }
                    };
                    final SkippableTextInputDialog dialog = new SkippableTextInputDialog(param.jsonFieldName(), desc, false, validator);
                    memberBuilder.lastName(requireNonNull(dialog.showDialog(gui)));
                }
                case SUFFIX -> {
                    final SkippableListSelectDialog<SuffixType> dialog = new SkippableListSelectDialog<>(param.jsonFieldName(), null, true, List.of(SuffixType.values()));
                    memberBuilder.suffix(dialog.showDialog(gui));
                }
                case BIRTHDAY -> {
                    final String desc = "Must be formatted like yyyy-MM-dd (e.g. 1970-12-31 -> Dec. 31, 1970)%n%n[Required] Please Enter %s:".formatted(param.jsonFieldName());
                    final TextInputDialogResultValidator validator = (content) -> {
                        try {
                            Member.builder()
                                  .birthday(Member.convertStringToDate(content));
                            return null;
                        } catch (final RuntimeException e) {
                            return e.getMessage();
                        }
                    };
                    final SkippableTextInputDialog dialog = new SkippableTextInputDialog(param.jsonFieldName(), desc, false, validator);
                    birthday = Member.convertStringToDate(requireNonNull(dialog.showDialog(gui)));
                    memberBuilder.birthday(birthday);
                }
                case DEATHDAY -> {
                    final String desc = "Must be formatted like yyyy-MM-dd (e.g. 1970-12-31 -> Dec. 31, 1970)%n%n[Optional] Please Enter %s:".formatted(param.jsonFieldName());
                    final TextInputDialogResultValidator validator = (content) -> {
                        if (isNull(content)) {
                            return null;
                        }
                        try {
                            Member.builder()
                                  .birthday(requireNonNull(finalBirthday))
                                  .deathday(Member.convertStringToDate(content));
                            return null;
                        } catch (final RuntimeException e) {
                            return e.getMessage();
                        }
                    };
                    final SkippableTextInputDialog dialog = new SkippableTextInputDialog(param.jsonFieldName(), desc, true, validator);
                    final String deathdayString = dialog.showDialog(gui);
                    if (nonNull(deathdayString)) {
                        final LocalDate deathday = Member.convertStringToDate(deathdayString);
                        memberBuilder.deathday(deathday);
                        breakLoop = true;
                    }
                }
                case EMAIL -> {
                    final String desc = "[Optional] Please Enter %s:".formatted(param.jsonFieldName());
                    final TextInputDialogResultValidator validator = (content) -> {
                        try {
                            Member.builder()
                                  .email(content);
                            return null;
                        } catch (final RuntimeException e) {
                            return e.getMessage();
                        }
                    };
                    final SkippableTextInputDialog dialog = new SkippableTextInputDialog(param.jsonFieldName(), desc, true, validator);
                    memberBuilder.email(dialog.showDialog(gui));
                }
                case PHONES -> {
                    final MessageDialog msgDialog = new MessageDialogBuilder().setTitle(param.jsonFieldName())
                                                                              .setText("Would You Like to Add %s to this Member?".formatted(param.jsonFieldName()))
                                                                              .addButton(MessageDialogButton.Yes)
                                                                              .addButton(MessageDialogButton.No)
                                                                              .build();
                    if (msgDialog.showDialog(gui)
                                 .equals(MessageDialogButton.Yes))
                    {
                        final String descPrefix = "For US numbers: '+' & Country Code are Optional%nFor Int'l Numbers: '+' & Country Code are Required".formatted();
                        final TextInputDialogResultValidator validator = (content) -> {
                            try {
                                DdbUtils.normalizePhoneNumber(content);
                                return null;
                            } catch (final RuntimeException e) {
                                return e.getMessage();
                            }
                        };
                        final Map<PhoneType, String> phones = new HashMap<>();
                        for (final PhoneType phoneType : PhoneType.values()) {
                            final String desc = "%s%n%n[Optional] Please Enter %s %s:".formatted(descPrefix, phoneType.name(), param.jsonFieldName());
                            final SkippableTextInputDialog dialog = new SkippableTextInputDialog(phoneType.name(), desc, true, validator);
                            final String content = dialog.showDialog(gui);
                            if (nonNull(content)) {
                                phones.put(phoneType, DdbUtils.normalizePhoneNumber(content));
                            }
                        }
                        memberBuilder.phones(phones);
                    }
                }
                case ADDRESS -> {
                    final List<String> addressLines = new ArrayList<>();
                    for (int i = 1; i <= Member.REQ_NON_NULL_ADDRESS_SIZE && (i == 1 || !addressLines.isEmpty()); ++i) {
                        final String desc = "[%s] Please Enter %s Line %d:".formatted((i == 1)
                                                                                              ? "Optional"
                                                                                              : "Required", param.jsonFieldName(), i);
                        final SkippableTextInputDialog dialog = new SkippableTextInputDialog(param.jsonFieldName(), desc, i == 1, null);
                        final String addressLine = dialog.showDialog(gui);
                        if (nonNull(addressLine)) {
                            addressLines.add(addressLine);
                        }
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
    WindowBasedTextGUI getGui ();

    @NotNull
    default
    MemberRecord getExistingMember (final @NotNull String title, final @Nullable String description, final @NotNull String waitText)
    {
        final WindowBasedTextGUI gui = this.getGui();
        final PickerDialogRefreshUtility pickerDialogRefreshUtility = new PickerDialogRefreshUtility(this.getPicker(), requireNonNull(title), description, requireNonNull(waitText));
        return pickerDialogRefreshUtility.showDialog(gui);
    }

    @NotNull
    PickerModel getPicker ();
}
