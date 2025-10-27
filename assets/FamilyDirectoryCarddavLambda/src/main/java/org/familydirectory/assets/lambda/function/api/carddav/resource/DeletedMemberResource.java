package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.resource.RemovedResource;
import java.util.Date;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import static java.util.Objects.requireNonNull;

public final
class DeletedMemberResource extends AbstractResourceObject implements IMemberResource, RemovedResource {

    @NotNull
    private final UUID id;

    @NotNull
    private final Date modifiedDate;

    /**
     * @see FDResourceFactory
     */
    DeletedMemberResource(@NotNull UUID id, @NotNull Date modifiedDate) {
        this.id = requireNonNull(id);
        this.modifiedDate = requireNonNull(modifiedDate);
    }

    @Override
    public
    String getName () {
        return this.id.toString();
    }

    @Override
    @NotNull
    public
    Date getModifiedDate () {
        return (Date) this.modifiedDate.clone();
    }
}
