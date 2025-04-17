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
import static java.util.Objects.requireNonNull;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SUPPORTED_METHODS;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SUPPORTED_PRIVILEGES;

public abstract
class AbstractResource implements GetableResource, ReportableResource, PropFindableResource, AccessControlledResource {
    @NotNull
    protected final CarddavLambdaHelper carddavLambdaHelper;

    protected
    AbstractResource (@NotNull CarddavLambdaHelper carddavLambdaHelper) {
        this.carddavLambdaHelper = requireNonNull(carddavLambdaHelper);
    }

    @Override
    @Unmodifiable
    @NotNull
    public final
    List<Priviledge> getPriviledges (Auth auth) {
        return SUPPORTED_PRIVILEGES;
    }

    @Contract(pure = true)
    @Override
    @Nullable
    public final
    Map<Principal, List<Priviledge>> getAccessControlList () {
        return null;
    }

    @Override
    @Deprecated
    public final
    void setAccessControlList (Map<Principal, List<Priviledge>> privs) {
        throw new UnsupportedOperationException();
    }

    @Contract(pure = true)
    @Override
    @NotNull
    public final
    Object authenticate (String user, String password) {
        return true;
    }

    @Override
    public final
    boolean authorise (Request request, Request.Method method, Auth auth) {
        return SUPPORTED_METHODS.contains(method);
    }
}
