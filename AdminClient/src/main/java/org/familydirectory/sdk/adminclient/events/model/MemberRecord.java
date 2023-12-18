package org.familydirectory.sdk.adminclient.events.model;

import java.util.UUID;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;

public
record MemberRecord(@NotNull UUID id, @NotNull Member member, @NotNull UUID familyId) {
    @Override
    public
    boolean equals (final Object o) {
        if (this == o) {
            return true;
        } else if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        return this.id.equals(((MemberRecord) o).id());
    }

    @Override
    public
    int hashCode () {
        return this.id.hashCode();
    }
}
