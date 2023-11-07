package org.familydirectory.assets.lambda.function.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

public
record DeleteEvent(@JsonProperty("id") @NotNull String id) {
}
