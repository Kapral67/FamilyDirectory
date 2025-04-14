package org.familydirectory.assets.ddb.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.uuid.UUIDType;
import com.fasterxml.uuid.impl.UUIDUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import lombok.Value;
import lombok.experimental.Accessors;
import org.apache.commons.codec.binary.Base64;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.jetbrains.annotations.NotNull;
import static java.util.Objects.requireNonNull;

@Value
@Accessors(fluent = true)
@JsonDeserialize(using = SyncToken.Deserializer.class)
public
class SyncToken {
    private static final String ID_JSON_FIELD_NAME = DdbUtils.PK;
    private static final String TTL_JSON_FIELD_NAME = "ttl";

    @JsonProperty(ID_JSON_FIELD_NAME)
    @NotNull UUID id;

    @JsonProperty(TTL_JSON_FIELD_NAME)
    @NotNull Instant ttl;

    public
    SyncToken (@NotNull UUID id, @NotNull Instant ttl) {
        this.id = requireNonNull(id);
        if (!UUIDType.TIME_BASED_EPOCH.equals(UUIDUtil.typeOf(id))) {
            throw new IllegalArgumentException("Invalid " + ID_JSON_FIELD_NAME);
        }
        this.ttl = requireNonNull(ttl);
    }

    public static
    SyncToken of (@NotNull String base64orJson, @NotNull ObjectMapper mapper) throws IOException {
        if (Base64.isBase64(base64orJson)) {
            base64orJson = new String(Base64.decodeBase64(base64orJson), StandardCharsets.UTF_8);
        }
        return mapper.readValue(base64orJson, SyncToken.class);
    }

    @Override
    @JsonValue
    @NotNull
    public
    String toString () {
        return "{\"%s\":\"%s\",\"%s\":\"%s\"}".formatted(ID_JSON_FIELD_NAME, id, TTL_JSON_FIELD_NAME, ttl);
    }

    @NotNull
    public
    String toBase64 () {
        return Base64.encodeBase64URLSafeString(this.toString()
                                                    .getBytes(StandardCharsets.UTF_8));
    }

    @NotNull
    public
    Instant timestamp() {
        return Instant.ofEpochMilli(UUIDUtil.extractTimestamp(this.id));
    }

    public static final
    class Deserializer extends JsonDeserializer<SyncToken> {
        @Override
        @NotNull
        public
        SyncToken deserialize (@NotNull JsonParser p, DeserializationContext context) throws IOException {
            final JsonNode json = p.readValueAsTree();
            final UUID id = UUID.fromString(json.get(ID_JSON_FIELD_NAME)
                                                .asText());
            final Instant ttl = Instant.parse(json.get(TTL_JSON_FIELD_NAME)
                                                  .asText());
            return new SyncToken(id, ttl);
        }
    }
}
