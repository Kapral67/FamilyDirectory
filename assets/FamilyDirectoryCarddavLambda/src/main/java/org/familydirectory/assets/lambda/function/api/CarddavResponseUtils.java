package org.familydirectory.assets.lambda.function.api;

import io.milton.http.Request;
import io.milton.http.Response;
import java.net.URI;
import java.util.List;
import org.familydirectory.assets.lambda.function.api.carddav.resource.AbstractResourceObject;
import org.familydirectory.assets.lambda.function.api.carddav.resource.PresentMemberResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.PrincipalCollectionResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.RootCollectionResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.SystemPrincipal;
import org.familydirectory.assets.lambda.function.api.carddav.resource.UserPrincipal;
import org.familydirectory.assets.lambda.function.api.carddav.response.CarddavResponse;
import org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.DavProperty;
import org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.DavResponse;
import static io.milton.http.DateUtils.formatForHeader;
import static io.milton.http.DateUtils.formatForWebDavModifiedDate;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.CURRENT_USER_PRIVILEGE_SET;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.PRINCIPALS_COLLECTION_PATH;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.VCARD_CONTENT_TYPE;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.cParent;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.cProp;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.dEmpty;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.dParent;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.dProp;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.okPropstat;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.renderMultistatus;

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
            // If it's not a valid URI, just use it as-is
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
                              .header(Response.Header.DAV, "1,addressbook,sync-collection")
                              .build();
    }

    static
    DavProperty getCurrentUserPrincipalProp(UserPrincipal principal) {
        return dParent("current-user-principal", singletonList(dProp("href", principal.getPrincipalURL())));
    }

    static
    List<DavProperty> getPresentMemberResourceProps(PresentMemberResource resource) {
        return List.of(
            dProp("getetag", '"' + resource.getEtag() + '"'),
            dProp("getlastmodified", formatForWebDavModifiedDate(resource.getModifiedDate())),
            dEmpty("resourcetype"),
            CURRENT_USER_PRIVILEGE_SET,
            // TODO: optional displayname dProp, in case we need lock emojis
            cProp("address-data", resource.getAddressData(), emptyMap())
        );
    }

    static
    CarddavResponse handleRootCollectionResource(Request.Method method, RootCollectionResource resource) {
        return switch (method) {
            case OPTIONS -> options(resource);
            case PROPFIND -> {
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
                final var props = List.of(
                    dParent("resourcetype", singletonList(dEmpty("collection"))),
                    CURRENT_USER_PRIVILEGE_SET,
                    getCurrentUserPrincipalProp(user)
                );
                final var davResponse = new DavResponse("/", singletonList(okPropstat(props)));
                yield CarddavResponse.builder()
                                     .status(Response.Status.SC_MULTI_STATUS)
                                     .header(Response.Header.CONTENT_TYPE, Response.APPLICATION_XML)
                                     .body(renderMultistatus(singletonList(davResponse)))
                                     .build();
            }
            default -> getDefaultMethodResponse(method, resource);
        };
    }

    static
    CarddavResponse handlePrincipalCollectionResource(Request.Method method, PrincipalCollectionResource resource) {
        return switch (method) {
            case OPTIONS -> options(resource);
            case PROPFIND -> {
                final UserPrincipal user = resource.getChildren()
                                                   .stream()
                                                   .filter(UserPrincipal.class::isInstance)
                                                   .map(UserPrincipal.class::cast)
                                                   .findAny()
                                                   .orElseThrow();
                final var props = List.of(
                    dParent("resourcetype", singletonList(dEmpty("collection"))),
                    CURRENT_USER_PRIVILEGE_SET,
                    getCurrentUserPrincipalProp(user)
                );
                final var davResponse = new DavResponse(PRINCIPALS_COLLECTION_PATH, singletonList(okPropstat(props)));
                yield CarddavResponse.builder()
                                     .status(Response.Status.SC_MULTI_STATUS)
                                     .header(Response.Header.CONTENT_TYPE, Response.APPLICATION_XML)
                                     .body(renderMultistatus(singletonList(davResponse)))
                                     .build();
            }
            default -> getDefaultMethodResponse(method, resource);
        };
    }

    static
    CarddavResponse handlePresentMemberResource(Request.Method method, PresentMemberResource resource) {
        return switch (method) {
            case OPTIONS -> options(resource);
            case GET, HEAD -> {
                final var responseBuilder = CarddavResponse.builder()
                                                           .status(Response.Status.SC_OK)
                                                           .header(Response.Header.CONTENT_TYPE, VCARD_CONTENT_TYPE)
                                                           .header(Response.Header.CONTENT_LENGTH, resource.getContentLength().toString())
                                                           .header(Response.Header.ETAG, '"' + resource.getEtag() + '"')
                                                           .header(Response.Header.LAST_MODIFIED, formatForHeader(resource.getModifiedDate()));
                if (Request.Method.GET.equals(method)) {
                    responseBuilder.body(resource.getAddressData());
                }
                yield responseBuilder.build();
            }
            case PROPFIND -> {
                // No request parsing, just return full prop set
                final var props = getPresentMemberResourceProps(resource);
                final var davResponse = new DavResponse(resource.getHref(), singletonList(okPropstat(props)));
                yield CarddavResponse.builder()
                                     .status(Response.Status.SC_MULTI_STATUS)
                                     .header(Response.Header.CONTENT_TYPE, Response.APPLICATION_XML)
                                     .body(renderMultistatus(singletonList(davResponse)))
                                     .build();
            }
            default -> getDefaultMethodResponse(method, resource);
        };
    }

    static
    CarddavResponse handleDeletedMemberResource() {
        return NOT_FOUND;
    }

    static
    CarddavResponse handleSystemPrincipal(Request.Method method, SystemPrincipal principal) {
        return switch (method) {
            case OPTIONS -> options(principal);
            case PROPFIND -> {
                final var props = List.of(
                    dParent("resourcetype", singletonList(dEmpty("principal"))),
                    CURRENT_USER_PRIVILEGE_SET
                );
                final var davResponse = new DavResponse(principal.getPrincipalURL(), singletonList(okPropstat(props)));
                yield CarddavResponse.builder()
                                     .status(Response.Status.SC_MULTI_STATUS)
                                     .header(Response.Header.CONTENT_TYPE, Response.APPLICATION_XML)
                                     .body(renderMultistatus(singletonList(davResponse)))
                                     .build();
            }
            default -> getDefaultMethodResponse(method, principal);
        };
    }

    static
    CarddavResponse handleUserPrincipal(Request.Method method, UserPrincipal principal) {
        return switch (method) {
            case OPTIONS -> options(principal);
            case PROPFIND -> {
                final var props = List.of(
                    dParent("resourcetype", singletonList(dEmpty("principal"))),
                    cParent("addressbook-home-set", singletonList(dProp("href", principal.getAddress()))),
                    CURRENT_USER_PRIVILEGE_SET
                );
                final var davResponse = new DavResponse(principal.getPrincipalURL(), singletonList(okPropstat(props)));
                yield CarddavResponse.builder()
                                     .status(Response.Status.SC_MULTI_STATUS)
                                     .header(Response.Header.CONTENT_TYPE, Response.APPLICATION_XML)
                                     .body(renderMultistatus(singletonList(davResponse)))
                                     .build();
            }
            default -> getDefaultMethodResponse(method, principal);
        };
    }
}
