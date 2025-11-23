package org.familydirectory.assets.lambda.function.api.carddav.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.INFINITY;

@SuppressFBWarnings("EI_EXPOSE_REP")
public
record CarddavRequest(
    @JsonProperty("method") String method,
    @JsonProperty("path") String path,
    @JsonProperty("headers") Map<String, String> headers,
    @JsonProperty("body") String body
) {
    public String getHeader(Request.Header header) {
        return Optional.ofNullable(headers())
                       .map(Map::entrySet)
                       .stream()
                       .flatMap(Set::stream)
                       .filter(e -> header.code.equalsIgnoreCase(e.getKey()))
                       .findAny()
                       .map(Map.Entry::getValue)
                       .map(String::trim)
                       .orElse(null);
    }

    @Contract(" -> new")
    @NotNull
    public
    InputStream getInputStream() {
        return new ByteArrayInputStream(getBytesUtf8(requireNonNullElse(body(), "")));
    }

    public
    int getDepthHeader () {
        int depth = INFINITY;
        try {
            depth = Integer.parseInt(getHeader(Request.Header.DEPTH));
        } catch (NumberFormatException ignored) {
        }
        return Math.min(depth, INFINITY);
    }

    public
    Request.Method getMethod() throws BadRequestException {
        try {
            return Request.Method.valueOf(method());
        } catch (Exception e) {
            throw new BadRequestException("Bad Method `%s`".formatted(method()), e);
        }
    }
}
