package org.familydirectory.assets.lambda.function.api.carddav.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.milton.http.Response;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.apache.commons.codec.binary.StringUtils;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.apache.commons.codec.binary.StringUtils.newStringUtf8;
import static org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper.OBJECT_MAPPER;

/**
 * @see Response
 */
@Value
@Builder
public
class CarddavResponse {
    @NonNull Response.Status status;
    @Singular Map<Response.Header, String> headers;
    String body;

    private record Json(
        @JsonProperty("status") Integer status,
        @JsonProperty("headers") Map<String, String> headers,
        @JsonProperty("body") String body
    ) {}

    @Override
    public String toString() {
        final byte[] bodyBytes = Optional.ofNullable(this.body)
                                         .map(StringUtils::getBytesUtf8)
                                         .orElse(new byte[0]);
        final var headers = new HashMap<>(this.headers);
        if (bodyBytes.length > 0 && !this.headers.containsKey(Response.Header.CONTENT_TYPE)) {
            throw new IllegalStateException("Missing " + Response.Header.CONTENT_TYPE.code);
        }
        if (bodyBytes.length > 0 || !this.headers.containsKey(Response.Header.CONTENT_LENGTH)) {
            headers.put(Response.Header.CONTENT_LENGTH, String.valueOf(bodyBytes.length));
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(new Json(
                this.status.code,
                headers.entrySet().stream()
                       .map(entry ->
                            Map.entry(entry.getKey().code.toLowerCase(Locale.ROOT), entry.getValue())
                       ).collect(toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)),
                newStringUtf8(bodyBytes)
            ));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
