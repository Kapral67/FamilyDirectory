package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.http.values.HrefList;
import io.milton.principal.DirectoryGatewayCardDavPrincipal;
import io.milton.principal.HrefPrincipleId;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.lambda.function.api.helper.ApiHelper;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.ADDRESS_BOOK_PATH;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.URL;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.PRINCIPALS_COLLECTION_PATH;

public final
class UserPrincipal extends AbstractPrincipal implements DirectoryGatewayCardDavPrincipal {

    @NotNull
    private final MemberRecord user;

    @NotNull
    private final PrincipleId principalId;

    /**
     * @see FDResourceFactory
     */
    UserPrincipal (@NotNull CarddavLambdaHelper carddavLambdaHelper) throws ApiHelper.ResponseException {
        super(carddavLambdaHelper);
        this.user = this.carddavLambdaHelper.getCaller().caller();
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
        return URL + ADDRESS_BOOK_PATH;
    }

    @Override
    @NotNull
    public
    String getPrincipalURL () {
        return URL + PRINCIPALS_COLLECTION_PATH + this.user.id();
    }

    /**
     * @see <a href="https://github.com/miltonio/milton2/issues/25">CardDAV Directory Gateway Extension</a>
     */
    @Override
    @NotNull
    public
    HrefList getDirectoryGateway () {
        return HrefList.asList(this.getAddress());
    }

    @Override
    public
    PrincipleId getIdenitifer () {
        return this.principalId;
    }

    @Override
    public
    String getName () {
        return this.user.id().toString();
    }
}
