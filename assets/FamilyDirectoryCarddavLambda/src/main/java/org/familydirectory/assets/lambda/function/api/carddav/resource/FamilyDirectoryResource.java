package org.familydirectory.assets.lambda.function.api.carddav.resource;

import com.fasterxml.uuid.impl.UUIDUtil;
import io.milton.common.InternationalizedString;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.values.Pair;
import io.milton.principal.PrincipalSearchCriteria;
import io.milton.resource.AddressBookQuerySearchableResource;
import io.milton.resource.AddressBookResource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.sync.SyncTableParameter;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.assets.lambda.function.api.helpers.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static com.fasterxml.uuid.UUIDType.TIME_BASED_EPOCH;
import static java.util.Locale.ENGLISH;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SUPPORTED_ADDRESS_DATA;

public final
class FamilyDirectoryResource extends AbstractResource implements AddressBookResource, AddressBookQuerySearchableResource {

    private Map<UUID, MemberResource> memberResources = new HashMap<>();
    private boolean isMemberResourcesComplete = false;

    private UUID ctag = null;

    private InternationalizedString description = null;

    public
    FamilyDirectoryResource (@NotNull CarddavLambdaHelper carddavLambdaHelper) {
        super (carddavLambdaHelper);
    }

    @Override
    public
    String getEtag () {
        return this.getCTag();
    }

    @Override
    public
    MemberResource child (String uuid) throws BadRequestException {
        final UUID memberId = UUID.fromString(uuid);
        final MemberResource prefetch = this.memberResources.get(memberId);
        if (prefetch != null) {
            return prefetch;
        } else if (this.isMemberResourcesComplete) {
            return null;
        }
        return Optional.ofNullable(this.carddavLambdaHelper.getDdbItem(uuid, DdbTable.MEMBER))
                       .map(MemberRecord::convertDdbMap)
                       .map(memberRecord -> {
                           final var resource = new MemberResource(this.carddavLambdaHelper, memberRecord);
                           this.memberResources.put(memberRecord.id(), resource);
                           return resource;
                       })
                       .orElse(null);
    }

    @Override
    public
    List<MemberResource> getChildren () {
        if (!this.isMemberResourcesComplete) {
            this.memberResources = this.carddavLambdaHelper.scanMemberDdb()
                                                           .stream()
                                                           .map(memberRecord -> Map.entry(memberRecord.id(), new MemberResource(this.carddavLambdaHelper, memberRecord)))
                                                           .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            this.isMemberResourcesComplete = true;
        }
        return this.memberResources.values()
                                   .stream()
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
    List<MemberResource> getChildren (PrincipalSearchCriteria crit) throws BadRequestException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public
    Date getModifiedDate () {
        if (this.ctag == null) this.getCTag();
        return new Date(UUIDUtil.extractTimestamp(this.ctag));
    }
}
