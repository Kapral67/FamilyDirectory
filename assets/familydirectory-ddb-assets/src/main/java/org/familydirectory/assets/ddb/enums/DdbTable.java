package org.familydirectory.assets.ddb.enums;

import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import static org.familydirectory.assets.ddb.enums.member.MemberParams.EMAIL;
import static software.amazon.awscdk.services.dynamodb.AttributeType.STRING;
import static software.amazon.awscdk.services.dynamodb.ProjectionType.KEYS_ONLY;

public
enum DdbTable {
    MEMBERS("MembersTableArn", GlobalSecondaryIndexProps.builder()
                                                        .indexName("MemberEmail")
                                                        .partitionKey(Attribute.builder()
                                                                               .name(EMAIL.jsonFieldName())
                                                                               .type(STRING)
                                                                               .build())
                                                        .projectionType(KEYS_ONLY)
                                                        .build());
//    FAMILIES("FamiliesTableArn", null);

    /**
     * For {@link DdbTable#MEMBERS}: This is the sha256Hex hash of a Member fullName & birthdayString, concatenated.
     * See {@link Member} for how fullName and birthdayString are computed
     */
    public static final Attribute PK = Attribute.builder()
                                                .name("id")
                                                .type(STRING)
                                                .build();
    private final String arnExportName;
    @Nullable
    private final GlobalSecondaryIndexProps globalSecondaryIndexProps;

    DdbTable (final String arnExportName, final @Nullable GlobalSecondaryIndexProps globalSecondaryIndexProps) {
        this.arnExportName = arnExportName;
        this.globalSecondaryIndexProps = globalSecondaryIndexProps;
    }

    public final
    String arnExportName () {
        return this.arnExportName;
    }

    public final @Nullable
    GlobalSecondaryIndexProps globalSecondaryIndexProps () {
        return this.globalSecondaryIndexProps;
    }
}
