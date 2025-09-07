package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.resource.Resource;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SUPPORTED_METHODS;

/**
 * Sane Defaults
 */
public sealed
interface IResource extends Resource permits IMemberResource, AbstractResourceObject {
    /**
     * @see #getName()
     */
    @Override
    @Deprecated
    default
    String getUniqueId () {
        return null;
    }

    @Override
    @Deprecated
    default
    Object authenticate(String user, String password) {
        return true;
    }

    @Override
    @Deprecated
    default
    String getRealm() {
        return null;
    }

    @Override
    @Deprecated
    default
    String checkRedirect (Request request) {
        return null;
    }

    @Override
    default
    boolean authorise (Request request, Request.Method method, Auth auth) {
        return SUPPORTED_METHODS.contains(method);
    }
}
