package org.familydirectory.assets.ddb.models.member;

import java.util.UUID;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;
import static java.util.Objects.isNull;

public
record MemberRecord(@NotNull UUID id, @NotNull Member member, @NotNull UUID familyId) {
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
