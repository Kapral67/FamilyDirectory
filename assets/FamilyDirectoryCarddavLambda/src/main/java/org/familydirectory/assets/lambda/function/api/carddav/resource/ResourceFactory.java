package org.familydirectory.assets.lambda.function.api.carddav.resource;

import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotFoundException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.lambda.function.api.carddav.principal.AbstractPrincipal;
import org.familydirectory.assets.lambda.function.api.carddav.principal.SystemPrincipal;
import org.familydirectory.assets.lambda.function.api.helpers.CarddavLambdaHelper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import static java.util.Objects.requireNonNull;

public final
class ResourceFactory implements io.milton.http.ResourceFactory {
    private static ResourceFactory INSTANCE = null;

    @NotNull
    private final CarddavLambdaHelper carddavLambdaHelper;
    private final Set<AbstractResourceObject> resources = new HashSet<>();

    private
    ResourceFactory (@NotNull CarddavLambdaHelper carddavLambdaHelper) {
        this.carddavLambdaHelper = requireNonNull(carddavLambdaHelper);
    }

    public static
    ResourceFactory getInstance (@NotNull CarddavLambdaHelper carddavLambdaHelper) {
        if (INSTANCE == null) {
            INSTANCE = new ResourceFactory(carddavLambdaHelper);
        }
        return INSTANCE;
    }

    @Override
    public
    AbstractResourceObject getResource (String host, String path) throws BadRequestException {
        final BadRequestException toThrow = new BadRequestException("Bad Path");
        try {
        } catch (final Exception e) {
            toThrow.initCause(e);
        }
        throw toThrow;
    }

    @NotNull
    public
    IMemberResource getMemberResource (final String name) throws NotFoundException {
       return this.getOptionalMemberResource(UUID.fromString(name)).orElseThrow(() -> new NotFoundException("Member not found"));
    }

    @NotNull
    public
    SystemPrincipal getSystemPrincipal () {
        return getFirstResource(SystemPrincipal.class, SystemPrincipal::new);
    }

    @NotNull
    public
    FamilyDirectoryResource getFamilyDirectoryResource () {
        return getFirstResource(FamilyDirectoryResource.class, FamilyDirectoryResource::new);
    }

    @NotNull
    Optional<IMemberResource> getOptionalMemberResource(@NotNull final UUID name) {
        final var clazz = IMemberResource.class;
        return this.getResourceStream(clazz::isInstance, getNamePredicate(name.toString())).map(clazz::cast).findFirst();
    }

    @NotNull
    IMemberResource getMemberResource(@NotNull final UUID name, final Date modifiedDate) {
        final var potential = this.getResourceStream(IMemberResource.class::isInstance, getNamePredicate(name.toString())).findFirst();
        if (potential.isPresent()) {
            if (potential.get() instanceof DeletedMemberResource deleted) {
                return deleted;
            }
            this.carddavLambdaHelper.getLogger().log("[CONFLICT] Expected Member %s Deleted".formatted(name), LogLevel.WARN);
            return (IMemberResource) potential.get();
        }
        final var deleted = new DeletedMemberResource(name, modifiedDate);
        this.resources.add(deleted);
        return deleted;
    }

    @NotNull
    IMemberResource getMemberResource(@NotNull final MemberRecord memberRecord) {
        final var potential = this.getResourceStream(IMemberResource.class::isInstance, getNamePredicate(memberRecord.id().toString())).findFirst();
        if (potential.isPresent()) {
            if (potential.get() instanceof MemberResource member) {
                return member;
            }
            this.carddavLambdaHelper.getLogger().log("[CONFLICT] Expected Member %s Exists".formatted(memberRecord.id()), LogLevel.WARN);
            return (IMemberResource) potential.get();
        }
        final var member = new MemberResource(this.carddavLambdaHelper,  memberRecord);
        this.resources.add(member);
        return member;
    }

    @NotNull
    @Unmodifiable
    List<IMemberResource> getMemberResources () {
        return this.getResources(IMemberResource.class);
    }

    @NotNull
    @Unmodifiable
    List<AbstractPrincipal> getPrincipals() {
        return this.getResources(AbstractPrincipal.class);
    }

    @NotNull
    @Unmodifiable
    <T extends IResource> List<T> getResources(@NotNull Class<T> clazz) {
        return this.getResourceStream(clazz::isInstance).map(clazz::cast).toList();
    }

    private <T extends AbstractResourceObject> T getFirstResource(@NotNull Class<T> clazz, Function<CarddavLambdaHelper, T> ctor) {
        final var potential = this.getResourceStream(clazz::isInstance).map(clazz::cast).findFirst();
        if (potential.isPresent()) {
            return potential.get();
        } else {
            final var t = ctor.apply(this.carddavLambdaHelper);
            this.resources.add(t);
            return t;
        }
    }

    @SafeVarargs
    private
    Stream<AbstractResourceObject> getResourceStream (Predicate<? super AbstractResourceObject> @NotNull ...predicates) {
        var stream = this.resources.stream();
        for (final var predicate : predicates) {
            stream = stream.filter(predicate);
        }
        return stream;
    }

    @Contract(pure = true)
    @NotNull
    private static
    Predicate<? super AbstractResourceObject> getNamePredicate(final String name) {
        return resource -> resource.getName().equals(name);
    }
}
