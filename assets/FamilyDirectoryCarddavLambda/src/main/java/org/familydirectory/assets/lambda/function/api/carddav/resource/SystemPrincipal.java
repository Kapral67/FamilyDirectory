package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.principal.HrefPrincipleId;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SYSTEM_PRINCIPAL;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SYSTEM_PRINCIPAL_PATH;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.URL;

public final
class SystemPrincipal extends AbstractPrincipal {

    private final PrincipleId principalId;

    /**
     * @see FDResourceFactory
     */
    SystemPrincipal (@NotNull CarddavLambdaHelper helper) {
        super(helper);
        this.principalId = new HrefPrincipleId(URL + SYSTEM_PRINCIPAL_PATH);
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
