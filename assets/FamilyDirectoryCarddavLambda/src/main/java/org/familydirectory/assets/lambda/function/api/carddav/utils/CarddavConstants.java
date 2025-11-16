package org.familydirectory.assets.lambda.function.api.carddav.utils;

import io.milton.http.Request.Method;
import io.milton.http.values.Pair;
import io.milton.resource.AccessControlledResource;
import java.util.EnumSet;
import java.util.List;
import static io.milton.http.Request.Method.GET;
import static io.milton.http.Request.Method.HEAD;
import static io.milton.http.Request.Method.OPTIONS;
import static io.milton.http.Request.Method.PROPFIND;
import static io.milton.http.Request.Method.REPORT;
import static java.util.Collections.singletonList;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.dEmpty;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.dParent;

public
enum CarddavConstants {
    ;
    public static final String CRLF = "\r\n";
    public static final Number INFINITY = 3;
    public static final EnumSet<Method> SUPPORTED_METHODS = EnumSet.of(HEAD, PROPFIND, GET, OPTIONS, REPORT);
    public static final String VCARD_CONTENT_TYPE = "text/vcard";
    public static final List<Pair<String, String>> SUPPORTED_ADDRESS_DATA = singletonList(new Pair<>(VCARD_CONTENT_TYPE, "3.0"));
    public static final List<AccessControlledResource.Priviledge> SUPPORTED_PRIVILEGES = singletonList(AccessControlledResource.Priviledge.READ);
    public static final CarddavXmlUtils.DavProperty CURRENT_USER_PRIVILEGE_SET = dParent("current-user-privilege-set", singletonList(
            dParent("privilege", singletonList(dEmpty("read")))
        ));
    public static final String ADDRESS_BOOK = "addressbook";
    public static final String ADDRESS_BOOK_PATH = "/%s".formatted(ADDRESS_BOOK);
    public static final String CONTACTS_COLLECTION_PATH = "%s/".formatted(ADDRESS_BOOK_PATH);
    public static final String PRINCIPALS = "principals";
    public static final String PRINCIPALS_COLLECTION_PATH = "/%s/".formatted(PRINCIPALS);
    public static final String SYSTEM_PRINCIPAL = "system";
    public static final String SYSTEM_PRINCIPAL_PATH = PRINCIPALS_COLLECTION_PATH + SYSTEM_PRINCIPAL;
    public static final int INITIAL_RESOURCE_CONTAINER_SIZE = 256;
    public static final List<String> SUPPORTED_REPORTS = List.of("addressbook-multiget", "addressbook-query", "sync-collection");
}
