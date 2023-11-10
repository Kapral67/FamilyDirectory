package org.familydirectory.sdk.adminclient.events.delete;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.sdk.adminclient.events.model.EventHelper;
import org.familydirectory.sdk.adminclient.events.model.MemberRecord;
import org.familydirectory.sdk.adminclient.utility.MemberPicker;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.Update;
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
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public final
class DeleteEvent implements EventHelper {
    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final @NotNull CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.create();
    private final @NotNull Scanner scanner;

    public
    DeleteEvent (final @NotNull Scanner scanner) {
        super();
        this.scanner = requireNonNull(scanner);
    }

    @Override
    public @NotNull
    Scanner scanner () {
        return this.scanner;
    }

    @Override
    public
    void execute () {
        if (MemberPicker.getEntries()
                        .isEmpty())
        {
            throw new IllegalStateException("No Members Exist to Update");
        }
        final MemberRecord memberRecord = this.getExistingMember("Please Select Existing Member to DELETE:");

        final List<TransactWriteItem> transactionItems;
        if (memberRecord.id()
                        .equals(memberRecord.familyId()))
        {
            // NATIVE
            final Map<String, AttributeValue> familyMap = requireNonNull(this.getDdbItem(memberRecord.familyId()
                                                                                                     .toString(), DdbTable.FAMILY));
            ofNullable(familyMap.get(FamilyTableParameter.SPOUSE.jsonFieldName())).map(AttributeValue::s)
                                                                                  .filter(Predicate.not(String::isBlank))
                                                                                  .ifPresent(spouse -> {
                                                                                      throw new IllegalStateException(
                                                                                              "Member Cannot Be Deleted Because Member's Family has a SPOUSE: %s".formatted(spouse));
                                                                                  });
            ofNullable(familyMap.get(FamilyTableParameter.DESCENDANTS.jsonFieldName())).map(AttributeValue::ss)
                                                                                       .filter(Predicate.not(List::isEmpty))
                                                                                       .ifPresent(descendants -> {
                                                                                           throw new IllegalStateException(
                                                                                                   "Member Cannot Be Deleted Because Member's Family has DESCENDANTS: %s".formatted(descendants));
                                                                                       });
            final Delete deleteFamily = Delete.builder()
                                              .tableName(DdbTable.FAMILY.name())
                                              .key(singletonMap(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(memberRecord.familyId()
                                                                                                                                          .toString())))
                                              .build();
            final Delete deleteMember = Delete.builder()
                                              .tableName(DdbTable.MEMBER.name())
                                              .key(singletonMap(MemberTableParameter.ID.jsonFieldName(), AttributeValue.fromS(memberRecord.id()
                                                                                                                                          .toString())))
                                              .build();
            transactionItems = List.of(TransactWriteItem.builder()
                                                        .delete(deleteFamily)
                                                        .build(), TransactWriteItem.builder()
                                                                                   .delete(deleteMember)
                                                                                   .build());
        } else {
            // NATURALIZED
            final Update update = Update.builder()
                                        .tableName(DdbTable.FAMILY.name())
                                        .key(singletonMap(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(memberRecord.familyId()
                                                                                                                                    .toString())))
                                        .updateExpression("REMOVE %s".formatted(FamilyTableParameter.SPOUSE.jsonFieldName()))
                                        .build();
            final Delete delete = Delete.builder()
                                        .tableName(DdbTable.MEMBER.name())
                                        .key(singletonMap(MemberTableParameter.ID.jsonFieldName(), AttributeValue.fromS(memberRecord.id()
                                                                                                                                    .toString())))
                                        .build();
            transactionItems = List.of(TransactWriteItem.builder()
                                                        .update(update)
                                                        .build(), TransactWriteItem.builder()
                                                                                   .delete(delete)
                                                                                   .build());
        }

        this.dynamoDbClient.transactWriteItems(TransactWriteItemsRequest.builder()
                                                                        .transactItems(transactionItems)
                                                                        .build());
        MemberPicker.removeEntry(memberRecord);
        this.deleteCognitoAccountAndNotify(memberRecord.id()
                                                       .toString());
    }

