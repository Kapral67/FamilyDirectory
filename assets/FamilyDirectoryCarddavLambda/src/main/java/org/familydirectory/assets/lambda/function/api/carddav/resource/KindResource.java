package org.familydirectory.assets.lambda.function.api.carddav.resource;

import java.time.Instant;
import java.util.Date;
import java.util.Set;
import org.familydirectory.assets.ddb.models.member.IMemberRecord;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.familydirectory.assets.lambda.function.api.carddav.utils.vcf.GroupVCF;
import org.familydirectory.assets.lambda.function.api.graph.Relationship;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.time.Clock.systemUTC;
import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;

public final
class KindResource extends AbstractVcardResource {

    static @Nullable
    KindResource create(final @NotNull CarddavLambdaHelper carddavLambdaHelper, final @NotNull Relationship relationship) {
        final var relatives = carddavLambdaHelper.getFamilyTree().getRelatives(relationship);
        if (relatives.isEmpty()) {
            return null;
        }
        return new KindResource(carddavLambdaHelper, relationship, relatives);
    }

    /**
     * @see FDResourceFactory
     */
    private KindResource(
        final @NotNull CarddavLambdaHelper carddavLambdaHelper,
        final @NotNull Relationship relationship,
        final @NotNull Set<IMemberRecord> relatives
    ) {
        super(carddavLambdaHelper, relationship.name(), () -> {
            final var vcard = new GroupVCF(relationship.name(), relationship.getDisplayLabel(), relatives);
            return getBytesUtf8(vcard.toString());
        });
    }

    @Override
    public
    Date getModifiedDate () {
        return Date.from(Instant.now(systemUTC()));
    }
}
