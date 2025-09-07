package org.familydirectory.assets.lambda.function.api.carddav.utils;

import io.milton.http.Request.Method;
import io.milton.http.values.Pair;
import io.milton.resource.AccessControlledResource;
import java.util.EnumSet;
import java.util.List;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import static io.milton.http.Request.Method.GET;
import static io.milton.http.Request.Method.HEAD;
import static io.milton.http.Request.Method.OPTIONS;
import static io.milton.http.Request.Method.PROPFIND;
import static io.milton.http.Request.Method.REPORT;
import static java.lang.System.getenv;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public
enum CarddavConstants {
    ;
    public static final String CRLF = "\r\n";
    public static final Number INFINITY = 3;
    public static final EnumSet<Method> SUPPORTED_METHODS = EnumSet.of(HEAD, PROPFIND, GET, OPTIONS, REPORT);
    public static final String VCARD_CONTENT_TYPE = "text/vcard";
    public static final List<Pair<String, String>> SUPPORTED_ADDRESS_DATA = singletonList(new Pair<>(VCARD_CONTENT_TYPE, "3.0"));
    public static final List<AccessControlledResource.Priviledge> SUPPORTED_PRIVILEGES = singletonList(AccessControlledResource.Priviledge.READ);
    public static final String URL = "https://carddav." + requireNonNull(getenv(LambdaUtils.EnvVar.HOSTED_ZONE_NAME.name()));
    public static final String SYNC_TOKEN_PATH = "/synctoken/";
    public static final String ADDRESS_BOOK = "addressbook";
    public static final String ADDRESS_BOOK_PATH = "/%s".formatted(ADDRESS_BOOK);
    public static final String CONTACTS_COLLECTION_PATH = "%s/".formatted(ADDRESS_BOOK_PATH);
    public static final String PRINCIPALS_COLLECTION_PATH = "/principals/";
    public static final String USER_PRINCIPALS_COLLECTION_PATH = "/principals/users/";
    public static final String SYSTEM_PRINCIPAL = "system";
    public static final String SYSTEM_PRINCIPAL_PATH = "/principals/" +  SYSTEM_PRINCIPAL;
}