    private
    void deleteCognitoAccountAndNotify (final @NotNull String memberId) {
        final String userPoolId = this.getUserPoolId();
        final QueryRequest cognitoMemberQueryRequest = QueryRequest.builder()
                                                                   .tableName(DdbTable.COGNITO.name())
                                                                   .indexName(requireNonNull(CognitoTableParameter.MEMBER.gsiProps()).getIndexName())
                                                                   .keyConditionExpression("#memberId = :memberId")
                                                                   .expressionAttributeNames(singletonMap("#memberId", CognitoTableParameter.MEMBER.gsiProps()
                                                                                                                                                   .getPartitionKey()
                                                                                                                                                   .getName()))
                                                                   .expressionAttributeValues(singletonMap(":memberId", AttributeValue.fromS(memberId)))
                                                                   .limit(1)
                                                                   .build();
        final QueryResponse cognitoMemberQueryResponse = this.dynamoDbClient.query(cognitoMemberQueryRequest);
        if (!cognitoMemberQueryResponse.items()
                                       .isEmpty())
        {
            final String ddbMemberCognitoSub = ofNullable(cognitoMemberQueryResponse.items()
                                                                                    .iterator()
                                                                                    .next()
                                                                                    .get(CognitoTableParameter.ID.jsonFieldName())).map(AttributeValue::s)
                                                                                                                                   .filter(Predicate.not(String::isBlank))
                                                                                                                                   .orElseThrow();

            this.dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                                                            .tableName(DdbTable.COGNITO.name())
                                                            .key(singletonMap(CognitoTableParameter.ID.jsonFieldName(), AttributeValue.fromS(ddbMemberCognitoSub)))
                                                            .build());

            final ListUsersRequest listUsersRequest = ListUsersRequest.builder()
                                                                      .filter("sub = \"%s\"".formatted(ddbMemberCognitoSub))
                                                                      .limit(1)
                                                                      .userPoolId(userPoolId)
                                                                      .build();
            final UserType ddbMemberCognitoUser = ofNullable(this.cognitoClient.listUsers(listUsersRequest)).filter(ListUsersResponse::hasUsers)
                                                                                                            .map(ListUsersResponse::users)
                                                                                                            .map(list -> list.iterator()
                                                                                                                             .next())
                                                                                                            .orElseThrow();
            final String ddbMemberCognitoUsername = Optional.of(ddbMemberCognitoUser)
                                                            .map(UserType::username)
                                                            .filter(Predicate.not(String::isBlank))
                                                            .orElseThrow();
            this.cognitoClient.adminDeleteUser(AdminDeleteUserRequest.builder()
                                                                     .userPoolId(userPoolId)
                                                                     .username(ddbMemberCognitoUsername)
                                                                     .build());

            final String ddbMemberCognitoEmail = Optional.of(ddbMemberCognitoUser)
                                                         .filter(UserType::hasAttributes)
                                                         .map(UserType::attributes)
                                                         .orElseThrow()
                                                         .stream()
                                                         .filter(attr -> attr.name()
                                                                             .equalsIgnoreCase("email"))
                                                         .findFirst()
                                                         .map(AttributeType::value)
                                                         .filter(s -> s.contains("@"))
                                                         .orElseThrow();
            sendDeletionNoticeEmail(singletonList(ddbMemberCognitoEmail));
        }
    }

    private static
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
        try (final SesV2Client sesClient = SesV2Client.create()) {
            sesClient.sendEmail(SendEmailRequest.builder()
                                                .destination(Destination.builder()
                                                                        .toAddresses(addresses)
                                                                        .build())
                                                .content(EmailContent.builder()
                                                                     .simple(message)
                                                                     .build())
                                                .fromEmailAddress("no-reply@%s".formatted(getenv("ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME")))
                                                .build());
        }
    }

    @NotNull
    private
    String getUserPoolId () {
        return ofNullable(this.cognitoClient.listUserPools(ListUserPoolsRequest.builder()
                                                                               .maxResults(1)
                                                                               .build())
                                            .userPools()).filter(Predicate.not(List::isEmpty))
                                                         .map(l -> l.iterator()
                                                                    .next()
                                                                    .id())
                                                         .orElseThrow();
    }

    @Override
    public @NotNull
    DynamoDbClient getDynamoDbClient () {
        return this.dynamoDbClient;
    }

    @Override
    public
    void close () {
        EventHelper.super.close();
        this.cognitoClient.close();
    }
}
