package org.familydirectory.assets.lambda.function.api.carddav.resource;

import java.time.Instant;
import java.util.Date;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.familydirectory.assets.lambda.function.api.carddav.utils.vcf.GroupVCF;
import org.familydirectory.assets.lambda.function.api.graph.Relationship;
import org.jetbrains.annotations.NotNull;
import static java.time.Clock.systemUTC;
import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;

public final
class KindResource extends AbstractVcardResource {
    private final Relationship relationship;

    /**
     * @see FDResourceFactory
     */
    KindResource(final @NotNull CarddavLambdaHelper carddavLambdaHelper, final @NotNull Relationship relationship) {
        super(carddavLambdaHelper, relationship.name(), () -> {
            carddavLambdaHelper.getFamilyTree()
                               .getRelatives(relationship);
            final var vcard = new GroupVCF();
            return getBytesUtf8(vcard.toString());
        });
        this.relationship = relationship;
    }

    @Override
    public
    Date getModifiedDate () {
        return Date.from(Instant.now(systemUTC()));
    }
}
