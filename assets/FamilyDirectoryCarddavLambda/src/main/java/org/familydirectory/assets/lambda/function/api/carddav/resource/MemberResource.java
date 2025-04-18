package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.values.HrefList;
import io.milton.resource.GetableResource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import org.familydirectory.assets.ddb.member.Vcard;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.lambda.function.api.helpers.CarddavLambdaHelper;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import static java.lang.System.getenv;
import static java.time.ZoneOffset.UTC;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.PRINCIPALS_COLLECTION_PATH;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.VCARD_CONTENT_TYPE;

public final
class MemberResource extends AbstractResource implements GetableResource {

    @NotNull
    private final MemberRecord member;
    private final byte[] vcard;

    public
    MemberResource (@NotNull CarddavLambdaHelper carddavLambdaHelper, @NotNull MemberRecord member) {
        super(carddavLambdaHelper);
        this.member = requireNonNull(member);
        this.vcard = getBytesUtf8(new Vcard(this.member).toString());
    }

    @Override
    public
    String getUniqueId () {
        return this.member.id().toString();
    }

    @Override
    public
    String getName () {
        return this.getUniqueId();
    }

    @Override
    @NotNull
    public
    Date getModifiedDate () {
        return Date.from(this.member.member().getLastModified());
    }

    @Override
    public
    String checkRedirect (Request request) {
        return null;
    }

    @Override
    public
    boolean equals (final Object o) {
        if (this == o) {
            return true;
        } else if (isNull(o) || !this.getClass().equals(o.getClass())) {
            return false;
        }
        return this.member.equals(((MemberResource) o).member);
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
    String getPrincipalURL() {
        return "https://carddav." + requireNonNull(getenv(LambdaUtils.EnvVar.HOSTED_ZONE_NAME.name())) + PRINCIPALS_COLLECTION_PATH + this.member.id();
    }

    @Override
    public
    String getEtag () {
        return this.member.member().getEftag();
    }
}
