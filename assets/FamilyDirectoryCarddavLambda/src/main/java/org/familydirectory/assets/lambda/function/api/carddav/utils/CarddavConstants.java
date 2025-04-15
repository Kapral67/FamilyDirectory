package org.familydirectory.assets.lambda.function.api.carddav.utils;

import io.milton.http.Request.Method;
import java.util.EnumSet;
import static io.milton.http.Request.Method.GET;
import static io.milton.http.Request.Method.HEAD;
import static io.milton.http.Request.Method.OPTIONS;
import static io.milton.http.Request.Method.PROPFIND;
import static io.milton.http.Request.Method.REPORT;

public
enum CarddavConstants {
    ;
    public static final String CRLF = "\r\n";
    public static final Number INFINITY = 3;
    public static final EnumSet<Method> SUPPORTED_METHODS = EnumSet.of(HEAD, PROPFIND, GET, OPTIONS, REPORT);
}
