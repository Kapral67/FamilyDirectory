package org.familydirectory.assets.lambda.function.api.carddav.resource;

import com.fasterxml.uuid.impl.UUIDUtil;
import io.milton.common.InternationalizedString;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.values.Pair;
import io.milton.principal.PrincipalSearchCriteria;
import io.milton.resource.AddressBookQuerySearchableResource;
import io.milton.resource.AddressBookResource;
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
import org.familydirectory.assets.lambda.function.api.helpers.CarddavLambdaHelper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static com.fasterxml.uuid.UUIDType.TIME_BASED_EPOCH;
import static java.util.Locale.ENGLISH;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.ADDRESS_BOOK;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SUPPORTED_ADDRESS_DATA;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SYNC_TOKEN_PATH;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.URL;

public final
class FamilyDirectoryResource extends AbstractResource implements AddressBookResource, AddressBookQuerySearchableResource, SyncCollectionResource {

    private boolean isMemberResourcesComplete = false;
    private UUID ctag = null;
    private InternationalizedString description = null;

    private final ResourceFactory resourceFactory;

    /**
     * @see ResourceFactory
     */
    FamilyDirectoryResource (@NotNull CarddavLambdaHelper carddavLambdaHelper) {
        super(carddavLambdaHelper);
        this.resourceFactory = ResourceFactory.getInstance(carddavLambdaHelper);
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
        final var prefetch = this.resourceFactory.getOptionalMemberResource(memberId);
        if (prefetch.isPresent()) {
            return prefetch.get();
        }
        if (this.isMemberResourcesComplete) {
            return this.resourceFactory.getMemberResource(memberId, this.getModifiedDate());
        }
        return Optional.ofNullable(this.carddavLambdaHelper.getDdbItem(uuid, DdbTable.MEMBER))
                       .map(MemberRecord::convertDdbMap)
                       .map(this.resourceFactory::getMemberResource)
                       .orElseGet(() -> this.resourceFactory.getMemberResource(memberId, this.getModifiedDate()));
    }

    @Override
    @NotNull
    @Unmodifiable
    public
    List<IMemberResource> getChildren () {
        if (!this.isMemberResourcesComplete) {
            this.carddavLambdaHelper.scanMemberDdb().forEach(this.resourceFactory::getMemberResource);
            this.isMemberResourcesComplete = true;
        }
        return this.resourceFactory.getMemberResources();
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
    List<MemberResource> getChildren (PrincipalSearchCriteria crit) throws BadRequestException {
        throw new UnsupportedOperationException();
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
        return URI.create(URL + SYNC_TOKEN_PATH + this.getCTag());
    }

    @Override
    public
    Map<String, Resource> findResourcesBySyncToken (URI syncToken) throws BadRequestException {
        try {
            return Map.of();
        } catch (final Exception e) {
            final var toThrow = new BadRequestException(this, "Invalid Sync Token");
            toThrow.initCause(e);
            throw toThrow;
        }
    }
}
