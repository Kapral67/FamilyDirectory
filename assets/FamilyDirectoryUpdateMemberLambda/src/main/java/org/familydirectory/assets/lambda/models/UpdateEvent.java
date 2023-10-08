package org.familydirectory.assets.lambda.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;

public final
class UpdateEvent {
    @JsonProperty("member")
    private final @NotNull Member member;

    public
    UpdateEvent (@NotNull Member member) {
        this.member = member;
    }

    public @NotNull
    Member getMember () {
        return this.member;
    }
}
