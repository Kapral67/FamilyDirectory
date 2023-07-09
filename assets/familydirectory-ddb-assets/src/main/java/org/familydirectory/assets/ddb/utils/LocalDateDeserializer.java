package org.familydirectory.assets.ddb.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;

import static java.time.LocalDate.parse;
import static org.familydirectory.assets.ddb.utils.DdbUtils.DATE_FORMATTER;

public class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        return parse(jsonParser.getText(), DATE_FORMATTER);
    }
}
