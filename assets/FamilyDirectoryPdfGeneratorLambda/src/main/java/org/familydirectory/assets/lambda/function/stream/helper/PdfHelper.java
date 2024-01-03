package org.familydirectory.assets.lambda.function.stream.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.lambda.function.helper.LambdaFunctionHelper;
import org.familydirectory.assets.lambda.function.stream.enums.StreamFunction;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static java.lang.System.getenv;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public final
class PdfHelper implements LambdaFunctionHelper {
    private static final String ROOT_MEMBER_ID = getenv(LambdaUtils.EnvVar.ROOT_ID.name());
    private static final Comparator<Map.Entry<String, Member>> DESCENDANT_COMPARATOR = Comparator.comparing(entry -> entry.getValue()
                                                                                                                          .getBirthday());
    @NotNull
    private final PDDocument pdf = new PDDocument(MemoryUsageSetting.setupMainMemoryOnly(StreamFunction.PDF_GENERATOR.memorySize()
                                                                                                                     .longValue() * 1000L * 1000L));
    @NotNull
    private final LocalDate date = LocalDate.now();
    @NotNull
    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    @NotNull
    private final LambdaLogger logger;
    @NotNull
    private final Map<String, Member> members;
    @NotNull
    private final Map<String, Map<String, AttributeValue>> families;
    @NotNull
    private final String title;
    private int pageNumber = 0;
    private PDPageHelper page = null;

    public
    PdfHelper (final @NotNull LambdaLogger logger) {
        super();
        this.logger = requireNonNull(logger);
        this.members = new HashMap<>();
        this.title = "%s FAMILY DIRECTORY".formatted(Optional.of(this.retrieveMember(ROOT_MEMBER_ID)
                                                                     .getLastName())
                                                             .filter(Predicate.not(String::isBlank))
                                                             .map(String::toUpperCase)
                                                             .orElseThrow());
        this.families = new HashMap<>();
    }

    @Contract("null -> null; !null -> !null")
    private @Nullable
    Member retrieveMember (final @Nullable String id) {
        if (isNull(id)) {
            return null;
        }
        if (!this.members.containsKey(id)) {
            this.members.put(id, this.getMemberById(id));
        }
        return this.members.get(id);
    }

    @NotNull
    private
    Member getMemberById (final @NotNull String id) {
        final Map<String, AttributeValue> memberMap = requireNonNull(this.getDdbItem(id, DdbTable.MEMBER));
        return Member.convertDdbMap(memberMap);
    }

    private
    void newPage () throws IOException {
        if (nonNull(this.page)) {
            this.page.close();
        }
        this.page = new PDPageHelper(this.pdf, new PDPage(), this.title, this.date, ++this.pageNumber);
        this.logger.log("[INFO]: Create Page %d".formatted(this.pageNumber), LogLevel.INFO);
    }

    @Contract("-> new")
    @NotNull
    public
    RequestBody getPdf () throws IOException {
        this.newPage();
        this.traverse(ROOT_MEMBER_ID);
        this.page.close();

        final ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        this.pdf.save(pdfOutputStream);
        return RequestBody.fromBytes(pdfOutputStream.toByteArray());
    }

    private
    void traverse (final @NotNull String id) throws IOException {
        this.logger.log("[INFO]: Begin Processing Id: %s".formatted(id), LogLevel.INFO);

        final @NotNull Map<String, AttributeValue> family = this.retrieveFamily(id);
        final @NotNull Member member = this.retrieveMember(id);
        final @Nullable Member spouse = this.retrieveMember(ofNullable(family.get(FamilyTableParameter.SPOUSE.jsonFieldName())).map(AttributeValue::s)
                                                                                                                               .filter(Predicate.not(String::isBlank))
                                                                                                                               .orElse(null));
        final @NotNull List<Member> deadEndDescendants = new ArrayList<>();
        final @NotNull List<String> recursiveDescendantIds = new ArrayList<>();
        final @NotNull List<Map.Entry<String, Member>> descendants = new ArrayList<>();
        ofNullable(family.get(FamilyTableParameter.DESCENDANTS.jsonFieldName())).map(AttributeValue::ss)
                                                                                .filter(Predicate.not(List::isEmpty))
                                                                                .ifPresent(ss -> ss.stream()
                                                                                                   .filter(Predicate.not(String::isBlank))
                                                                                                   .forEach(s -> descendants.add(Map.entry(s, this.retrieveMember(s)))));

        if (!descendants.isEmpty()) {
            descendants.sort(DESCENDANT_COMPARATOR);
            for (final Map.Entry<String, Member> descendant : descendants) {
                final @NotNull Map<String, AttributeValue> descendantFamily = this.retrieveFamily(descendant.getKey());

//          DEAD-END-DESCENDANTS ARE (A) NOT ADULTS & (B) DON'T HAVE DESCENDANTS
                if (!descendant.getValue()
                               .isAdult() && ofNullable(descendantFamily.get(FamilyTableParameter.DESCENDANTS.jsonFieldName())).map(AttributeValue::ss)
                                                                                                                               .filter(Predicate.not(List::isEmpty))
                                                                                                                               .isEmpty())
                {
                    deadEndDescendants.add(descendant.getValue());
                } else {
                    recursiveDescendantIds.add(descendant.getKey());
                }
            }
        }

        this.addFamily(member, spouse, (deadEndDescendants.isEmpty())
                ? null
                : deadEndDescendants, ofNullable(this.retrieveFamily(ROOT_MEMBER_ID)
                                                     .get(FamilyTableParameter.DESCENDANTS.jsonFieldName())).map(AttributeValue::ss)
                                                                                                            .filter(Predicate.not(List::isEmpty))
                                                                                                            .filter(ss -> ss.contains(id))
                                                                                                            .isPresent());

        for (final String recursiveDescendant : recursiveDescendantIds) {
            this.traverse(recursiveDescendant);
        }

        this.logger.log("[INFO]: End Processing Id: %s".formatted(id), LogLevel.INFO);
    }

    private @NotNull
    Map<String, AttributeValue> retrieveFamily (final @NotNull String id) {
        Optional.of(id)
                .filter(Predicate.not(String::isBlank))
                .orElseThrow();
        if (!this.families.containsKey(id)) {
            this.families.put(id, requireNonNull(this.getDdbItem(id, DdbTable.FAMILY)));
        }
        return this.families.get(id);
    }

    private
    void addFamily (final @NotNull Member member, final @Nullable Member spouse, final @Nullable List<Member> descendants, final boolean startOfSection) throws IOException {
        try {
            this.page.addBodyTextBlock(member, spouse, descendants, startOfSection);
        } catch (final PDPageHelper.NewPageException e) {
            this.newPage();
            try {
                this.page.addBodyTextBlock(member, spouse, descendants, startOfSection);
            } catch (final PDPageHelper.NewPageException x) {
                final IOException thrown = new IOException(x);
                thrown.addSuppressed(e);
                throw thrown;
            }
        }
    }

    @Override
    public @NotNull
    LambdaLogger getLogger () {
        return this.logger;
    }

    @Override
    public @NotNull
    DynamoDbClient getDynamoDbClient () {
        return this.dynamoDbClient;
    }

    @Override
    public
    void close () {
        LambdaFunctionHelper.super.close();
        try {
            this.pdf.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
