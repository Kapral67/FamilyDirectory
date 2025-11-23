package org.familydirectory.assets.lambda.function.api;

import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.http.exceptions.BadRequestException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.xml.namespace.QName;
import org.familydirectory.assets.lambda.function.api.carddav.request.CarddavRequest;
import org.familydirectory.assets.lambda.function.api.carddav.resource.AbstractResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.AbstractResourceObject;
import org.familydirectory.assets.lambda.function.api.carddav.resource.PresentMemberResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.PrincipalCollectionResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.RootCollectionResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.SystemPrincipal;
import org.familydirectory.assets.lambda.function.api.carddav.resource.UserPrincipal;
import org.familydirectory.assets.lambda.function.api.carddav.response.CarddavResponse;
import org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.DavProperty;
import org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.DavResponse;
import org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.DavPropStat;
import org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.PropFindRequest;
import static io.milton.http.DateUtils.formatForHeader;
import static io.milton.http.DateUtils.formatForWebDavModifiedDate;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.joining;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.CURRENT_USER_PRIVILEGE_SET;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.PRINCIPALS_COLLECTION_PATH;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.VCARD_CONTENT_TYPE;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.CARDDAV_NS;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.DAV_NS;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.cParent;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.cProp;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.dEmpty;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.dParent;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.dProp;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.okPropstat;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.parsePropFind;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.renderMultistatus;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.statusPropstat;

enum CarddavResponseUtils {
    ;
    static final CarddavResponse FORBIDDEN = CarddavResponse.builder()
                                                            .status(Response.Status.SC_FORBIDDEN)
                                                            .build();
    static final CarddavResponse NOT_FOUND = CarddavResponse.builder()
                                                            .status(Response.Status.SC_NOT_FOUND)
                                                            .build();

    private static
    String getAllowHeader(AbstractResourceObject resource) {
        return resource.getAllowedMethods()
                       .stream()
                       .map(method -> method.code)
                       .collect(joining(","));
    }

    static
    String normalizeHref(String href) {
        try {
            final var uri = URI.create(href);
            final var path = uri.getPath();
            return (path == null || path.isEmpty()) ? href : path;
        } catch (final IllegalArgumentException e) {
            return href;
        }
    }

    static
    CarddavResponse getDefaultMethodResponse(Request.Method method, AbstractResourceObject resource) {
        return method.isWrite ? FORBIDDEN : methodNotAllowed(resource);
    }

    static
    CarddavResponse methodNotAllowed(AbstractResourceObject resource) {
        return CarddavResponse.builder()
                              .status(Response.Status.SC_METHOD_NOT_ALLOWED)
                              .header(Response.Header.ALLOW, getAllowHeader(resource))
                              .build();
    }

    static
    CarddavResponse options(AbstractResourceObject resource) {
        return CarddavResponse.builder()
                              .status(Response.Status.SC_OK)
                              .header(Response.Header.ALLOW, getAllowHeader(resource))
                              .header(Response.Header.DAV, "1,3,addressbook,sync-collection")
                              .build();
    }

    static
    DavProperty getCurrentUserPrincipalProp(UserPrincipal principal) {
        return dParent("current-user-principal", singletonList(dProp("href", principal.getPrincipalURL())));
    }

    static
    DavProperty getPrincipalUrlProp(AbstractResource resource) {
        return dParent("principal-URL", singletonList(dProp("href", resource.getPrincipalURL())));
    }

    static
    List<DavProperty> getPresentMemberResourceProps(PresentMemberResource resource) {
        return getPresentMemberResourceProps(resource, emptyList(), false);
    }

