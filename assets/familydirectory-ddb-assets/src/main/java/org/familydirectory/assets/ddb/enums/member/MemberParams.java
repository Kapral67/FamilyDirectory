package org.familydirectory.assets.ddb.enums.member;

import org.familydirectory.assets.ddb.enums.DdbType;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import static org.familydirectory.assets.ddb.enums.DdbTable.PK;
import static org.familydirectory.assets.ddb.enums.DdbType.MAP;
import static org.familydirectory.assets.ddb.enums.DdbType.STR;
import static org.familydirectory.assets.ddb.enums.DdbType.STR_SET;
import static software.amazon.awscdk.services.dynamodb.AttributeType.STRING;
import static software.amazon.awscdk.services.dynamodb.ProjectionType.KEYS_ONLY;

public
enum MemberParams {
    ID(STR, PK.getName(), null),
    KEY(STR, "key", GlobalSecondaryIndexProps.builder()
                                             .indexName("MemberKey")
                                             .partitionKey(Attribute.builder()
                                                                    .name("key")
                                                                    .type(STRING)
                                                                    .build())
                                             .projectionType(KEYS_ONLY)
                                             .build()),
    FIRST_NAME(STR, "firstName", null),
    LAST_NAME(STR, "lastName", null),
    SUFFIX(STR, "suffix", null),
    BIRTHDAY(STR, "birthday", null),
    DEATHDAY(STR, "deathday", null),
    EMAIL(STR, "email", GlobalSecondaryIndexProps.builder()
                                                 .indexName("MemberEmail")
                                                 .partitionKey(Attribute.builder()
                                                                        .name("email")
                                                                        .type(STRING)
                                                                        .build())
                                                 .projectionType(KEYS_ONLY)
                                                 .build()),
    PHONES(MAP, "phones", null),
    ADDRESS(STR_SET, "address", null),
    FAMILY_ID(STR, "familyId", null);
    private final DdbType ddbType;
    private final String jsonFieldName;
    @Nullable
    private final GlobalSecondaryIndexProps gsiProps;

    MemberParams (final DdbType ddbType, final String jsonFieldName, final @Nullable GlobalSecondaryIndexProps gsiProps) {
        this.ddbType = ddbType;
        this.jsonFieldName = jsonFieldName;
        this.gsiProps = gsiProps;
    }

    public final
    DdbType ddbType () {
        return this.ddbType;
    }

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
