package org.familydirectory.assets.lambda.function.api.carddav.resource;

import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;
import static java.util.Objects.requireNonNull;

public abstract non-sealed
class AbstractResourceObject implements IResource {
    @NotNull
    protected final CarddavLambdaHelper carddavLambdaHelper;
    @NotNull
    protected final FDResourceFactory resourceFactory;

    protected
    AbstractResourceObject (@NotNull CarddavLambdaHelper carddavLambdaHelper) {
        this.carddavLambdaHelper = requireNonNull(carddavLambdaHelper);
        this.resourceFactory = carddavLambdaHelper.getResourceFactory();
        this.resourceFactory.registerNewResource(this);
    }

    @Override
    public final
    boolean equals (final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (this.getClass() != o.getClass()) {
            if (!(this instanceof IMemberResource && o instanceof IMemberResource)) {
                return false;
            }
        }

        return this.getName().equals(((AbstractResourceObject) o).getName());
    }

    @Override
    public final
    int hashCode () {
        return this.getName().hashCode();
    }
}
