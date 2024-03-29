package org.familydirectory.assets.lambda.function.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public
record CreateEvent(@JsonProperty("member") @NotNull Member member, @JsonProperty("isSpouse") @NotNull Boolean isSpouse, @JsonProperty("ancestor") @Nullable String ancestor) {
}
