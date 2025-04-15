package org.familydirectory.assets.lambda.function.api.carddav.response;

import io.milton.http.Cookie;
import io.milton.http.Response;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

public
class CarddavResponse implements Response {
    @Override
    public
    Status getStatus () {
        return null;
    }

    @Override
    public
    Map<String, String> getHeaders () {
        return Map.of();
    }

    @Override
    public
    Long getContentLength () {
        return 0L;
    }

    @Override
    public
    void setContentEncodingHeader (ContentEncoding encoding) {

    }

    @Override
    public
    void setExpiresHeader (Date expiresAt) {

    }

    @Override
    public
    void setLockTokenHeader (String tokenId) {

    }

    @Override
    public
    void setAuthenticateHeader (List<String> challenges) {

    }

    @Override
    public
    void setStatus (Status status) {

    }

    @Override
    public
    void setEtag (String uniqueId) {

    }

    @Override
    public
    void setContentRangeHeader (long start, long finish, Long totalLength) {

    }

    @Override
    public
    void setContentLengthHeader (Long totalLength) {

    }

    @Override
    public
    void setContentTypeHeader (String string) {

    }

    @Override
    public
    String getContentTypeHeader () {
        return "";
    }

    @Override
    public
    Entity getEntity () {
        return null;
    }

    @Override
    public
    void setEntity (Entity entity) {

    }

    @Override
    public
    void setCacheControlMaxAgeHeader (Long deltaSeconds) {

    }

    @Override
    public
    void setCacheControlPrivateMaxAgeHeader (Long deltaSeconds) {

    }

    @Override
    public
    void setCacheControlNoCacheHeader () {

    }

    @Override
    public
    void setLastModifiedHeader (Date date) {

    }

    @Override
    public
    void setDavHeader (String string) {
        // always DAV: 1
    }

    @Override
    public
    void setNonStandardHeader (String code, String value) {

    }

    @Override
    public
    String getNonStandardHeader (String code) {
        return "";
    }

    @Override
    public
    void setAllowHeader (List<String> methodsAllowed) {

    }

    @Override
    public
    OutputStream getOutputStream () {
        return null;
    }

    @Override
    public
    void setLocationHeader (String redirectUrl) {

    }

    @Override
    public
    void setVaryHeader (String string) {

    }

    @Override
    public
    void setDateHeader (Date date) {

    }

    @Override
    public
    String getAccessControlAllowOrigin () {
        return "";
    }

    @Override
    public
    void setAccessControlAllowOrigin (String s) {

    }

    @Override
    public
    String getAcceptRanges () {
        return "";
    }

    @Override
    public
    void setAcceptRanges (String s) {

    }

    @Override
    public
    void close () {

    }

    @Override
    public
    void sendError (Status status, String message) {

    }

    @Override
    public
    void sendRedirect (String url) {

    }

    @Override
    public
    void sendPermanentRedirect (String url) {

    }

    @Override
    public
    Cookie setCookie (Cookie cookie) {
        return null;
    }

    @Override
    public
    Cookie setCookie (String name, String value) {
        return null;
    }
}
