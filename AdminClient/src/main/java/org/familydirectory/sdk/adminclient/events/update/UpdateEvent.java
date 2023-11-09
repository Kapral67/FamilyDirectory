package org.familydirectory.sdk.adminclient.events.update;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.UUID;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.sdk.adminclient.events.model.EventHelper;
import org.familydirectory.sdk.adminclient.events.model.MemberRecord;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public final
class UpdateEvent implements EventHelper {
    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final @NotNull Scanner scanner;
    private final @NotNull UUID memberId;

    public
    UpdateEvent (final @NotNull Scanner scanner, final @NotNull UUID memberId) {
        super();
        this.scanner = requireNonNull(scanner);
        this.memberId = requireNonNull(memberId);
    }

    @Override
    public @NotNull
    Scanner scanner () {
        return this.scanner;
    }

    @Override
    public
    void execute () {
        final Map<String, AttributeValue> memberMap = this.getDdbItem(this.memberId.toString(), DdbTable.MEMBER);
        if (isNull(memberMap)) {
            throw new NoSuchElementException("Member Not Found");
        }
        System.out.println("WARNING: Any [Optional] attributes unspecified are removed if currently present.");
        final MemberRecord memberRecord = this.buildMemberRecord(this.memberId);
        {
            final String ddbMemberEmail = ofNullable(memberMap.get(MemberTableParameter.EMAIL.jsonFieldName())).map(AttributeValue::s)
                                                                                                               .orElse(null);
            final String updateMemberEmail = memberRecord.member()
                                                         .getEmail();
            if (nonNull(updateMemberEmail) && !updateMemberEmail.equals(ddbMemberEmail)) {
                this.validateMemberEmailIsUnique(updateMemberEmail);
            }
        }
        this.dynamoDbClient.putItem(PutItemRequest.builder()
                                                  .tableName(DdbTable.MEMBER.name())
                                                  .item(this.buildMember(memberRecord, memberMap.get(MemberTableParameter.FAMILY_ID.jsonFieldName())
                                                                                                .s()))
                                                  .build());
    }

    @Override
    public @NotNull
    DynamoDbClient getDynamoDbClient () {
        return this.dynamoDbClient;
    }
}
