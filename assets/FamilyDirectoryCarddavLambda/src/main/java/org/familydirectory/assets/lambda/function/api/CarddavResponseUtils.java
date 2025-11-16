package org.familydirectory.assets.lambda.function.api;

import io.milton.http.Request;
import io.milton.http.Response;
import org.familydirectory.assets.lambda.function.api.carddav.resource.AbstractResourceObject;
import org.familydirectory.assets.lambda.function.api.carddav.resource.DeletedMemberResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.FamilyDirectoryResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.PresentMemberResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.PrincipalCollectionResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.RootCollectionResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.SystemPrincipal;
import org.familydirectory.assets.lambda.function.api.carddav.resource.UserPrincipal;
import org.familydirectory.assets.lambda.function.api.carddav.response.CarddavResponse;
import static io.milton.http.DateUtils.formatForHeader;
import static java.util.stream.Collectors.joining;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.VCARD_CONTENT_TYPE;

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
    CarddavResponse handleRootCollectionResource(Request.Method method, RootCollectionResource resource) {

    }

    static
    CarddavResponse handlePrincipalCollectionResource(Request.Method method, PrincipalCollectionResource resource) {

    }

    static
    CarddavResponse handleFamilyDirectoryResource(Request.Method method, FamilyDirectoryResource resource) {

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
                // TODO
            }
            default -> method.isWrite
                ? FORBIDDEN
                : methodNotAllowed(resource);
        };
    }

    static
    CarddavResponse handleDeletedMemberResource(Request.Method method, DeletedMemberResource resource) {
        return NOT_FOUND;
    }

    static
    CarddavResponse handleSystemPrincipal(Request.Method method, SystemPrincipal principal) {

    }

    static
    CarddavResponse handleUserPrincipal(Request.Method method, UserPrincipal principal) {

    }
}
