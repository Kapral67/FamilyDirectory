package org.familydirectory.assets.ddb.models.member;

import java.util.UUID;

public
interface IMemberRecord {
    UUID id ();
    UUID familyId ();
    default boolean isInLaw () {
        return !this.id().equals(this.familyId());
    }
    static boolean equals (IMemberRecord a, Object b) {
        if (a == b) {
            return true;
        }
        if (b instanceof IMemberRecord rhs) {
            return a.id().equals(rhs.id());
        }
        return false;
    }
    static int hashCode (IMemberRecord a) {
        return a.id().hashCode();
    }
}
