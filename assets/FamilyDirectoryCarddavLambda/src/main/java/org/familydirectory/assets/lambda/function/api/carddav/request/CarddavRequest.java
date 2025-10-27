package org.familydirectory.assets.lambda.function.api.carddav.request;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.milton.http.Auth;
import io.milton.http.Cookie;
import io.milton.http.DateUtils;
import io.milton.http.FileItem;
import io.milton.http.Request;
import io.milton.http.RequestParseException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.CRLF;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.INFINITY;

public
class CarddavRequest implements Request {
    private final Map<String, Object> attributes = new HashMap<>(0);

    private final SequencedMap<String, String> headers;
    private final Method method;
    private final String requestUri;
    private final InputStream body;

    public CarddavRequest(final @NotNull String rawRequest) {
        Objects.requireNonNull(rawRequest);
        final String[] request = rawRequest.split(CRLF + CRLF, 2);
        final String[] headers = request[0].split(CRLF);

        this.headers = getHeadersMap(Arrays.copyOfRange(headers, 1, headers.length));

        final String[] requestAttributes = headers[0].split(" ");
        this.method = Method.valueOf(requestAttributes[0].toUpperCase(Locale.ROOT));
        this.requestUri = requestAttributes[1];
        // version is assumed to be HTTP/1.1

        final String body = request.length > 1 ? request[1] : "";
        this.body = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
    }

    @UnmodifiableView
    @NotNull
    private static SequencedMap<String, String> getHeadersMap(final String @NotNull [] headers) {
        final SequencedMap<String, String> headersMap = new LinkedHashMap<>();
        for (final String header : headers) {
            final String[] headerPair = header.split(": ", 2);
            String key = headerPair[0].trim();
            try {
                key = Header.valueOf(key.toUpperCase(Locale.ROOT)).code;
            } catch (final IllegalArgumentException ignored) {
            }
            if (Header.AUTHORIZATION.code.equals(key)) {
                continue;
            }
            headersMap.put(key, headerPair[1].trim());
        }
        return Collections.unmodifiableSequencedMap(headersMap);
    }

    @Override
    @UnmodifiableView
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public
    Map<String, String> getHeaders () {
        return this.headers;
    }

    @Override
    public
    String getFromAddress () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getLockTokenHeader () {
        return this.headers.get(Header.LOCK_TOKEN.code);
    }

    @Override
    public
    String getRequestHeader (Header header) {
        return this.headers.get(header.code);
    }

    @Override
    public
    Method getMethod () {
        return this.method;
    }

    @Override
    public
    Auth getAuthorization () {
        return null;
    }

    @Override
    public
    void setAuthorization (Auth auth) {}

    @Override
    public
    String getRefererHeader () {
        return this.headers.get(Header.REFERER.code);
    }

    @Override
    public
    String getTimeoutHeader () {
        return this.headers.get(Header.TIMEOUT.code);
    }

    @Override
    public
    String getIfHeader () {
        return this.headers.get(Header.IF.code);
    }

    @Override
    public
    String getIfRangeHeader () {
        return this.headers.get(Header.IF_RANGE.code);
    }

    @Override
    public
    String getIfMatchHeader () {
        return this.headers.get(Header.IF_MATCH.code);
    }

    @Override
    public
    String getIfNoneMatchHeader () {
        return this.headers.get(Header.IF_NONE_MATCH.code);
    }

    @Override
    public
    Date getIfModifiedHeader () {
        final String ifModifiedHeader = this.headers.get(Header.IF_MODIFIED.code);

        if (ifModifiedHeader == null || ifModifiedHeader.isBlank()) {
            return null;
        }

        try {
            return DateUtils.parseDate(ifModifiedHeader);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public
    int getDepthHeader () {
        int depth = INFINITY.intValue();
        try {
            depth = Integer.parseInt(this.headers.get(Header.DEPTH.code));
        } catch (NumberFormatException ignored) {}
        return Math.min(depth, INFINITY.intValue());
    }

    @Override
    public
    String getAbsoluteUrl () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getAbsolutePath () {
        final int queryIndex = this.requestUri.indexOf('?');
        return queryIndex > 0 ? this.requestUri.substring(0, queryIndex) : this.requestUri;
    }

    @Override
    public
    String getHostHeader () {
        return this.headers.get(Header.HOST.code);
    }

    @Override
    public
    String getDestinationHeader () {
        return this.headers.get(Header.DESTINATION.code);
    }

    @Override
    public
    String getExpectHeader () {
        return this.headers.get(Header.EXPECT.code);
    }

    @Override
    public
    InputStream getInputStream () {
        return this.body;
    }

    @Override
    public
    void parseRequestParameters (Map<String, String> map, Map<String, FileItem> map1) throws RequestParseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getContentTypeHeader () {
        return this.headers.get(Header.CONTENT_TYPE.code);
    }

    @Override
    public
    Long getContentLengthHeader () {
        return Long.parseLong(this.headers.get(Header.CONTENT_LENGTH.code));
    }

    @Override
    public
    String getAcceptHeader () {
        return this.headers.get(Header.ACCEPT.code);
    }

    @Override
    public
    String getAcceptEncodingHeader () {
        return this.headers.get(Header.ACCEPT_ENCODING.code);
    }

    @Override
    public
    String getAcceptLanguage () {
        return this.headers.get(Header.ACCEPT_LANGUAGE.code);
    }

    @Override
    public
    String getRangeHeader () {
        return this.headers.get(Header.RANGE.code);
    }

    @Override
    public
    String getContentRangeHeader () {
        return this.headers.get(Header.CONTENT_RANGE.code);
    }

    @Override
    public
    Boolean getOverwriteHeader () {
        return Boolean.parseBoolean(this.headers.get(Header.OVERWRITE.code));
    }

    @Override
    public
    String getOriginHeader () {
        return this.headers.get(Header.ORIGIN.code);
    }

    @Override
    public
    String getUserAgentHeader () {
        return this.headers.get(Header.ORIGIN.code);
    }

    @Override
    public
    Map<String, Object> getAttributes () {
        return Collections.unmodifiableMap(this.attributes);
    }

    @SuppressWarnings("unchecked")
    @Override
    public
    Map<String, String> getParams () {
        return (Map<String, String>) attributes.get("_params");
    }

    @SuppressWarnings("unchecked")
    @Override
    public
    Map<String, FileItem> getFiles () {
        return (Map<String, FileItem>) attributes.get("_files");
    }

    @Override
    public
    Cookie getCookie (String s) {
        return null;
    }

    @Override
    public
    List<Cookie> getCookies () {
        return null;
    }

    @Override
    public
    String getRemoteAddr () {
        return null;
    }

    @Override
    public
    Locale getLocale () {
        return Locale.ENGLISH;
    }
}
