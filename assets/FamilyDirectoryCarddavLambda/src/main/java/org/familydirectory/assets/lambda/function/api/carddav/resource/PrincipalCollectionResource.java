package org.familydirectory.assets.lambda.function.api.carddav.resource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.milton.http.exceptions.BadRequestException;
import io.milton.resource.CollectionResource;
import java.util.Date;
import java.util.List;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.PRINCIPALS;

public final
class PrincipalCollectionResource extends AbstractResource implements CollectionResource {
    private final SystemPrincipal systemPrincipal;
    private final UserPrincipal userPrincipal;

    /**
     * @see FDResourceFactory
     */
    PrincipalCollectionResource(@NotNull CarddavLambdaHelper carddavLambdaHelper) {
        super(carddavLambdaHelper, PRINCIPALS);
        this.systemPrincipal = new SystemPrincipal(carddavLambdaHelper);
        this.userPrincipal = new UserPrincipal(carddavLambdaHelper);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    @Override
    public
    AbstractPrincipal child (String name) throws BadRequestException {
        if (systemPrincipal.getName().equals(name)) {
            return systemPrincipal;
        }
        if (userPrincipal.getName().equals(name)) {
            return userPrincipal;
        }
        throw new BadRequestException(this, "Unknown principal: " + name);
    }

    @Override
    public
    List<AbstractPrincipal> getChildren () {
        return List.of(systemPrincipal, userPrincipal);
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
