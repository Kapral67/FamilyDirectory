package org.familydirectory.assets.ddb.models.member;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

public
record MemberRecord(@NotNull UUID id, @NotNull Member member, @NotNull UUID familyId) {
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
        if (this == o) {
            return true;
        } else if (isNull(o) || !this.getClass()
                                     .equals(o.getClass()))
        {
            return false;
        }
        return this.id.equals(((MemberRecord) o).id());
    }

    @Override
    public
    int hashCode () {
        return this.id.hashCode();
    }

    @Override
    @NotNull
    public
    String toString () {
        return this.member.getFullName();
    }
}
