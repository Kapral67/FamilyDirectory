package org.familydirectory.assets.lambda.function.api.carddav.resource;

import java.util.Date;
import java.util.HashSet;
import org.familydirectory.assets.ddb.member.Vcard;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.familydirectory.assets.lambda.function.api.graph.FamilyGraphUtils;
import org.familydirectory.assets.lambda.function.api.graph.Relationship;
import org.jetbrains.annotations.NotNull;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.unmodifiableSet;
import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;

public final
class PresentMemberResource extends AbstractVcardResource {
    @NotNull
    private final MemberRecord member;

    /**
     * @see FDResourceFactory
     */
    PresentMemberResource (final @NotNull CarddavLambdaHelper carddavLambdaHelper, final @NotNull MemberRecord member) {
        super(carddavLambdaHelper, member.id().toString(), () -> {
            final var parent = getParent(carddavLambdaHelper);
            final var caller = getCaller(carddavLambdaHelper).caller();
            final var categories = new HashSet<String>();
            categories.add(parent.getDescription()
                                 .getValue());
            FamilyGraphUtils.getRelationships(carddavLambdaHelper.getFamilyGraph(), caller, member)
                            .stream()
                            .map(Relationship::getDisplayLabel)
                            .forEach(categories::add);
            final var _vcard = new Vcard(member, unmodifiableSet(categories));
            return getBytesUtf8(_vcard.toString());
        });
        this.member = member;
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
}
