package org.familydirectory.assets.lambda.function.api.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;

public
record CreateEvent(@JsonProperty("member") @NotNull Member member, @JsonProperty("isSpouse") @NotNull Boolean isSpouse) {
    @JsonCreator
    public
    CreateEvent {
    }
}
