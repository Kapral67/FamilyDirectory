package org.familydirectory.assets.lambda.function.api.carddav;

import io.milton.common.InternationalizedString;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.values.HrefList;
import io.milton.http.values.Pair;
import io.milton.principal.Principal;
import io.milton.principal.PrincipalSearchCriteria;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.AddressBookQuerySearchableResource;
import io.milton.resource.AddressBookResource;
import io.milton.resource.GetableResource;
import io.milton.resource.ReportableResource;
import io.milton.resource.Resource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

public
class FamilyDirectoryResource implements AddressBookResource, GetableResource, ReportableResource, AccessControlledResource, AddressBookQuerySearchableResource {
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
    String getCTag () {
        return "";
    }

    @Override
    public
    Resource child (String childName) throws NotAuthorizedException, BadRequestException {
        return null;
    }

    @Override
    public
    List<? extends Resource> getChildren () throws NotAuthorizedException, BadRequestException {
        return List.of();
    }

    @Override
    public
    Date getCreateDate () {
        return null;
    }

    @Override
    public
    String getUniqueId () {
        return "";
    }

    @Override
    public
    String getName () {
        return "";
    }

    @Override
    public
    Object authenticate (String user, String password) {
        return null;
    }

    @Override
    public
    boolean authorise (Request request, Request.Method method, Auth auth) {
        return false;
    }

    @Override
    public
    String getRealm () {
        return "";
    }

    @Override
    public
    Date getModifiedDate () {
        return null;
    }

    @Override
    public
    String checkRedirect (Request request) throws NotAuthorizedException, BadRequestException {
        return "";
    }

    @Override
    public
    void sendContent (OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {

    }

    @Override
    public
    Long getMaxAgeSeconds (Auth auth) {
        return 0L;
    }

    @Override
    public
    String getContentType (String accepts) {
        return "";
    }

    @Override
    public
    Long getContentLength () {
        return 0L;
    }

    @Override
    public
    String getPrincipalURL () {
        return "";
    }

    @Override
    public
    List<Priviledge> getPriviledges (Auth auth) {
        return List.of();
    }

    @Override
    public
    Map<Principal, List<Priviledge>> getAccessControlList () {
        return Map.of();
    }

    @Override
    public
    void setAccessControlList (Map<Principal, List<Priviledge>> privs) {

    }

    @Override
    public
    HrefList getPrincipalCollectionHrefs () {
        return null;
    }

    @Override
    public
    List<? extends Resource> getChildren (PrincipalSearchCriteria crit) throws NotAuthorizedException, BadRequestException {
        return List.of();
    }
}