    static
    Map<QName, DavProperty> getPresentMemberResourceSupportedProps(PresentMemberResource resource, boolean isPropFind) {
        final var supported = new HashMap<>(Map.of(
            new QName(DAV_NS, "getetag"), dProp("getetag", '"' + resource.getEtag() + '"'),
            new QName(DAV_NS, "getlastmodified"), dProp("getlastmodified", formatForWebDavModifiedDate(resource.getModifiedDate())),
            new QName(DAV_NS, "getcontenttype"), dProp("getcontenttype", resource.getContentType(null)),
            new QName(DAV_NS, "resourcetype"), dEmpty("resourcetype"),
            new QName(DAV_NS, "current-user-privilege-set"), CURRENT_USER_PRIVILEGE_SET
        ));

        if (!isPropFind) {
            supported.put(new QName(CARDDAV_NS, "address-data"), cProp("address-data", resource.getAddressData(), emptyMap()));
        }

        return unmodifiableMap(supported);
    }

    static
    List<DavProperty> getPresentMemberResourceProps(PresentMemberResource resource, Collection<QName> requested, boolean isPropFind) {
        Predicate<QName> wants = qn -> requested.isEmpty() || requested.contains(qn);
        return getPresentMemberResourceSupportedProps(resource, isPropFind).entrySet()
                                                                           .stream()
                                                                           .filter(e -> wants.test(e.getKey()))
                                                                           .map(Map.Entry::getValue)
                                                                           .toList();
    }

    static
    boolean needsChildrenForProps(PropFindRequest propFindRequest) {
        return switch (propFindRequest.kind()) {
            case ALL -> true;
            case NAME -> false;
            case LIST -> {
                final var supportedChildProps = Set.of(
                    new QName(DAV_NS, "getetag"),
                    new QName(DAV_NS, "getlastmodified"),
                    new QName(DAV_NS, "getcontenttype")
                );
                for (final var qn : propFindRequest.properties()) {
                    if (supportedChildProps.contains(qn)) {
                        yield true;
                    }
                }
                yield false;
            }
        };
    }

    static
    List<DavPropStat> buildPropStatsForFixedProps(PropFindRequest propFindRequest, Map<QName, DavProperty> supportedProps) {
        final boolean isAllOrName = EnumSet.of(PropFindRequest.Kind.ALL, PropFindRequest.Kind.NAME)
                                           .contains(propFindRequest.kind());

        final var requested = propFindRequest.properties();
        final var okProps = new ArrayList<DavProperty>();
        final var notFoundProps = new ArrayList<DavProperty>();

        if (isAllOrName || requested.isEmpty()) {
            okProps.addAll(supportedProps.values());
        } else {
            for (final var qn : requested) {
                if (supportedProps.containsKey(qn)) {
                    okProps.add(supportedProps.get(qn));
                } else {
                    notFoundProps.add(new DavProperty(qn, null, emptyMap(), emptyList()));
                }
            }
        }

        final var propStats = new ArrayList<DavPropStat>();
        if (!okProps.isEmpty()) {
            propStats.add(okPropstat(unmodifiableList(okProps)));
        }
        if (!notFoundProps.isEmpty()) {
            propStats.add(statusPropstat(Response.Status.SC_NOT_FOUND, unmodifiableList(notFoundProps)));
        }

        return unmodifiableList(propStats);
    }

