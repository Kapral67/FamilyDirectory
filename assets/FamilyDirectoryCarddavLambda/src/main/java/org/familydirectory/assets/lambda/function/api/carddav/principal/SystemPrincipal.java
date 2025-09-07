package org.familydirectory.assets.lambda.function.api.carddav.principal;

import org.familydirectory.assets.lambda.function.api.helpers.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SYSTEM_PRINCIPAL;

public final
class SystemPrincipal extends AbstractPrincipal {

    public SystemPrincipal (@NotNull CarddavLambdaHelper helper) {
        super(helper);
    }

    @Override
    public
    String getName () {
        return SYSTEM_PRINCIPAL;
    }

    @Override
    public
    PrincipleId getIdenitifer () {
        return null;
    }
}
