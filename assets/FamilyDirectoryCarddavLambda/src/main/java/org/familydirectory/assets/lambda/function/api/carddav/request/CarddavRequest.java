package org.familydirectory.assets.lambda.function.api.carddav.request;

import io.milton.http.Auth;
import io.milton.http.Cookie;
import io.milton.http.FileItem;
import io.milton.http.Request;
import io.milton.http.RequestParseException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.UnmodifiableView;

public
class CarddavRequest implements Request {

    private static final String CRLF = "\r\n";

    private final Map<String, String> headers;
    private final InputStream body;

    public CarddavRequest(final String rawRequest) {
        final String[] request = rawRequest.split(CRLF + CRLF, 2);

        final String[] headers = request[0].split(CRLF);
        final Map<String, String> headersMap = new LinkedHashMap<>();
        for (int i = 1; i < headers.length; ++i) {
            final String[] header = headers[i].split(": ", 2);
            String key = header[0].trim();
            try {
                key = Header.valueOf(key.toUpperCase(Locale.ROOT)).code;
            } catch (final IllegalArgumentException ignored) {}
            if (Header.AUTHORIZATION.code.equals(key)) {
                continue;
            }
            headersMap.put(key, header[1].trim());
        }
        this.headers = Collections.unmodifiableMap(headersMap);

        final String requestLine = headers[0];

        final String body = request.length > 1 ? request[1] : "";
        this.body = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @UnmodifiableView
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
        throw new UnsupportedOperationException();
    }

    @Override
    public
    String getRequestHeader (Header header) {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    Method getMethod () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    Auth getAuthorization () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    void setAuthorization (Auth auth) {
        throw new UnsupportedOperationException();
    }

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
