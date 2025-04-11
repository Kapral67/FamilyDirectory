package org.familydirectory.assets.ddb.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Objects.requireNonNull;

public
class InstantDeserializer extends JsonDeserializer<Instant> {
    @Override
    public Instant deserialize (final @NotNull JsonParser jsonParser, final @Nullable DeserializationContext deserializationContext) throws IOException {
        return Instant.parse(requireNonNull(jsonParser.getText()));
    }
}
