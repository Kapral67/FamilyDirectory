package org.familydirectory.assets.lambda.function.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.jetbrains.annotations.NotNull;

@SuppressFBWarnings("EI_EXPOSE_REP")
public
record UpdateEvent(@JsonProperty(DdbUtils.PK) @NotNull String id, @JsonProperty("member") @NotNull Member member) {
}
