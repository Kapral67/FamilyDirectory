package org.familydirectory.assets.lambda.function.api.carddav.resource;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.familydirectory.assets.lambda.function.api.carddav.utils.vcf.ContactVCF;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
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
            final var categories = new HashSet<String>();
            categories.add(parent.getDescription()
                                 .getValue());
            carddavLambdaHelper.getFamilyTree()
                               .getRelationships(member)
                               .stream()
                               .map(Relationship::getDisplayLabel)
                               .forEach(categories::add);
            final var vcard = new ContactVCF(member, unmodifiableSet(categories));
            return getBytesUtf8(vcard.toString());
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

    public
    Set<Relationship> getRelationships () {
        return this.carddavLambdaHelper.getFamilyTree()
                                       .getRelationships(this.member);
    }
}
