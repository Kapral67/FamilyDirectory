package org.familydirectory.assets.lambda.function.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;

public
record UpdateEvent(@JsonProperty("id") @NotNull String id, @JsonProperty("member") @NotNull Member member) {
}
