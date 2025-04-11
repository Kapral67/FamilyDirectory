package org.familydirectory.assets.lambda.function.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressFBWarnings("EI_EXPOSE_REP")
public
record CreateEvent(@JsonProperty("member") @NotNull Member member, @JsonProperty("isSpouse") @NotNull Boolean isSpouse, @JsonProperty("ancestor") @Nullable String ancestor) {
}
