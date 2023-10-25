package org.familydirectory.assets.ddb.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDate;
import org.familydirectory.assets.ddb.models.member.MemberModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Objects.requireNonNull;

public
class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public
    LocalDate deserialize (final @NotNull JsonParser jsonParser, final @Nullable DeserializationContext deserializationContext) throws IOException
    {
        return MemberModel.convertStringToDate(requireNonNull(jsonParser.getText()));
    }
}