    static
    CarddavResponse handleRootCollectionResource(CarddavRequest request, RootCollectionResource resource) throws BadRequestException {
        return switch (request.getMethod()) {
            case OPTIONS -> options(resource);
            case PROPFIND -> {
                final var pf = parsePropFind(request::getInputStream);
                final UserPrincipal user = resource.getChildren()
                                                   .stream()
                                                   .filter(PrincipalCollectionResource.class::isInstance)
                                                   .map(PrincipalCollectionResource.class::cast)
                                                   .map(PrincipalCollectionResource::getChildren)
                                                   .flatMap(List::stream)
                                                   .filter(UserPrincipal.class::isInstance)
                                                   .map(UserPrincipal.class::cast)
                                                   .findAny()
                                                   .orElseThrow();
                final var supported = new HashMap<QName, DavProperty>();
                final QName RESOURCETYPE = new QName(DAV_NS, "resourcetype");
                final QName CURR_PRIV = new QName(DAV_NS, "current-user-privilege-set");
                final QName CURR_USER_PRINCIPAL = new QName(DAV_NS, "current-user-principal");
                final QName PRINCIPAL_URL = new QName(DAV_NS, "principal-URL");

                supported.put(RESOURCETYPE, dParent("resourcetype", List.of(dEmpty("collection"))));
                supported.put(CURR_PRIV, CURRENT_USER_PRIVILEGE_SET);
                supported.put(CURR_USER_PRINCIPAL, getCurrentUserPrincipalProp(user));
                supported.put(PRINCIPAL_URL, getPrincipalUrlProp(resource));

                final var propStats = buildPropStatsForFixedProps(pf, supported);
                final var davResponse = new DavResponse("/", propStats);

                yield CarddavResponse.builder()
                                     .status(Response.Status.SC_MULTI_STATUS)
                                     .header(Response.Header.CONTENT_TYPE, Response.APPLICATION_XML)
                                     .body(renderMultistatus(List.of(davResponse)))
                                     .build();
            }
            default -> getDefaultMethodResponse(request.getMethod(), resource);
        };
    }

    static
    CarddavResponse handlePrincipalCollectionResource(CarddavRequest request, PrincipalCollectionResource resource) throws BadRequestException {
        return switch (request.getMethod()) {
            case OPTIONS -> options(resource);
            case PROPFIND -> {
                final var pf = parsePropFind(request::getInputStream);
                final UserPrincipal user = resource.getChildren()
                                                   .stream()
                                                   .filter(UserPrincipal.class::isInstance)
                                                   .map(UserPrincipal.class::cast)
                                                   .findAny()
                                                   .orElseThrow();
                final var supported = new HashMap<QName, DavProperty>();
                final QName RESOURCETYPE = new QName(DAV_NS, "resourcetype");
                final QName CURR_PRIV = new QName(DAV_NS, "current-user-privilege-set");
                final QName CURR_USER_PRINCIPAL = new QName(DAV_NS, "current-user-principal");
                final QName PRINCIPAL_URL = new QName(DAV_NS, "principal-URL");

                supported.put(RESOURCETYPE, dParent("resourcetype", List.of(dEmpty("collection"))));
                supported.put(CURR_PRIV, CURRENT_USER_PRIVILEGE_SET);
                supported.put(CURR_USER_PRINCIPAL, getCurrentUserPrincipalProp(user));
                supported.put(PRINCIPAL_URL, getPrincipalUrlProp(resource));

                final var propStats = buildPropStatsForFixedProps(pf, supported);
                final var davResponse = new DavResponse(PRINCIPALS_COLLECTION_PATH, propStats);

                yield CarddavResponse.builder()
                                     .status(Response.Status.SC_MULTI_STATUS)
                                     .header(Response.Header.CONTENT_TYPE, Response.APPLICATION_XML)
                                     .body(renderMultistatus(List.of(davResponse)))
                                     .build();
            }
            default -> getDefaultMethodResponse(request.getMethod(), resource);
        };
    }

    static
    CarddavResponse handlePresentMemberResource(CarddavRequest request, PresentMemberResource resource) throws BadRequestException {
        return switch (request.getMethod()) {
            case OPTIONS -> options(resource);
            case GET, HEAD -> {
                final var responseBuilder = CarddavResponse.builder()
                                                           .status(Response.Status.SC_OK)
                                                           .header(Response.Header.CONTENT_TYPE, VCARD_CONTENT_TYPE)
                                                           .header(Response.Header.CONTENT_LENGTH, resource.getContentLength().toString())
                                                           .header(Response.Header.ETAG, '"' + resource.getEtag() + '"')
                                                           .header(Response.Header.LAST_MODIFIED, formatForHeader(resource.getModifiedDate()));
                if (Request.Method.GET.equals(request.getMethod())) {
                    responseBuilder.body(resource.getAddressData());
                }
                yield responseBuilder.build();
            }
            case PROPFIND -> {
                final var pf = parsePropFind(request::getInputStream);
                final var supported = getPresentMemberResourceSupportedProps(resource, true);
                final var propStats = buildPropStatsForFixedProps(pf, supported);
                final var davResponse = new DavResponse(resource.getHref(), propStats);
                yield CarddavResponse.builder()
                                     .status(Response.Status.SC_MULTI_STATUS)
                                     .header(Response.Header.CONTENT_TYPE, Response.APPLICATION_XML)
                                     .body(renderMultistatus(singletonList(davResponse)))
                                     .build();
            }
            default -> getDefaultMethodResponse(request.getMethod(), resource);
        };
    }

