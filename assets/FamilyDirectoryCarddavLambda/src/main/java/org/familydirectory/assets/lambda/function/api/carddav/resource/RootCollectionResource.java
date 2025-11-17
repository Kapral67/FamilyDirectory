package org.familydirectory.assets.lambda.function.api.carddav.resource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.milton.http.exceptions.BadRequestException;
import io.milton.resource.CollectionResource;
import java.util.Date;
import java.util.List;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.familydirectory.assets.lambda.function.api.helper.ApiHelper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public final
class RootCollectionResource extends AbstractResource implements CollectionResource {
    private final PrincipalCollectionResource principalsCollection;
    private final FamilyDirectoryResource membersCollection;

    /**
     * @see FDResourceFactory
     */
    RootCollectionResource (@NotNull CarddavLambdaHelper carddavLambdaHelper) throws ApiHelper.ResponseException {
        super(carddavLambdaHelper, "");
        this.principalsCollection = new PrincipalCollectionResource(carddavLambdaHelper);
        this.membersCollection = new FamilyDirectoryResource(carddavLambdaHelper);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    @Override
    public
    AbstractResource child (String childName) throws BadRequestException {
        if (principalsCollection.getName().equals(childName)) {
            return principalsCollection;
        }
        if (membersCollection.getName().equals(childName)) {
            return membersCollection;
        }
        throw new BadRequestException(this, "Unknown Resource" + childName);
    }

    @Override
    @Contract(value = " -> new", pure = true)
    @NotNull
    @Unmodifiable
    public
    List<AbstractResource> getChildren () {
        return List.of(principalsCollection, membersCollection);
    }

    @Override
    public
    Date getModifiedDate () {
        return this.getCreateDate();
    }

    @Override
    public
    String getEtag () {
        return this.getName();
    }
}
