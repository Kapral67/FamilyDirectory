package org.familydirectory.assets.lambda.function.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;

public
class DeleteEvent {
    @JsonProperty("member")
    private final @NotNull Member member;

    public
    DeleteEvent (@NotNull Member member) {
        this.member = member;
    }

    public @NotNull
    Member getMember () {
        return this.member;
    }
}
