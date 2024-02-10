package org.familydirectory.sdk.adminclient.events.update;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.enums.Commands;
import org.familydirectory.sdk.adminclient.events.model.MemberEventHelper;
import org.familydirectory.sdk.adminclient.utility.SdkClientProvider;
import org.familydirectory.sdk.adminclient.utility.pickers.MemberPicker;
import org.familydirectory.sdk.adminclient.utility.pickers.model.PickerModel;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

public final
class UpdateEvent implements MemberEventHelper {
    private final @NotNull WindowBasedTextGUI gui;
    private final @NotNull MemberPicker memberPicker;
    private final @NotNull List<PickerModel> pickerModels;

    public
    UpdateEvent (final @NotNull WindowBasedTextGUI gui, final @NotNull MemberPicker memberPicker, final PickerModel... pickerModels) {
        super();
        this.gui = requireNonNull(gui);
        this.memberPicker = requireNonNull(memberPicker);
        this.pickerModels = Arrays.stream(pickerModels)
                                  .filter(Objects::nonNull)
                                  .toList();
    }

    @Override
    public
    void run () {
        if (this.memberPicker.isEmpty()) {
            throw new IllegalStateException("No Members Exist to Update");
        }
        final MemberRecord ddbMemberRecord = this.getExistingMember(Commands.UPDATE.name(), "Please Select an Existing Member:", "Retrieving Members from AWS, Please Wait");
        final MemberRecord memberRecord = this.buildMemberRecord(ddbMemberRecord.id(), ddbMemberRecord.familyId());
        {
            final String updateMemberEmail = memberRecord.member()
                                                         .getEmail();
            if (nonNull(updateMemberEmail) && !updateMemberEmail.equals(ddbMemberRecord.member()
                                                                                       .getEmail()))
            {
                MemberEventHelper.validateMemberEmailIsUnique(updateMemberEmail);
            }
        }
        SdkClientProvider.getSdkClientProvider()
                         .getSdkClient(DynamoDbClient.class)
                         .putItem(PutItemRequest.builder()
                                                .tableName(DdbTable.MEMBER.name())
                                                .item(MemberEventHelper.buildMember(memberRecord))
                                                .build());
        this.memberPicker.addEntry(memberRecord);
        for (final PickerModel pickerModel : this.pickerModels) {
            pickerModel.addEntry(memberRecord);
        }
    }

    @Override
    public @NotNull
    WindowBasedTextGUI getGui () {
        return this.gui;
    }

    @Override
    public @NotNull
    PickerModel getPicker () {
        return this.memberPicker;
    }
}
