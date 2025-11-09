package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.common.Path;
import io.milton.http.ResourceFactory;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.familydirectory.assets.lambda.function.api.helper.ApiHelper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import static java.util.Objects.requireNonNull;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.INITIAL_RESOURCE_CONTAINER_SIZE;

public final
class FDResourceFactory implements ResourceFactory {
    private final Set<AbstractResourceObject> resources = new HashSet<>(INITIAL_RESOURCE_CONTAINER_SIZE);
    private final RootCollectionResource rootCollectionResource;

    public
    FDResourceFactory (@NotNull CarddavLambdaHelper carddavLambdaHelper) throws ApiHelper.ResponseException {
        requireNonNull(carddavLambdaHelper);
        carddavLambdaHelper.registerResourceFactory(this);
        this.rootCollectionResource = new RootCollectionResource(carddavLambdaHelper);
    }

    @Override
    public
    Resource getResource (String host, String sPath) throws BadRequestException, NotAuthorizedException {
        return this.find(Path.path(sPath));
    }

    private Resource find(Path path) throws BadRequestException, NotAuthorizedException {
        if (path.isRoot()) {
            return this.rootCollectionResource;
        }
        final var parent = find(path.getParent());
        if (parent instanceof CollectionResource parentCollection) {
            return parentCollection.child(path.getName());
        }
        throw new BadRequestException(parent, "Unknown Resource: " + path.getName());
    }

    void registerNewResource (AbstractResourceObject resource) {
        if (this.resources.contains(resource)) {
            throw new IllegalStateException("Resource `%s` already exists".formatted(resource.getName()));
        }
        this.resources.add(resource);
    }

    @Contract(pure = true)
    @NotNull
    @UnmodifiableView
    Set<AbstractResourceObject> getResources () {
        return Collections.unmodifiableSet(this.resources);
    }

    RootCollectionResource getRoot() {
        return this.rootCollectionResource;
    }
}
