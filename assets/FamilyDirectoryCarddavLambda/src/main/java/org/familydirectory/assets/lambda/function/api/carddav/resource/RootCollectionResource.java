package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import java.util.Date;
import java.util.List;
import org.familydirectory.assets.lambda.function.api.helpers.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;

public final
class RootCollectionResource extends AbstractResource implements CollectionResource {
    /**
     * @see FDResourceFactory
     */
    RootCollectionResource (@NotNull CarddavLambdaHelper carddavLambdaHelper) {
        super(carddavLambdaHelper);
    }

    @Override
    public
    Resource child (String childName) throws NotAuthorizedException, BadRequestException {
        return null;
    }

    @Override
    public
    List<IResource> getChildren () throws NotAuthorizedException, BadRequestException {
        return FDResourceFactory.getInstance(this.carddavLambdaHelper).getResources(IResource.class);
    }

    @Override
    public
    String getName () {
        return "";
    }

    @Override
    public
    Date getModifiedDate () {
        return this.getCreateDate();
    }

    @Override
    public
    String getEtag () {
        return this.getName();
    }
}
