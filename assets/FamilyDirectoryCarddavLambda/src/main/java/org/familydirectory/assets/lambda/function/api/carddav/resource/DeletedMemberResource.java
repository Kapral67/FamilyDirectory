package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.resource.RemovedResource;
import java.util.Date;
import java.util.UUID;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;
import static java.util.Objects.requireNonNull;

public final
class DeletedMemberResource extends AbstractResourceObject implements IMemberResource, RemovedResource {

    @NotNull
    private final Date modifiedDate;

    /**
     * @see FDResourceFactory
     */
    DeletedMemberResource(@NotNull CarddavLambdaHelper carddavLambdaHelper, @NotNull UUID id, @NotNull Date modifiedDate) {
        super(carddavLambdaHelper, id.toString());
        this.modifiedDate = requireNonNull(modifiedDate);
    }

    @Override
    @NotNull
    public
    Date getModifiedDate () {
        return (Date) this.modifiedDate.clone();
    }
}