    static
    CarddavResponse handleDeletedMemberResource() {
        return NOT_FOUND;
    }

    static
    CarddavResponse handleSystemPrincipal(CarddavRequest request, SystemPrincipal principal) throws BadRequestException {
        return switch (request.getMethod()) {
            case OPTIONS -> options(principal);
            case PROPFIND -> {
                final var pf = parsePropFind(request::getInputStream);
                final var supported = new HashMap<QName, DavProperty>();
                final QName RESOURCETYPE  = new QName(DAV_NS, "resourcetype");
                final QName CURR_PRIV = new QName(DAV_NS, "current-user-privilege-set");
                final QName PRINCIPAL_URL = new QName(DAV_NS, "principal-URL");

                supported.put(RESOURCETYPE, dParent("resourcetype", List.of(dEmpty("principal"))));
                supported.put(CURR_PRIV, CURRENT_USER_PRIVILEGE_SET);
                supported.put(PRINCIPAL_URL, getPrincipalUrlProp(principal));

                final var propStats = buildPropStatsForFixedProps(pf, supported);

                final var davResponse = new DavResponse(principal.getPrincipalURL(), propStats);

                yield CarddavResponse.builder()
                                     .status(Response.Status.SC_MULTI_STATUS)
                                     .header(Response.Header.CONTENT_TYPE, Response.APPLICATION_XML)
                                     .body(renderMultistatus(List.of(davResponse)))
                                     .build();
            }
            default -> getDefaultMethodResponse(request.getMethod(), principal);
        };
    }

    static
    CarddavResponse handleUserPrincipal(CarddavRequest request, UserPrincipal principal) throws BadRequestException {
        return switch (request.getMethod()) {
            case OPTIONS -> options(principal);
            case PROPFIND -> {
                final var pf = parsePropFind(request::getInputStream);
                final var supported = new HashMap<QName, DavProperty>();
                final QName RESOURCETYPE = new QName(DAV_NS, "resourcetype");
                final QName ADDRBOOK_HOME_SET = new QName(CARDDAV_NS, "addressbook-home-set");
                final QName CURR_PRIV = new QName(DAV_NS, "current-user-privilege-set");
                final QName CURR_USER_PRINCIPAL = new QName(DAV_NS, "current-user-principal");
                final QName PRINCIPAL_URL = new QName(DAV_NS, "principal-URL");

                supported.put(RESOURCETYPE, dParent("resourcetype", List.of(dEmpty("principal"))));
                supported.put(ADDRBOOK_HOME_SET, cParent("addressbook-home-set", List.of(dProp("href", principal.getAddress()))));
                supported.put(CURR_PRIV, CURRENT_USER_PRIVILEGE_SET);
                supported.put(CURR_USER_PRINCIPAL, getCurrentUserPrincipalProp(principal));
                supported.put(PRINCIPAL_URL, getPrincipalUrlProp(principal));

                final var propStats = buildPropStatsForFixedProps(pf, supported);
                final var davResponse = new DavResponse(principal.getPrincipalURL(), propStats);

                yield CarddavResponse.builder()
                                     .status(Response.Status.SC_MULTI_STATUS)
                                     .header(Response.Header.CONTENT_TYPE, Response.APPLICATION_XML)
                                     .body(renderMultistatus(List.of(davResponse)))
                                     .build();
            }
            default -> getDefaultMethodResponse(request.getMethod(), principal);
        };
    }
}
