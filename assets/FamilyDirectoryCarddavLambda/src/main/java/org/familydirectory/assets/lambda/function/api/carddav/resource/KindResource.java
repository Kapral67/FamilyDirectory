package org.familydirectory.assets.lambda.function.api.carddav.resource;

import java.time.Instant;
import java.util.Date;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.familydirectory.assets.lambda.function.api.graph.Relationship;
import org.jetbrains.annotations.NotNull;
import static java.time.Clock.systemUTC;

public final
class KindResource extends AbstractVcardResource {
    private final Relationship relationship;

    /**
     * @see FDResourceFactory
     */
    KindResource(final @NotNull CarddavLambdaHelper carddavLambdaHelper, final @NotNull Relationship relationship) {
        super(carddavLambdaHelper, relationship.name(), () -> {
            final var caller = getCaller(carddavLambdaHelper).caller();
            return null;
        });
        this.relationship = relationship;
    }

    @Override
    public
    Date getModifiedDate () {
        return Date.from(Instant.now(systemUTC()));
    }
}
