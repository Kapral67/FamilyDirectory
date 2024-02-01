package org.familydirectory.sdk.adminclient.events.update;

import java.util.Scanner;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.events.model.EventHelper;
import org.familydirectory.sdk.adminclient.utility.Logger;
import org.familydirectory.sdk.adminclient.utility.pickers.MemberPicker;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

public final
class UpdateEvent implements EventHelper {
    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final @NotNull Scanner scanner;

    public
    UpdateEvent (final @NotNull Scanner scanner) {
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
        if (MemberPicker.isEmpty()) {
            throw new IllegalStateException("No Members Exist to Update");
        }
        final MemberRecord ddbMemberRecord = this.getExistingMember("Please Select Existing Member to UPDATE:");
        Logger.warn("Any [Optional] attributes unspecified are removed if currently present.");
        final MemberRecord memberRecord = this.buildMemberRecord(ddbMemberRecord.id(), ddbMemberRecord.familyId());
        {
            final String updateMemberEmail = memberRecord.member()
                                                         .getEmail();
            if (nonNull(updateMemberEmail) && !updateMemberEmail.equals(ddbMemberRecord.member()
                                                                                       .getEmail()))
            {
                this.validateMemberEmailIsUnique(updateMemberEmail);
            }
        }
        this.dynamoDbClient.putItem(PutItemRequest.builder()
                                                  .tableName(DdbTable.MEMBER.name())
                                                  .item(EventHelper.buildMember(memberRecord))
                                                  .build());
        MemberPicker.addEntry(memberRecord);
    }

    @Override
    public @NotNull
    DynamoDbClient getDynamoDbClient () {
        return this.dynamoDbClient;
    }
}
