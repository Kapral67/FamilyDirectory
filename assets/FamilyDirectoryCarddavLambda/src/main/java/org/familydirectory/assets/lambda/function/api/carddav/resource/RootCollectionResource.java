package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;

public
class RootCollectionResource extends AbstractResource implements CollectionResource {
    @Override
    public
    Resource child (String childName) throws NotAuthorizedException, BadRequestException {
        return null;
    }
}
