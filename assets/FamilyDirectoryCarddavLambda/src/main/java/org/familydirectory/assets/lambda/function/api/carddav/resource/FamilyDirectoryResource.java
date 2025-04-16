package org.familydirectory.assets.lambda.function.api.carddav.resource;

import io.milton.common.InternationalizedString;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.values.Pair;
import io.milton.principal.PrincipalSearchCriteria;
import io.milton.resource.AddressBookQuerySearchableResource;
import io.milton.resource.AddressBookResource;
import io.milton.resource.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.familydirectory.assets.lambda.function.api.helpers.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;

public final
class FamilyDirectoryResource extends AbstractResource implements AddressBookResource, AddressBookQuerySearchableResource {

    private Set<MemberResource> memberResources = new HashSet<>();

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
        this.memberResources = this.carddavLambdaHelper.scanMemberDdb()
                                                       .stream()
                                                       .map(memberRecord -> new MemberResource(this.carddavLambdaHelper, memberRecord))
                                                       .collect(Collectors.toUnmodifiableSet());
        return this.memberResources.stream()
                                   .toList();
    }

    @Override
    public
    String getCTag () {
        return "";
    }

    @Override
    public
    InternationalizedString getDescription () {
        return null;
    }

    @Override
    public
    void setDescription (InternationalizedString description) {

    }

    @Override
    public
    List<Pair<String, String>> getSupportedAddressData () {
        return List.of();
    }

    @Override
    public
    Long getMaxResourceSize () {
        return 0L;
    }

    @Override
    public
    List<MemberResource> getChildren (PrincipalSearchCriteria crit) throws NotAuthorizedException, BadRequestException {
        return List.of();
    }
}
