package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.http.Auth;
import io.milton.http.values.HrefList;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.PropFindableResource;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.PRINCIPALS_COLLECTION_PATH;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SUPPORTED_PRIVILEGES;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SYSTEM_PRINCIPAL_PATH;

public sealed abstract
class AbstractResource extends AbstractResourceObject implements PropFindableResource, AccessControlledResource
    permits RootCollectionResource, PrincipalCollectionResource, FamilyDirectoryResource, AbstractPrincipal, PresentMemberResource
{
    protected
    AbstractResource (@NotNull CarddavLambdaHelper carddavLambdaHelper) {
        super(carddavLambdaHelper);
    }

    @Override
    @Unmodifiable
    @NotNull
    public final
    List<Priviledge> getPriviledges (Auth auth) {
        return SUPPORTED_PRIVILEGES;
    }

    @Override
    public final
    Map<Principal, List<Priviledge>> getAccessControlList () {
        return this.resourceFactory.getRoot()
                                   .getChildren()
                                   .stream()
                                   .filter(PrincipalCollectionResource.class::isInstance)
                                   .map(PrincipalCollectionResource.class::cast)
                                   .map(PrincipalCollectionResource::getChildren)
                                   .flatMap(List::stream)
                                   .collect(toUnmodifiableMap(identity(), p -> p.getPriviledges(null)));
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
        return SYSTEM_PRINCIPAL_PATH;
    }

    public abstract String getEtag();
}
