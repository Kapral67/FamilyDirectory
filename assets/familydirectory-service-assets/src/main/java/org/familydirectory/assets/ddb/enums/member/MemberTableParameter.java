package org.familydirectory.assets.ddb.enums.member;

import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.DdbType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import static software.amazon.awscdk.services.dynamodb.AttributeType.STRING;
import static software.amazon.awscdk.services.dynamodb.ProjectionType.KEYS_ONLY;

public
enum MemberTableParameter {
    ID(DdbType.STR, DdbTable.PK.getName(), null),
    KEY(DdbType.STR, "key", GlobalSecondaryIndexProps.builder()
                                                     .indexName("MemberKey")
                                                     .partitionKey(Attribute.builder()
                                                                            .name("key")
                                                                            .type(STRING)
                                                                            .build())
                                                     .projectionType(KEYS_ONLY)
                                                     .build()),
    FIRST_NAME(DdbType.STR, "firstName", null),
    MIDDLE_NAME(DdbType.STR, "middleName", null),
    LAST_NAME(DdbType.STR, "lastName", null),
    SUFFIX(DdbType.STR, "suffix", null),
    BIRTHDAY(DdbType.STR, "birthday", null),
    DEATHDAY(DdbType.STR, "deathday", null),
    EMAIL(DdbType.STR, "email", GlobalSecondaryIndexProps.builder()
                                                         .indexName("MemberEmail")
                                                         .partitionKey(Attribute.builder()
                                                                                .name("email")
                                                                                .type(STRING)
                                                                                .build())
                                                         .projectionType(KEYS_ONLY)
                                                         .build()),
    PHONES(DdbType.MAP, "phones", null),
    ADDRESS(DdbType.STR_SET, "address", null),
    FAMILY_ID(DdbType.STR, "familyId", null);

    @NotNull
    private final DdbType ddbType;
    @NotNull
    private final String jsonFieldName;
    @Nullable
    private final GlobalSecondaryIndexProps gsiProps;

    MemberTableParameter (final @NotNull DdbType ddbType, final @NotNull String jsonFieldName, final @Nullable GlobalSecondaryIndexProps gsiProps) {
        this.ddbType = ddbType;
        this.jsonFieldName = jsonFieldName;
        this.gsiProps = gsiProps;
    }

    @NotNull
    public final
    DdbType ddbType () {
        return this.ddbType;
    }

    @NotNull
    public final
    String jsonFieldName () {
        return this.jsonFieldName;
    }

    @Nullable
    public final
    GlobalSecondaryIndexProps gsiProps () {
        return this.gsiProps;
    }
}
