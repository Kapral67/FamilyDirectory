package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.resource.AddressResource;
import io.milton.resource.GetableResource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.function.Supplier;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.familydirectory.assets.lambda.function.api.helper.ApiHelper;
import org.jetbrains.annotations.NotNull;
import static org.apache.commons.codec.binary.StringUtils.newStringUtf8;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.apache.commons.lang3.exception.ExceptionUtils.asRuntimeException;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.VCARD_CONTENT_TYPE;

public sealed abstract
class AbstractVcardResource extends AbstractResource implements IMemberResource, GetableResource, AddressResource
    permits PresentMemberResource, KindResource
{
    protected final byte[] vcard;

    /**
     * @see FDResourceFactory
     */
    AbstractVcardResource (@NotNull CarddavLambdaHelper carddavLambdaHelper, @NotNull String name, @NotNull Supplier<byte[]> vcardSupplier) {
        super(carddavLambdaHelper, name);
        this.vcard = vcardSupplier.get();
    }

    protected static FamilyDirectoryResource getParent(CarddavLambdaHelper carddavLambdaHelper) {
        return carddavLambdaHelper.getResourceFactory().getResources()
                                  .stream()
                                  .filter(FamilyDirectoryResource.class::isInstance)
                                  .map(FamilyDirectoryResource.class::cast)
                                  .findAny()
                                  .orElseThrow();
    }

    protected static ApiHelper.Caller getCaller(CarddavLambdaHelper carddavLambdaHelper) {
        try {
            return carddavLambdaHelper.getCaller();
        } catch (ApiHelper.ResponseException e) {
            throw asRuntimeException(e);
        }
    }

    @Override
    public final
    void sendContent (@NotNull OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException {
        out.write(vcard);
    }

    @Override
    public final
    Long getMaxAgeSeconds (Auth auth) {
        return null;
    }

    @Override
    public final
    String getContentType (String accepts) {
        return VCARD_CONTENT_TYPE;
    }

    @Override
    public final
    Long getContentLength () {
        return (long) this.vcard.length;
    }

    @Override
    public final
    String getEtag () {
        return sha256Hex(this.vcard);
    }

    @Override
    public final
    String getAddressData () {
        return newStringUtf8(this.vcard);
    }
}
