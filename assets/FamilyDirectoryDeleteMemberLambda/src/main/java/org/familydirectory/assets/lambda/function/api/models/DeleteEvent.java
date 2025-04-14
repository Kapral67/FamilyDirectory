package org.familydirectory.assets.lambda.function.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.jetbrains.annotations.NotNull;

public
record DeleteEvent(@JsonProperty(DdbUtils.PK) @NotNull String id) {
}
