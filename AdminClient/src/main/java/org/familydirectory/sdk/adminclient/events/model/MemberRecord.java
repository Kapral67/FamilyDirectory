package org.familydirectory.sdk.adminclient.events.model;

import java.util.UUID;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;

public
record MemberRecord(@NotNull UUID id, @NotNull Member member) {
}
