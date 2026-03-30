package org.familydirectory.assets.ddb.models.member;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static java.util.Objects.requireNonNull;

public
record MemberRecord(@NotNull UUID id, @NotNull Member member, @NotNull UUID familyId) implements IMemberRecord {
    public MemberRecord {
        requireNonNull(id);
        requireNonNull(member);
        requireNonNull(familyId);
    }

    @NotNull
    public static MemberRecord convertDdbMap(final @NotNull Map<String, AttributeValue> memberMap) {
        final UUID memberId = Optional.ofNullable(memberMap.get(MemberTableParameter.ID.jsonFieldName()))
                                      .map(AttributeValue::s)
                                      .map(UUID::fromString)
                                      .orElseThrow();
        final UUID familyId = Optional.ofNullable(memberMap.get(MemberTableParameter.FAMILY_ID.jsonFieldName()))
                                      .map(AttributeValue::s)
                                      .map(UUID::fromString)
                                      .orElseThrow();
        return new MemberRecord(memberId, Member.convertDdbMap(memberMap), familyId);
    }

    @Override
    public
    boolean equals (final Object o) {
        return IMemberRecord.equals(this, o);
    }

    @Override
    public
    int hashCode () {
        return IMemberRecord.hashCode(this);
    }

    @Override
    @NotNull
    public
    String toString () {
        return this.member.getFullName();
    }
}
