package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.ReportableResource;
import java.util.List;
import java.util.Map;
import org.familydirectory.assets.lambda.function.api.helpers.CarddavLambdaHelper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SUPPORTED_METHODS;

public abstract
class AbstractResource implements GetableResource, ReportableResource, PropFindableResource, AccessControlledResource {
    @NotNull
    protected final CarddavLambdaHelper carddavLambdaHelper;

    protected
    AbstractResource (@NotNull CarddavLambdaHelper carddavLambdaHelper) {
        this.carddavLambdaHelper = requireNonNull(carddavLambdaHelper);
    }

    @Contract(value = "_ -> new", pure = true)
    @Override
    @Unmodifiable
    @NotNull
    public final
    List<Priviledge> getPriviledges (Auth auth) {
        return singletonList(Priviledge.READ);
    }

    @Contract(pure = true)
    @Override
    @Nullable
    public final
    Map<Principal, List<Priviledge>> getAccessControlList () {
        return null;
    }

    @Override
    public final
    void setAccessControlList (Map<Principal, List<Priviledge>> privs) {}

    @Contract(pure = true)
    @Override
    @Nullable
    public final
    Object authenticate (String user, String password) {
        return null;
    }

    @Override
    public final
    boolean authorise (Request request, Request.Method method, Auth auth) {
        return SUPPORTED_METHODS.contains(method);
    }
}
