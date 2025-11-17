package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.resource.AddressResource;
import io.milton.resource.GetableResource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import org.familydirectory.assets.ddb.member.Vcard;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;
import static java.time.ZoneOffset.UTC;
import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;
import static org.apache.commons.codec.binary.StringUtils.newStringUtf8;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.VCARD_CONTENT_TYPE;

public final
class PresentMemberResource extends AbstractResource implements IMemberResource, GetableResource, AddressResource {

    @NotNull
    private final MemberRecord member;
    private final byte[] vcard;

    /**
     * @see FDResourceFactory
     */
    PresentMemberResource (@NotNull CarddavLambdaHelper carddavLambdaHelper, @NotNull MemberRecord member) {
        super(carddavLambdaHelper, member.id().toString());
        this.member = member;
        this.vcard = getBytesUtf8(new Vcard(this.member).toString());
    }

    @Override
    @NotNull
    public
    Date getModifiedDate () {
        return Date.from(this.member.member().getLastModified());
    }

    @Override
    public
    Date getCreateDate () {
        return Date.from(this.member.member()
                                    .getBirthday()
                                    .atStartOfDay(UTC)
                                    .toInstant());
    }

    @Override
    public
    void sendContent (@NotNull OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException {
        out.write(vcard);
    }

    @Override
    public
    Long getMaxAgeSeconds (Auth auth) {
        return null;
    }

    @Override
    public
    String getContentType (String accepts) {
        return VCARD_CONTENT_TYPE;
    }

    @Override
    public
    Long getContentLength () {
        return (long) this.vcard.length;
    }

    @Override
    public
    String getEtag () {
        return this.member.member().getEtag();
    }

    @Override
    public
    String getAddressData () {
        return newStringUtf8(this.vcard);
    }
}
