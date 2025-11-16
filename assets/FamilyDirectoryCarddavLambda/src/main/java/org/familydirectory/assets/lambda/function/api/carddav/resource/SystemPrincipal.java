package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.principal.HrefPrincipleId;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SYSTEM_PRINCIPAL;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SYSTEM_PRINCIPAL_PATH;

public final
class SystemPrincipal extends AbstractPrincipal {

    private final PrincipleId principalId;

    /**
     * @see FDResourceFactory
     */
    SystemPrincipal (@NotNull CarddavLambdaHelper helper) {
        super(helper);
        this.principalId = new HrefPrincipleId(SYSTEM_PRINCIPAL_PATH);
    }

    @Override
    public
    String getName () {
        return SYSTEM_PRINCIPAL;
    }

    @Override
    public
    PrincipleId getIdenitifer () {
        return this.principalId;
    }
}
