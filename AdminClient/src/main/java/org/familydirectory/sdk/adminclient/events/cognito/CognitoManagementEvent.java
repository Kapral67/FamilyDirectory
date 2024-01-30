package org.familydirectory.sdk.adminclient.events.cognito;

import io.leego.banana.Ansi;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.enums.cognito.CognitoManagementOptions;
import org.familydirectory.sdk.adminclient.events.delete.DeleteEvent;
import org.familydirectory.sdk.adminclient.events.model.EventHelper;
import org.familydirectory.sdk.adminclient.utility.Logger;
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
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public final
class CognitoManagementEvent implements EventHelper {
    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final @NotNull CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.create();
    private final @NotNull Scanner scanner;
    private final @NotNull CognitoManagementOptions cognitoManagementOption;

    public
    CognitoManagementEvent (final @NotNull Scanner scanner, final @NotNull CognitoManagementOptions cognitoManagementOption) {
        super();
        this.scanner = requireNonNull(scanner);
        this.cognitoManagementOption = requireNonNull(cognitoManagementOption);
    }

    @Override
    @NotNull
    public
    DynamoDbClient getDynamoDbClient () {
        return this.dynamoDbClient;
    }

    @Override
    public
    void close () {
        EventHelper.super.close();
        this.cognitoClient.close();
    }

    @Override
    public
    void execute () {
        switch (this.cognitoManagementOption) {
            case DELETE_COGNITO_USER -> {
                if (MemberPicker.isCognitoEmpty()) {
                    throw new IllegalStateException("No Cognito Users Exist to Delete");
                }
                final MemberRecord memberRecord = this.getExistingMember("Please Select Existing Cognito User to Delete:");
                this.deleteCognitoAccountAndNotify(memberRecord);
                MemberPicker.addEntry(memberRecord);
            }
            case ELEVATE_COGNITO_USER -> {
                if (MemberPicker.isCognitoEmpty()) {
                    throw new IllegalStateException("No Cognito Users Exist to Elevate");
                }
                final MemberRecord memberRecord = this.getExistingMember("Please Select Existing Cognito User to Elevate:");
                this.elevateCognitoAccount(memberRecord);
            }
            default -> throw new IllegalStateException("Unhandled CognitoManagementOption: %s".formatted(this.cognitoManagementOption.name()));
        }
    }

    private
    void deleteCognitoAccountAndNotify (final @NotNull MemberRecord memberRecord) {
        final String cognitoSub = MemberPicker.getCognitoSub(memberRecord);
        this.dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                                                        .tableName(DdbTable.COGNITO.name())
                                                        .key(singletonMap(CognitoTableParameter.ID.jsonFieldName(), AttributeValue.fromS(cognitoSub)))
                                                        .build());
        final String userPoolId = this.getUserPoolId();
        final ListUsersRequest listUsersRequest = ListUsersRequest.builder()
                                                                  .filter("sub = \"%s\"".formatted(cognitoSub))
                                                                  .limit(1)
                                                                  .userPoolId(userPoolId)
                                                                  .build();
        final UserType cognitoUser = ofNullable(this.cognitoClient.listUsers(listUsersRequest)).map(ListUsersResponse::users)
                                                                                               .filter(Predicate.not(List::isEmpty))
                                                                                               .map(List::getFirst)
                                                                                               .orElseThrow();
        final String cognitoUserName = Optional.of(cognitoUser)
                                               .map(UserType::username)
                                               .filter(Predicate.not(String::isBlank))
                                               .orElseThrow();
        this.cognitoClient.adminDeleteUser(AdminDeleteUserRequest.builder()
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
        DeleteEvent.sendDeletionNoticeEmail(singletonList(cognitoEmail));
    }

    @NotNull
    private
    String getUserPoolId () {
        return ofNullable(this.cognitoClient.listUserPools(ListUserPoolsRequest.builder()
                                                                               .maxResults(1)
                                                                               .build())
                                            .userPools()).filter(Predicate.not(List::isEmpty))
                                                         .map(l -> l.getFirst()
                                                                    .id())
                                                         .orElseThrow();
    }

    private
    void elevateCognitoAccount (final @NotNull MemberRecord memberRecord) {
        final String cognitoSub = MemberPicker.getCognitoSub(memberRecord);
        final boolean cognitoUserIsAdmin = ofNullable(requireNonNull(this.getDdbItem(cognitoSub, DdbTable.COGNITO)).get(CognitoTableParameter.IS_ADMIN.jsonFieldName())).map(AttributeValue::bool)
                                                                                                                                                                        .orElse(false);
        if (!cognitoUserIsAdmin) {
            this.dynamoDbClient.putItem(PutItemRequest.builder()
                                                      .tableName(DdbTable.COGNITO.name())
                                                      .item(Map.of(CognitoTableParameter.ID.jsonFieldName(), AttributeValue.fromS(cognitoSub), CognitoTableParameter.MEMBER.jsonFieldName(),
                                                                   AttributeValue.fromS(memberRecord.id()
                                                                                                    .toString()), CognitoTableParameter.IS_ADMIN.jsonFieldName(), AttributeValue.fromBool(true)))
                                                      .build());
            Logger.customLine("SUCCESS: COGNITO USER NOW ADMIN", Ansi.BOLD, Ansi.GREEN);
        } else {
            Logger.customLine("WARN: COGNITO USER WAS ALREADY ADMIN", Ansi.BOLD, Ansi.YELLOW);
        }

        System.out.println();
    }

    @Override
    @NotNull
    public
    Scanner scanner () {
        return this.scanner;
    }

    @Override
    @NotNull
    public
    MemberRecord getExistingMember (final @NotNull String message) {
        return this.getExistingMember(message, MemberPicker.getCognitoMemberRecords());
    }
}
