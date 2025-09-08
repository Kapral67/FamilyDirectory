package org.familydirectory.assets.lambda.function.api.carddav.principal;

import io.milton.principal.DiscretePrincipal;
import java.util.Date;
import org.familydirectory.assets.lambda.function.api.carddav.resource.AbstractResource;
import org.familydirectory.assets.lambda.function.api.helpers.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;

public sealed abstract
class AbstractPrincipal extends AbstractResource implements DiscretePrincipal permits SystemPrincipal, UserPrincipal {
    protected
    AbstractPrincipal (@NotNull CarddavLambdaHelper carddavLambdaHelper) {
        super(carddavLambdaHelper);
    }

    @Override
    public final
    String getEtag () {
        return this.getName();
    }

    @Override
    public final
    Date getModifiedDate () {
        return this.getCreateDate();
    }
}
