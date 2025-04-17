package org.familydirectory.assets.lambda.function.api.carddav.resource;

import com.fasterxml.uuid.impl.UUIDUtil;
import io.milton.common.InternationalizedString;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.values.Pair;
import io.milton.principal.PrincipalSearchCriteria;
import io.milton.resource.AddressBookQuerySearchableResource;
import io.milton.resource.AddressBookResource;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.sync.SyncTableParameter;
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

    private Set<MemberResource> memberResources = new HashSet<>();
    private boolean isMemberResourcesComplete = false;

    private UUID ctag = null;

    private InternationalizedString description = null;

    public
    FamilyDirectoryResource (@NotNull CarddavLambdaHelper carddavLambdaHelper) {
        super (carddavLambdaHelper);
    }

    @Override
    public
    MemberResource child (String childName) throws BadRequestException {
        return null;
    }

    @Override
    public
    List<MemberResource> getChildren () {
        if (!this.isMemberResourcesComplete) {
            this.memberResources = this.carddavLambdaHelper.scanMemberDdb()
                                                           .stream()
                                                           .map(memberRecord -> new MemberResource(this.carddavLambdaHelper, memberRecord))
                                                           .collect(Collectors.toUnmodifiableSet());
            this.isMemberResourcesComplete = true;
        }
        return this.memberResources.stream()
                                   .toList();
    }

    @Override
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
    public @NotNull
    List<Pair<String, String>> getSupportedAddressData () {
        return SUPPORTED_ADDRESS_DATA;
    }

    @Override
    @Deprecated
    public
    Long getMaxResourceSize () {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    List<MemberResource> getChildren (PrincipalSearchCriteria crit) throws NotAuthorizedException, BadRequestException {
        return List.of();
    }

    @Override
    @NotNull
    public
    Date getModifiedDate () {
        if (this.ctag == null) this.getCTag();
        return Date.from(Instant.ofEpochMilli(UUIDUtil.extractTimestamp(this.ctag)));
    }
}
