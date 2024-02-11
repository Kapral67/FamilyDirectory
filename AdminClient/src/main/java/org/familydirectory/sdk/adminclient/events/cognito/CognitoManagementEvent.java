package org.familydirectory.sdk.adminclient.events.cognito;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.enums.cognito.CognitoManagementOptions;
import org.familydirectory.sdk.adminclient.events.model.MemberEventHelper;
import org.familydirectory.sdk.adminclient.utility.SdkClientProvider;
import org.familydirectory.sdk.adminclient.utility.pickers.CognitoUserPicker;
import org.familydirectory.sdk.adminclient.utility.pickers.model.PickerModel;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public final
class CognitoManagementEvent implements MemberEventHelper {
    private final @NotNull WindowBasedTextGUI gui;
    private final @NotNull CognitoManagementOptions cognitoManagementOption;
    private final @NotNull CognitoUserPicker cognitoUserPicker;

    public
    CognitoManagementEvent (final @NotNull WindowBasedTextGUI gui, final @NotNull CognitoManagementOptions cognitoManagementOption, final @NotNull CognitoUserPicker cognitoUserPicker) {
        super();
        this.gui = requireNonNull(gui);
        this.cognitoManagementOption = requireNonNull(cognitoManagementOption);
        this.cognitoUserPicker = cognitoUserPicker;
    }

    @Override
    public
    void run () {
        final String waitText = "Retrieving Cognito Users from AWS, Please Wait";
        switch (this.cognitoManagementOption) {
            case DELETE_COGNITO_USER -> {
                if (this.cognitoUserPicker.isEmpty()) {
                    throw new IllegalStateException("No Cognito Users Exist to Delete");
                }
                final MemberRecord memberRecord = this.getExistingMember(this.cognitoManagementOption.name(), "Please Select Existing Cognito User:", waitText);
                MemberEventHelper.deleteCognitoAccountAndNotify(this.cognitoUserPicker.getCognitoSub(memberRecord));
                this.cognitoUserPicker.removeEntry(memberRecord);
            }
            case DEMOTE_COGNITO_USER -> {
                if (this.cognitoUserPicker.isEmpty()) {
                    throw new IllegalStateException("No Cognito Users Exist to Demote");
                }
                final MemberRecord memberRecord = this.getExistingMember(this.cognitoManagementOption.name(), "Please Select Existing Cognito User:", waitText);
                this.alterCognitoAccountStatus(memberRecord, false);
            }
            case ELEVATE_COGNITO_USER -> {
                if (this.cognitoUserPicker.isEmpty()) {
                    throw new IllegalStateException("No Cognito Users Exist to Elevate");
                }
                final MemberRecord memberRecord = this.getExistingMember(this.cognitoManagementOption.name(), "Please Select Existing Cognito User:", waitText);
                this.alterCognitoAccountStatus(memberRecord, true);
            }
            default -> throw new IllegalStateException("Unhandled CognitoManagementOption: %s".formatted(this.cognitoManagementOption.name()));
        }
    }

    private
    void alterCognitoAccountStatus (final @NotNull MemberRecord memberRecord, final boolean shouldPromote) {
        final String cognitoSub = this.cognitoUserPicker.getCognitoSub(memberRecord);
        final boolean cognitoUserIsAdmin = ofNullable(requireNonNull(MemberEventHelper.getDdbItem(cognitoSub, DdbTable.COGNITO)).get(CognitoTableParameter.IS_ADMIN.jsonFieldName())).map(
                                                                                                                                                                                             AttributeValue::bool)
                                                                                                                                                                                     .orElse(false);
        final MessageDialogBuilder msgDialogBuilder = new MessageDialogBuilder().setTitle(this.cognitoManagementOption.name())
                                                                                .addButton(MessageDialogButton.OK);
        if (cognitoUserIsAdmin ^ shouldPromote) {
            SdkClientProvider.getSdkClientProvider()
                             .getSdkClient(DynamoDbClient.class)
                             .putItem(PutItemRequest.builder()
                                                    .tableName(DdbTable.COGNITO.name())
                                                    .item(Map.of(CognitoTableParameter.ID.jsonFieldName(), AttributeValue.fromS(cognitoSub), CognitoTableParameter.MEMBER.jsonFieldName(),
                                                                 AttributeValue.fromS(memberRecord.id()
                                                                                                  .toString()), CognitoTableParameter.IS_ADMIN.jsonFieldName(), AttributeValue.fromBool(shouldPromote)))
                                                    .build());
            msgDialogBuilder.setText("%s %s".formatted(memberRecord.member()
                                                                   .getFullName(), shouldPromote
                                                               ? "Promoted"
                                                               : "Demoted"));
        } else {
            msgDialogBuilder.setText("No Action Taken; %s Already %s".formatted(memberRecord.member()
                                                                                            .getFullName(), shouldPromote
                                                                                        ? "Promoted"
                                                                                        : "Demoted"));
        }
        msgDialogBuilder.build()
                        .showDialog(this.gui);
    }

    @Override
    @NotNull
    public
    WindowBasedTextGUI getGui () {
        return this.gui;
    }

    @Override
    @NotNull
    public
    PickerModel getPicker () {
        return this.cognitoUserPicker;
    }
}
