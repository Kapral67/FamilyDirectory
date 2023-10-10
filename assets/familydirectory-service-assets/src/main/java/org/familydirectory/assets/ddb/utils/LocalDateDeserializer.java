package org.familydirectory.assets.ddb.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDate;
import org.jetbrains.annotations.NotNull;
import static java.time.LocalDate.parse;

public
class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public
    LocalDate deserialize (@NotNull JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return parse(jsonParser.getText(), DdbUtils.DATE_FORMATTER);
    }
}
