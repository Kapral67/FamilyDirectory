package org.familydirectory.assets.lambda.function.api.carddav.request;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.milton.http.Auth;
import io.milton.http.Cookie;
import io.milton.http.FileItem;
import io.milton.http.Request;
import io.milton.http.RequestParseException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public
class CarddavRequest implements Request {

    private static final String CRLF = "\r\n";

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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getTimeoutHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getIfHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getIfRangeHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getIfMatchHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getIfNoneMatchHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    Date getIfModifiedHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    int getDepthHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getAbsoluteUrl () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getAbsolutePath () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getHostHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getDestinationHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getExpectHeader () {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    @Override
    public
    Long getContentLengthHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getAcceptHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getAcceptEncodingHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getAcceptLanguage () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getRangeHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getContentRangeHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    Boolean getOverwriteHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getOriginHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getUserAgentHeader () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    Map<String, Object> getAttributes () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    Map<String, String> getParams () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    Map<String, FileItem> getFiles () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    Cookie getCookie (String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    List<Cookie> getCookies () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getRemoteAddr () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    Locale getLocale () {
        throw new UnsupportedOperationException();
    }
}
