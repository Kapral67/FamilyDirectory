package org.familydirectory.assets.lambda.function.api.carddav.response;

import io.milton.http.Response;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.apache.commons.codec.binary.StringUtils;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.CRLF;

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

    @Override
    public String toString() {
        final var sb = new StringBuilder(this.status.toString());
        final byte[] bodyBytes = Optional.ofNullable(this.body)
                                         .map(StringUtils::getBytesUtf8)
                                         .orElse(new byte[0]);

        sb.append(CRLF);

        if (bodyBytes.length > 0) {
            if (!headers.containsKey(Response.Header.CONTENT_TYPE)) {
                throw new IllegalStateException("Missing " + Response.Header.CONTENT_TYPE.code);
            }
            sb.append(Response.Header.CONTENT_LENGTH.code)
              .append(": ")
              .append(bodyBytes.length)
              .append(CRLF);
        }

        for (final var header : headers.entrySet()) {
            if (Response.Header.CONTENT_LENGTH.equals(header.getKey())) {
                continue;
            }
            sb.append(header.getKey().code)
              .append(": ")
              .append(header.getValue())
              .append(CRLF);
        }

        sb.append(CRLF);

        if (bodyBytes.length > 0) {
            sb.append(this.body);
        }

        return sb.toString();
    }
}
