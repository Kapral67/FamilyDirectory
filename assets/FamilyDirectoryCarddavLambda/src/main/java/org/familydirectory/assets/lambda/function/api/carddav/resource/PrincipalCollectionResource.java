package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import java.util.Date;
import java.util.List;

public final
class PrincipalCollectionResource implements CollectionResource {
    @Override
    public
    Resource child (String childName) throws NotAuthorizedException, BadRequestException {
        return null;
    }

    @Override
    public
    List<? extends Resource> getChildren () throws NotAuthorizedException, BadRequestException {
        return List.of();
    }

    @Override
    public
    String getUniqueId () {
        return "";
    }

    @Override
    public
    String getName () {
        return "";
    }

    @Override
    public
    Object authenticate (String user, String password) {
        return null;
    }

    @Override
    public
    boolean authorise (Request request, Request.Method method, Auth auth) {
        return false;
    }

    @Override
    public
    String getRealm () {
        return "";
    }

    @Override
    public
    Date getModifiedDate () {
        return null;
    }

    @Override
    public
    String checkRedirect (Request request) throws NotAuthorizedException, BadRequestException {
        return "";
    }
}
