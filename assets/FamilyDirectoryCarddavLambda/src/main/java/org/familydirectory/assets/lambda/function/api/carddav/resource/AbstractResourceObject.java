package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.http.Request;
import io.milton.resource.GetableResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.ReportableResource;
import java.util.EnumSet;
import java.util.Set;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

public sealed abstract
class AbstractResourceObject implements IResource permits AbstractResource, DeletedMemberResource {
    @NotNull
    protected final CarddavLambdaHelper carddavLambdaHelper;
    @NotNull
    protected final FDResourceFactory resourceFactory;
    @NotNull
    private final String name;

    protected
    AbstractResourceObject (@NotNull CarddavLambdaHelper carddavLambdaHelper, @NotNull String name) {
        this.carddavLambdaHelper = requireNonNull(carddavLambdaHelper);
        this.resourceFactory = carddavLambdaHelper.getResourceFactory();
        this.name = name;
        this.resourceFactory.registerNewResource(this);
    }

    @Override
    @NotNull
    public final
    String getName() {
        return this.name;
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

    @NotNull
    @UnmodifiableView
    public final
    Set<Request.Method> getAllowedMethods() {
        final var allowedMethods = EnumSet.of(Request.Method.OPTIONS);
        if (this instanceof PropFindableResource) {
            allowedMethods.add(Request.Method.PROPFIND);
        }
        if (this instanceof ReportableResource) {
            allowedMethods.add(Request.Method.REPORT);
        }
        if (this instanceof GetableResource) {
            allowedMethods.add(Request.Method.GET);
            allowedMethods.add(Request.Method.HEAD);
        }
        return unmodifiableSet(allowedMethods);
    }
}
