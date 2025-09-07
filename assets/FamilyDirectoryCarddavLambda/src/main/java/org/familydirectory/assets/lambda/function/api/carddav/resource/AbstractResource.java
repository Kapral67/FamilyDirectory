package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.http.Auth;
import io.milton.http.values.HrefList;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.ReportableResource;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.familydirectory.assets.lambda.function.api.helpers.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import static java.util.Objects.requireNonNull;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.PRINCIPALS_COLLECTION_PATH;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SUPPORTED_PRIVILEGES;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SYSTEM_PRINCIPAL_PATH;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.URL;

public abstract
class AbstractResource extends AbstractResourceObject implements ReportableResource, PropFindableResource, AccessControlledResource {
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

    @Override
    @Deprecated
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

    @Override
    public final
    HrefList getPrincipalCollectionHrefs () {
        return HrefList.asList(PRINCIPALS_COLLECTION_PATH);
    }

    @Override
    public Date getCreateDate() {
        return Date.from(Instant.EPOCH);
    }

    @Override
    public
    String getPrincipalURL() {
        return URL + SYSTEM_PRINCIPAL_PATH;
    }

    public abstract String getEtag();
}
