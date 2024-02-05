package org.familydirectory.sdk.adminclient.events.cognito;

import io.leego.banana.Ansi;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.enums.cognito.CognitoManagementOptions;
import org.familydirectory.sdk.adminclient.events.model.EventHelper;
import org.familydirectory.sdk.adminclient.utility.Logger;
import org.familydirectory.sdk.adminclient.utility.pickers.CognitoUserPicker;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public final
class CognitoManagementEvent implements EventHelper {
    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final @NotNull CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.create();
    private final @NotNull Scanner scanner;
    private final @NotNull CognitoManagementOptions cognitoManagementOption;
    private final @NotNull CognitoUserPicker cognitoUserPicker;

    public
    CognitoManagementEvent (final @NotNull Scanner scanner, final @NotNull CognitoManagementOptions cognitoManagementOption, final @NotNull CognitoUserPicker cognitoUserPicker) {
        super();
        this.scanner = requireNonNull(scanner);
        this.cognitoManagementOption = requireNonNull(cognitoManagementOption);
        this.cognitoUserPicker = cognitoUserPicker;
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
    void run () {
        switch (this.cognitoManagementOption) {
            case DELETE_COGNITO_USER -> {
                if (this.cognitoUserPicker.isEmpty()) {
                    throw new IllegalStateException("No Cognito Users Exist to Delete");
                }
                final MemberRecord memberRecord = this.getExistingMember("Please Select Existing Cognito User to Delete:");
                this.deleteCognitoAccountAndNotify(this.cognitoClient, this.cognitoUserPicker.getCognitoSub(memberRecord));
                this.cognitoUserPicker.removeEntry(memberRecord);
            }
            case DEMOTE_COGNITO_USER -> {
                if (this.cognitoUserPicker.isEmpty()) {
                    throw new IllegalStateException("No Cognito Users Exist to Demote");
                }
                final MemberRecord memberRecord = this.getExistingMember("Please Select Existing Cognito User to Demote:");
                this.demoteCognitoAccount(memberRecord);
            }
            case ELEVATE_COGNITO_USER -> {
                if (this.cognitoUserPicker.isEmpty()) {
                    throw new IllegalStateException("No Cognito Users Exist to Elevate");
                }
                final MemberRecord memberRecord = this.getExistingMember("Please Select Existing Cognito User to Elevate:");
                this.elevateCognitoAccount(memberRecord);
            }
            default -> throw new IllegalStateException("Unhandled CognitoManagementOption: %s".formatted(this.cognitoManagementOption.name()));
        }
    }

    private
    void elevateCognitoAccount (final @NotNull MemberRecord memberRecord) {
        final String cognitoSub = this.cognitoUserPicker.getCognitoSub(memberRecord);
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

    private
    void demoteCognitoAccount (final @NotNull MemberRecord memberRecord) {
        final String cognitoSub = this.cognitoUserPicker.getCognitoSub(memberRecord);
        final boolean cognitoUserIsAdmin = ofNullable(requireNonNull(this.getDdbItem(cognitoSub, DdbTable.COGNITO)).get(CognitoTableParameter.IS_ADMIN.jsonFieldName())).map(AttributeValue::bool)
                                                                                                                                                                        .orElse(false);
        if (cognitoUserIsAdmin) {
            this.dynamoDbClient.putItem(PutItemRequest.builder()
                                                      .tableName(DdbTable.COGNITO.name())
                                                      .item(Map.of(CognitoTableParameter.ID.jsonFieldName(), AttributeValue.fromS(cognitoSub), CognitoTableParameter.MEMBER.jsonFieldName(),
                                                                   AttributeValue.fromS(memberRecord.id()
                                                                                                    .toString()), CognitoTableParameter.IS_ADMIN.jsonFieldName(), AttributeValue.fromBool(false)))
                                                      .build());
            Logger.customLine("SUCCESS: COGNITO USER NO LONGER ADMIN", Ansi.BOLD, Ansi.GREEN);
        } else {
            Logger.customLine("WARN: NO ACTION TAKEN; COGNITO USER WAS NOT ADMIN", Ansi.BOLD, Ansi.YELLOW);
        }

        System.out.println();
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

    @Override
    @NotNull
    public
    Scanner scanner () {
        return this.scanner;
    }

    @Override
    public @NotNull
    List<MemberRecord> getPickerEntries () {
        return this.cognitoUserPicker.getEntries();
    }
}
