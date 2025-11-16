package org.familydirectory.assets.lambda.function.api.carddav.resource;

import com.fasterxml.uuid.impl.UUIDUtil;
import io.milton.common.InternationalizedString;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.values.Pair;
import io.milton.principal.PrincipalSearchCriteria;
import io.milton.resource.AddressBookQuerySearchableResource;
import io.milton.resource.AddressBookResource;
import io.milton.resource.ReportableResource;
import io.milton.resource.Resource;
import io.milton.resource.SyncCollectionResource;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.sync.SyncTableParameter;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.assets.lambda.function.api.CarddavLambdaHelper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static com.fasterxml.uuid.UUIDType.TIME_BASED_EPOCH;
import static java.util.Collections.emptyMap;
import static java.util.Locale.ENGLISH;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.ADDRESS_BOOK;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SUPPORTED_ADDRESS_DATA;

public final
class FamilyDirectoryResource extends AbstractResource implements AddressBookResource, AddressBookQuerySearchableResource, SyncCollectionResource, ReportableResource {

    private boolean isMemberResourcesComplete = false;
    private UUID ctag = null;
    private InternationalizedString description = null;

    /**
     * @see FDResourceFactory
     */
    FamilyDirectoryResource (@NotNull CarddavLambdaHelper carddavLambdaHelper) {
        super(carddavLambdaHelper);
    }

    @Override
    @NotNull
    public
    String getEtag () {
        return this.getCTag();
    }

    @Override
    @NotNull
    public
    IMemberResource child (String uuid) {
        final UUID memberId = UUID.fromString(uuid);
        final var prefetch = this.resourceFactory.getResources()
                                                 .stream()
                                                 .filter(IMemberResource.class::isInstance)
                                                 .map(IMemberResource.class::cast)
                                                 .filter(memberResource -> memberResource.getName().equals(uuid))
                                                 .findAny();
        if (prefetch.isPresent()) {
            return prefetch.get();
        }
        if (this.isMemberResourcesComplete) {
            return new DeletedMemberResource(this.carddavLambdaHelper, memberId, this.getModifiedDate());
        }
        return Optional.ofNullable(this.carddavLambdaHelper.getDdbItem(uuid, DdbTable.MEMBER))
                       .map(MemberRecord::convertDdbMap)
                       .map(memberRecord -> (IMemberResource) new PresentMemberResource(this.carddavLambdaHelper, memberRecord))
                       .orElse(new DeletedMemberResource(this.carddavLambdaHelper, memberId, this.getModifiedDate()));
    }

    @Override
    @NotNull
    @Unmodifiable
    public
    List<IMemberResource> getChildren () {
        if (!this.isMemberResourcesComplete) {
            final var existingResources = this.resourceFactory.getResources()
                                                              .stream()
                                                              .filter(PresentMemberResource.class::isInstance)
                                                              .map(PresentMemberResource.class::cast)
                                                              .map(PresentMemberResource::getName)
                                                              .collect(toUnmodifiableSet());
            this.carddavLambdaHelper.scanMemberDdb()
                                    .stream()
                                    .filter(memberRecord -> !existingResources.contains(memberRecord.id().toString()))
                                    .forEach(memberRecord -> new PresentMemberResource(this.carddavLambdaHelper, memberRecord));
            this.isMemberResourcesComplete = true;
        }
        return this.resourceFactory.getResources()
                                   .stream()
                                   .filter(IMemberResource.class::isInstance)
                                   .map(IMemberResource.class::cast)
                                   .toList();
    }

    @Override
    @NotNull
    public
    String getCTag () {
        if (this.ctag != null) return this.ctag.toString();
        this.ctag = Optional.ofNullable(this.carddavLambdaHelper.getDdbItem(DdbUtils.SYNC_TOKEN_LATEST.toString(), DdbTable.SYNC))
                            .map(map -> map.get(SyncTableParameter.NEXT.toString()))
                            .map(AttributeValue::s)
                            .map(UUID::fromString)
                            .filter(ctag -> TIME_BASED_EPOCH.equals(UUIDUtil.typeOf(ctag)))
                            .orElseThrow();
        return this.ctag.toString();
    }

    @Override
    public
    InternationalizedString getDescription () {
        if (this.description != null) return this.description;
        final String description = "%s Family Directory".formatted(this.carddavLambdaHelper.getRootMemberSurname());
        this.description = new InternationalizedString(ENGLISH.getLanguage(), description);
        return this.description;
    }

    @Override
    @Deprecated
    public
    void setDescription (InternationalizedString description) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Unmodifiable
    @NotNull
    public
    List<Pair<String, String>> getSupportedAddressData () {
        return SUPPORTED_ADDRESS_DATA;
    }

    @Override
    @NotNull
    public
    Long getMaxResourceSize () {
        return 0L;
    }

    @Override
    public
    List<IMemberResource> getChildren (PrincipalSearchCriteria crit) {
        // We return all children to any addressbook-query request
        // to prefer clients perform filtering locally
        return this.getChildren();
    }

    @Override
    public
    String getName () {
        return ADDRESS_BOOK;
    }

    @Override
    @NotNull
    public
    Date getModifiedDate () {
        if (this.ctag == null) this.getCTag();
        return new Date(UUIDUtil.extractTimestamp(this.ctag));
    }

    @Contract(" -> new")
    @Override
    @NotNull
    public
    URI getSyncToken () {
        return URI.create(this.getCTag());
    }

    /**
     * @throws CarddavLambdaHelper.NoSuchTokenException when sync token has been ttl'd
     */
    @Override
    public
    Map<String, Resource> findResourcesBySyncToken (URI syncTokenUri) throws BadRequestException {
        try {
            if (this.getSyncToken().equals(syncTokenUri)) {
                return emptyMap();
            }
            final var syncToken = UUID.fromString(syncTokenUri.toString());
            return this.carddavLambdaHelper.traverseSyncDdb(syncToken)
                                           .stream()
                                           .map(UUID::toString)
                                           .map(this::child)
                                           .collect(toUnmodifiableMap(IMemberResource::getHref, identity()));
        } catch (final CarddavLambdaHelper.NoSuchTokenException e) {
            throw e;
        } catch (final Exception e) {
            throw (BadRequestException) new BadRequestException(this, "Invalid Sync Token").initCause(e);
        }
    }
}
