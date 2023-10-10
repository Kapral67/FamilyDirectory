package org.familydirectory.assets.lambda.function.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;
import static java.util.Objects.requireNonNull;

public final
class CreateEvent {
    @JsonProperty("member")
    private final @NotNull Member member;

    @JsonProperty("isSpouse")
    private final @NotNull Boolean isSpouse;

    public
    CreateEvent (@NotNull Member member, @NotNull Boolean isSpouse) {
        this.member = requireNonNull(member);
        this.isSpouse = isSpouse;
    }

    public @NotNull
    Member getMember () {
        return this.member;
    }

    public @NotNull
    Boolean getIsSpouse () {
        return this.isSpouse;
    }
}
