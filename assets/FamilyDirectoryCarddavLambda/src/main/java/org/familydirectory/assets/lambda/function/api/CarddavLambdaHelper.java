package org.familydirectory.assets.lambda.function.api;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.MiltonException;
import io.milton.http.exceptions.NotAuthorizedException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.sync.SyncTableParameter;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.lambda.function.api.carddav.request.CarddavRequest;
import org.familydirectory.assets.lambda.function.api.carddav.resource.AbstractResourceObject;
import org.familydirectory.assets.lambda.function.api.carddav.resource.DeletedMemberResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.FDResourceFactory;
import org.familydirectory.assets.lambda.function.api.carddav.resource.FamilyDirectoryResource;
import org.familydirectory.assets.lambda.function.api.carddav.resource.IMemberResource;
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
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import static io.milton.http.ResponseStatus.SC_BAD_REQUEST;
import static java.lang.System.getenv;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.isBase64;
import static org.apache.commons.codec.binary.StringUtils.newStringUtf8;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.FORBIDDEN;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.getDefaultMethodResponse;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.getPresentMemberResourceProps;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.getPrincipalUrlProp;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.handleDeletedMemberResource;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.handlePresentMemberResource;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.handlePrincipalCollectionResource;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.handleRootCollectionResource;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.handleSystemPrincipal;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.handleUserPrincipal;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.normalizeHref;
import static org.familydirectory.assets.lambda.function.api.CarddavResponseUtils.options;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.ADDRESS_BOOK_PATH;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.CURRENT_USER_PRIVILEGE_SET;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.INITIAL_RESOURCE_CONTAINER_SIZE;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.SUPPORTED_REPORTS;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.CARDDAV_NS;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.cEmpty;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.cParent;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.cProp;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.dEmpty;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.dParent;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.dProp;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.okPropstat;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.parseMultigetHrefs;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.parseReportRoot;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.parseSyncToken;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.renderMultistatus;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.renderValidSyncTokenError;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavXmlUtils.statusPropstat;

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
            final var status = Optional.ofNullable(e.getResponseEvent().getStatusCode())
                                       .map(Response.Status::fromCode)
                                       .orElse(Response.Status.SC_INTERNAL_SERVER_ERROR);
            throw (CarddavResponseException) new CarddavResponseException(
                CarddavResponse.builder()
                               .status(status)
                               .build()
            ).initCause(e);
        }
    }

    @Override
    @NotNull
    public
    Caller getCaller() {
        if (this.caller != null) {
            return this.caller;
        }
        // FIXME
        return this.caller = Optional.ofNullable(this.getDdbItem(requireNonNull(getenv(LambdaUtils.EnvVar.ROOT_ID.name())), DdbTable.MEMBER))
                                     .map(MemberRecord::convertDdbMap)
                                     .map(memberRecord -> new Caller(memberRecord, false))
                                     .orElseThrow();
    }

    public
    CarddavResponse getResponse () {
        try {
            return this.process();
        } catch (MiltonException e) {
            if (!(e instanceof BadRequestException badE)) {
                throw new RuntimeException(e);
            }
            if (badE.getReason() != null && badE.getResource() != null) {
                final var msg = "Bad Request: %s.class %s".formatted(
                    badE.getResource().getClass().getSimpleName(),
                    String.valueOf(badE.getReason())
                );
                this.getLogger().log(msg, LogLevel.INFO);
            }
            LambdaUtils.logTrace(this.getLogger(), badE, LogLevel.INFO);
            return CarddavResponse.builder()
                                  .status(Response.Status.SC_BAD_REQUEST)
                                  .build();
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
            case DeletedMemberResource ignored -> handleDeletedMemberResource();
            case SystemPrincipal systemPrincipal -> handleSystemPrincipal(method, systemPrincipal);
            case UserPrincipal callerPrincipal -> handleUserPrincipal(method, callerPrincipal);
        };
    }

    private
    CarddavResponse processAddressBookRequest(Request.Method method, FamilyDirectoryResource addressbook) throws BadRequestException, NotAuthorizedException {
        return switch (method) {
            case OPTIONS -> options(addressbook);
            case PROPFIND -> handleAddressBookPropFind(addressbook);
            case REPORT -> {
                final var reportRoot = parseReportRoot(this.request::getInputStream);
                final var ns = reportRoot.getNamespaceURI();
                if (!ns.startsWith("DAV") && !ns.contains(CARDDAV_NS)) {
                    yield FORBIDDEN;
                }
                yield switch (reportRoot.getLocalPart()) {
                    case "addressbook-multiget" -> handleAddressbookMultigetReport(addressbook);
                    case "addressbook-query" -> handleAddressbookQueryReport(addressbook);
                    case "sync-collection" -> handleAddressbookSyncReport(addressbook);
                    default -> FORBIDDEN;
                };
            }
            default -> getDefaultMethodResponse(method, addressbook);
        };
    }

    private
    CarddavResponse handleAddressbookMultigetReport(FamilyDirectoryResource addressbook) throws BadRequestException {
        final var hrefs = parseMultigetHrefs(this.request::getInputStream);

        final List<DavResponse> responses = new ArrayList<>(hrefs.size());
        for (final var href : hrefs) {
            final var path = normalizeHref(href);
            final IMemberResource resource;
            try {
                resource = (IMemberResource) this.resourceFactory.getResource("", path);
            } catch (final Exception e) {
                LambdaUtils.logTrace(this.getLogger(), e, LogLevel.INFO);
                responses.add(
                    new DavResponse(href, singletonList(statusPropstat(Response.Status.SC_BAD_REQUEST, emptyList())))
                );
                continue;
            }
            responses.add(
                switch (resource) {
                    case DeletedMemberResource ignored ->
                        new DavResponse(href, singletonList(statusPropstat(Response.Status.SC_NOT_FOUND, emptyList())));
                    case PresentMemberResource presentMemberResource ->
                        new DavResponse(href, singletonList(okPropstat(getPresentMemberResourceProps(presentMemberResource))));
                }
            );
        }

        return CarddavResponse.builder()
                              .status(Response.Status.SC_MULTI_STATUS)
                              .header(Response.Header.CONTENT_TYPE, Response.APPLICATION_XML)
                              .body(renderMultistatus(unmodifiableList(responses)))
                              .build();
    }

    private
    CarddavResponse handleAddressbookQueryReport(FamilyDirectoryResource addressbook) {
        final var responses = addressbook.getChildren()
                                         .stream()
                                         .filter(PresentMemberResource.class::isInstance)
                                         .map(PresentMemberResource.class::cast)
                                         .map(member -> new DavResponse(
                                            member.getHref(),
                                            singletonList(okPropstat(getPresentMemberResourceProps(member)))
                                         )).toList();

        return CarddavResponse.builder()
                              .status(Response.Status.SC_MULTI_STATUS)
                              .header(Response.Header.CONTENT_TYPE, Response.APPLICATION_XML)
                              .body(renderMultistatus(responses))
                              .build();
    }

    private
    CarddavResponse handleAddressbookSyncReport(FamilyDirectoryResource addressbook) {
        final URI syncTokenUri;
        final Set<IMemberResource> changesSinceLastSync;
        try {
            syncTokenUri = parseSyncToken(this.request::getInputStream).map(UUID::fromString)
                                                                       .map(UUID::toString)
                                                                       .map(URI::create)
                                                                       .orElse(null);
            final var stream = syncTokenUri == null
                ? addressbook.getChildren()
                             .stream()
                : addressbook.findResourcesBySyncToken(syncTokenUri)
                             .values()
                             .stream()
                             .map(IMemberResource.class::cast);
            changesSinceLastSync = stream.collect(toUnmodifiableSet());
        } catch (final Exception e) {
            LambdaUtils.logTrace(this.getLogger(), e, LogLevel.INFO);
            return CarddavResponse.builder()
                                  .status(Response.Status.SC_FORBIDDEN)
                                  .header(Response.Header.CONTENT_TYPE, Response.APPLICATION_XML)
                                  .body(renderValidSyncTokenError())
                                  .build();
        }

        final var responses = changesSinceLastSync.stream().map(memberResource -> switch (memberResource) {
            case DeletedMemberResource deleted ->
                new DavResponse(deleted.getHref(), singletonList(statusPropstat(Response.Status.SC_NOT_FOUND, emptyList())));
            case PresentMemberResource present ->
                new DavResponse(present.getHref(), singletonList(okPropstat(getPresentMemberResourceProps(present))));
        }).toList();

        return CarddavResponse.builder().status(Response.Status.SC_MULTI_STATUS)
                              .header(Response.Header.CONTENT_TYPE, Response.APPLICATION_XML)
                              .body(renderMultistatus(responses, addressbook.getSyncToken()))
                              .build();
    }

    private
    CarddavResponse handleAddressBookPropFind(FamilyDirectoryResource addressbook) {
        final int depth = this.request.getDepthHeader();
        final var responses = new ArrayList<DavResponse>();
        {
            final var props = new ArrayList<DavProperty>();

            props.add(dParent("resourcetype", List.of(dEmpty("collection"), cEmpty("addressbook"))));

            props.add(CURRENT_USER_PRIVILEGE_SET);

            props.add(getPrincipalUrlProp(addressbook));

            props.add(dProp("displayname", addressbook.getDescription().getValue()));

            props.add(dProp("sync-token", addressbook.getSyncToken().toString()));

            props.add(cProp("getctag", addressbook.getCTag(), emptyMap()));

            final var supportedAddressDataTypes = new ArrayList<DavProperty>(1);
            addressbook.getSupportedAddressData().forEach(pair -> supportedAddressDataTypes.add(
               cProp("address-data-type", null, Map.of("content-type", pair.getObject1(), "version", pair.getObject2()))
            ));
            props.add(cParent("supported-address-data", unmodifiableList(supportedAddressDataTypes)));

            final var supportedReports = SUPPORTED_REPORTS.stream()
                                               .map(CarddavXmlUtils::dEmpty)
                                               .map(Collections::singletonList)
                                               .map(dProps -> dParent("report", dProps))
                                               .map(Collections::singletonList)
                                               .map(dProps -> dParent("supported-report", dProps))
                                               .toList();
            props.add(dParent("supported-report-set", supportedReports));

            responses.add(new DavResponse(ADDRESS_BOOK_PATH, singletonList(okPropstat(unmodifiableList(props)))));
        }
        if (depth > 0) {
            addressbook.getChildren()
                       .stream()
                       .filter(PresentMemberResource.class::isInstance)
                       .map(PresentMemberResource.class::cast)
                       .map(child ->
                            new DavResponse(child.getHref(), singletonList(okPropstat(getPresentMemberResourceProps(child))))
                       ).forEach(responses::add);
        }
        return CarddavResponse.builder()
                              .status(Response.Status.SC_MULTI_STATUS)
                              .header(Response.Header.CONTENT_TYPE, Response.APPLICATION_XML)
                              .body(renderMultistatus(unmodifiableList(responses)))
                              .build();
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
    Set<UUID> traverseSyncDdb (UUID fromToken) throws NoSuchTokenException {
        final var changedMemberIds = new HashSet<UUID>();
        Optional<UUID> nextToken = Optional.of(fromToken);
        while (nextToken.isPresent()) {
            final String token = nextToken.get().toString();
            final boolean isFromToken = fromToken.equals(nextToken.get());
            final var tokenAttrMap = Optional.ofNullable(this.getDdbItem(token, DdbTable.SYNC))
                                             .orElseThrow(() -> new NoSuchTokenException(token));
            nextToken = Optional.ofNullable(tokenAttrMap.get(SyncTableParameter.NEXT.jsonFieldName()))
                                .map(AttributeValue::s)
                                .filter(Predicate.not(String::isBlank))
                                .map(UUID::fromString);
            if (isFromToken) continue;
            changedMemberIds.addAll(Optional.ofNullable(tokenAttrMap.get(SyncTableParameter.MEMBERS.jsonFieldName()))
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
    class NoSuchTokenException extends NoSuchElementException {
        NoSuchTokenException(String token) {
            super(token);
        }
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
