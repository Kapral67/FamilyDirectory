package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.http.values.HrefList;
import io.milton.principal.CardDavPrincipal;
import io.milton.principal.HrefPrincipleId;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.ADDRESS_BOOK_PATH;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.PRINCIPALS_COLLECTION_PATH;

public final
class UserPrincipal extends AbstractPrincipal implements CardDavPrincipal {

    @NotNull
    private final PrincipleId principalId;

    /**
     * @see FDResourceFactory
     */
    UserPrincipal (@NotNull CarddavLambdaHelper carddavLambdaHelper) {
        super(carddavLambdaHelper, carddavLambdaHelper.getCaller().caller().id().toString());
        this.principalId = new HrefPrincipleId(this.getPrincipalURL());
    }

    @Override
    @NotNull
    public
    HrefList getAddressBookHomeSet () {
        return HrefList.asList(this.getAddress());
    }

    @Override
    @NotNull
    public
    String getAddress () {
        return ADDRESS_BOOK_PATH;
    }

    @Override
    @NotNull
    public
    String getPrincipalURL () {
        return PRINCIPALS_COLLECTION_PATH + this.getName();
    }

    @Override
    public
    PrincipleId getIdenitifer () {
        return this.principalId;
    }
}
