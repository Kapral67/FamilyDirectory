package org.familydirectory.assets.lambda.function.api;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.MiltonException;
import io.milton.http.exceptions.NotAuthorizedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.sync.SyncTableParameter;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.lambda.function.api.carddav.request.CarddavRequest;
import org.familydirectory.assets.lambda.function.api.carddav.resource.AbstractResourceObject;
import org.familydirectory.assets.lambda.function.api.carddav.resource.DeletedMemberResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.FDResourceFactory;
import org.familydirectory.assets.lambda.function.api.carddav.resource.FamilyDirectoryResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.PresentMemberResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.PrincipalCollectionResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.RootCollectionResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.SystemPrincipal;
import org.familydirectory.assets.lambda.function.api.carddav.resource.UserPrincipal;
import org.familydirectory.assets.lambda.function.api.carddav.response.CarddavResponse;
import org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils;
import org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.DavProperty;
import org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.DavResponse;
import org.familydirectory.assets.lambda.function.api.helper.ApiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import static io.milton.http.DateUtils.formatForWebDavModifiedDate;
import static io.milton.http.ResponseStatus.SC_BAD_REQUEST;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.isBase64;
import static org.apache.commons.codec.binary.StringUtils.newStringUtf8;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.getDefaultMethodResponse;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.handleDeletedMemberResource;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.handlePresentMemberResource;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.handlePrincipalCollectionResource;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.handleRootCollectionResource;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.handleSystemPrincipal;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.handleUserPrincipal;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.options;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.ADDRESS_BOOK_PATH;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.INITIAL_RESOURCE_CONTAINER_SIZE;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.cEmpty;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.cParent;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.cProp;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.dEmpty;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.dParent;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.dProp;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.okPropstat;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.renderMultistatus;

public final
class CarddavLambdaHelper extends ApiHelper {
    private FDResourceFactory resourceFactory = null;
    @NotNull
    private final Set<MemberRecord> memberRecords = new HashSet<>(INITIAL_RESOURCE_CONTAINER_SIZE);
    private final CarddavRequest request;

    CarddavLambdaHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) throws CarddavResponseException {
        super(logger, requestEvent);
        try {
            if (!isBase64(this.requestEvent.getBody())) {
                throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_BAD_REQUEST));
            }
            this.request = new CarddavRequest(newStringUtf8(decodeBase64(this.requestEvent.getBody())));
            new FDResourceFactory(this);
        } catch (ResponseException e) {
            // TODO
            throw new CarddavResponseException(CarddavResponse.builder().build());
        }
    }

    public
    CarddavResponse getResponse () {
        try {
            return this.process();
        } catch (MiltonException e) {
            // TODO
            return CarddavResponse.builder().build();
        }
    }

    private
    CarddavResponse process() throws BadRequestException, NotAuthorizedException {
        final FDResourceFactory resourceFactory = this.getResourceFactory();
        final var resource = (AbstractResourceObject) resourceFactory.getResource("", this.request.getAbsolutePath());
        final var method = this.request.getMethod();

        return switch (resource) {
            case RootCollectionResource root -> handleRootCollectionResource(method, root);
            case PrincipalCollectionResource principals -> handlePrincipalCollectionResource(method, principals);
            case FamilyDirectoryResource addressbook -> processAddressBookRequest(method, addressbook);
            case PresentMemberResource presentMember -> handlePresentMemberResource(method, presentMember);
            case DeletedMemberResource deletedMember -> handleDeletedMemberResource(method, deletedMember);
            case SystemPrincipal systemPrincipal -> handleSystemPrincipal(method, systemPrincipal);
            case UserPrincipal callerPrincipal -> handleUserPrincipal(method, callerPrincipal);
        };
    }

    private
    CarddavResponse processAddressBookRequest(Request.Method method, FamilyDirectoryResource addressbook) {
        return switch (method) {
            case OPTIONS -> options(addressbook);
            case PROPFIND -> handleAddressBookPropFind(addressbook);
            case REPORT -> handleAddressBookReport(addressbook);
            default -> getDefaultMethodResponse(method, addressbook);
        };
    }

    private
    CarddavResponse handleAddressBookPropFind(FamilyDirectoryResource addressbook) {
        final int depth = this.request.getDepthHeader();
        final var responses = new ArrayList<DavResponse>();
        {
            final var props = new ArrayList<DavProperty>();

            props.add(dParent("resourcetype", List.of(dEmpty("collection"), cEmpty("addressbook"))));

            props.add(dProp("displayname", addressbook.getDescription().getValue()));

            props.add(dProp("sync-token", addressbook.getSyncToken().toString()));

            final var supportedAddressDataTypes = new ArrayList<DavProperty>(1);
            addressbook.getSupportedAddressData().forEach(pair -> supportedAddressDataTypes.add(
               cProp("address-data-type", null, Map.of("content-type", pair.getObject1(), "version", pair.getObject2()))
            ));
            props.add(cParent("supported-address-data", unmodifiableList(supportedAddressDataTypes)));

            final var supportedReports = Stream.of("addressbook-multiget", "sync-collection")
                                               .map(CarddavXmlUtils::dEmpty)
                                               .map(Collections::singletonList)
                                               .map(dProps -> dParent("report", dProps))
                                               .map(Collections::singletonList)
                                               .map(dProps -> dParent("supported-report", dProps))
                                               .toList();
            props.add(dParent("supported-report-set", supportedReports));

            responses.add(new DavResponse(ADDRESS_BOOK_PATH, singletonList(okPropstat(props))));
        }
        if (depth > 0) {
            addressbook.getChildren()
                       .stream()
                       .filter(PresentMemberResource.class::isInstance)
                       .map(PresentMemberResource.class::cast)
                       .map(child -> {
                           final var props = List.of(
                               dProp("getetag", '"' + child.getEtag() + '"'),
                               dProp("getlastmodified", formatForWebDavModifiedDate(child.getModifiedDate())),
                               dEmpty("resourcetype"),
                               cProp("address-data", child.getAddressData(), emptyMap())
                           );
                           return new DavResponse(child.getHref(), singletonList(okPropstat(props)));
                       }).forEach(responses::add);
        }
        return CarddavResponse.builder()
                              .status(Response.Status.SC_MULTI_STATUS)
                              .header(Response.Header.CONTENT_TYPE, Response.APPLICATION_XML)
                              .body(renderMultistatus(unmodifiableList(responses)))
                              .build();
    }

    private
    CarddavResponse handleAddressBookReport(FamilyDirectoryResource addressbook) {
        // TODO
    }

    @NotNull
    @UnmodifiableView
    public
    Set<MemberRecord> scanMemberDdb () {
        if (!this.memberRecords.isEmpty()) {
            return Collections.unmodifiableSet(this.memberRecords);
        }

        Map<String, AttributeValue> lastEvaluatedKey = emptyMap();
        do {
            final ScanRequest.Builder scanRequestBuilder = ScanRequest.builder().tableName(DdbTable.MEMBER.name());

            if (!lastEvaluatedKey.isEmpty()) {
                scanRequestBuilder.exclusiveStartKey(lastEvaluatedKey);
            }

            final ScanResponse scanResponse = this.getDynamoDbClient().scan(scanRequestBuilder.build());

            scanResponse.items()
                        .stream()
                        .map(MemberRecord::convertDdbMap)
                        .forEach(this.memberRecords::add);

            lastEvaluatedKey = scanResponse.lastEvaluatedKey();

        } while (!lastEvaluatedKey.isEmpty());

        return Collections.unmodifiableSet(this.memberRecords);
    }

    @NotNull
    @UnmodifiableView
    public
    Set<UUID> traverseSyncDdb (UUID fromToken) {
        final var changedMemberIds = new HashSet<UUID>();
        Optional<UUID> nextToken = Optional.of(fromToken);
        while (nextToken.isPresent()) {
            final var tokenAttrMap = Optional.ofNullable(this.getDdbItem(fromToken.toString(), DdbTable.SYNC))
                                             .orElseThrow();
            nextToken = Optional.ofNullable(tokenAttrMap.get(SyncTableParameter.NEXT.toString()))
                                .map(AttributeValue::s)
                                .filter(Predicate.not(String::isBlank))
                                .map(UUID::fromString);
            changedMemberIds.addAll(Optional.ofNullable(tokenAttrMap.get(SyncTableParameter.MEMBERS.toString()))
                                            .map(AttributeValue::ss)
                                            .stream()
                                            .flatMap(List::stream)
                                            .map(UUID::fromString)
                                            .toList());
        }
        return Collections.unmodifiableSet(changedMemberIds);
    }

    public
    void registerResourceFactory(@NotNull FDResourceFactory resourceFactory) {
        if (this.resourceFactory != null) {
            throw new IllegalStateException("Resource factory already registered");
        }
        this.resourceFactory = requireNonNull(resourceFactory);
    }

    public
    FDResourceFactory getResourceFactory () {
        return requireNonNull(this.resourceFactory);
    }

    public static final
    class CarddavResponseException extends Exception {
        private final CarddavResponse response;

        public
        CarddavResponseException (final @NotNull CarddavResponse response) {
            super();
            this.response = requireNonNull(response);
        }

        public @NotNull
        CarddavResponse getResponse () {
            return this.response;
        }
    }
}
