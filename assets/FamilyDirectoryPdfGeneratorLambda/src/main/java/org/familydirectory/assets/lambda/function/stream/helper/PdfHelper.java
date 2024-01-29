package org.familydirectory.assets.lambda.function.stream.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Predicate;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.lambda.function.helper.LambdaFunctionHelper;
import org.familydirectory.assets.lambda.function.stream.helper.models.PDPageHelperModel;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    private static final Comparator<MemberRecord> DAY_OF_MONTH_COMPARATOR = Comparator.comparing(entry -> entry.member()
                                                                                                               .getBirthday()
                                                                                                               .getDayOfMonth());
    @NotNull
    private final PDDocument familyDirectoryPdf = new PDDocument(MemoryUsageSetting.setupMainMemoryOnly());
    @NotNull
    private final PDDocument birthdayPdf = new PDDocument(MemoryUsageSetting.setupMainMemoryOnly());
    @NotNull
    private final LocalDate date = LocalDate.now();
    @NotNull
    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    @NotNull
    private final LambdaLogger logger;
    @NotNull
    private final Set<MemberRecord> members;
    @NotNull
    private final Map<String, Map<String, AttributeValue>> families;
    @NotNull
    private final String familyDirectoryTitle;
    @NotNull
    private final String birthdayTitle;
    @NotNull
    private final EnumMap<Month, TreeSet<MemberRecord>> birthdayTreeSets;
    private PDBirthdayPageHelper birthdayPage = null;
    private PDFamilyDirectoryPageHelper familyDirectoryPage = null;
    private int familyDirectoryPageNumber = 0;
    private int birthdayPageNumber = 0;

    public
    PdfHelper (final @NotNull LambdaLogger logger) {
        super();
        this.logger = requireNonNull(logger);
        this.members = new HashSet<>();
        final String rootMemberLastName = this.getRootMemberSurname()
                                              .toUpperCase();
        this.familyDirectoryTitle = "%s FAMILY DIRECTORY".formatted(rootMemberLastName);
        this.birthdayTitle = "%s FAMILY BIRTHDAYS".formatted(rootMemberLastName);
        this.families = new HashMap<>();
        this.birthdayTreeSets = new EnumMap<>(Month.class);
        for (final Month value : Month.values()) {
            this.birthdayTreeSets.put(value, new TreeSet<>(DAY_OF_MONTH_COMPARATOR));
        }
    }

    @Contract("null -> null; !null -> !null")
    private @Nullable
    MemberRecord retrieveMember (final @Nullable String id) {
        if (isNull(id)) {
            return null;
        }
        final UUID uuid = UUID.fromString(id);
        final MemberRecord[] result = new MemberRecord[1];
        this.members.stream()
                    .filter(memberRecord -> memberRecord.id()
                                                        .equals(uuid))
                    .findAny()
                    .ifPresentOrElse(memberRecord -> result[0] = memberRecord, () -> {
                        final Map<String, AttributeValue> memberMap = requireNonNull(this.getDdbItem(id, DdbTable.MEMBER));
                        final Member member = Member.convertDdbMap(memberMap);
                        final UUID ddbId = UUID.fromString(memberMap.get(MemberTableParameter.ID.jsonFieldName())
                                                                    .s());
                        if (!ddbId.equals(uuid)) {
                            throw new IllegalStateException("Invalid ID `%s` doesn't match DDB_ID `%s`".formatted(uuid.toString(), ddbId.toString()));
                        }
                        result[0] = new MemberRecord(uuid, member, UUID.fromString(memberMap.get(MemberTableParameter.FAMILY_ID.jsonFieldName())
                                                                                            .s()));
                        if (!this.members.add(result[0])) {
                            throw new IllegalStateException("Member: `%s` Not Present And Can't Be Added".formatted(id));
                        }
                        if (!this.addToBirthdayTreeSets(result[0])) {
                            throw new IllegalStateException("Birthday TreeSets Not Accepting Member: `%s`".formatted(id));
                        }
                    });
        return result[0];
    }

    private
    boolean addToBirthdayTreeSets (final @NotNull MemberRecord memberRecord) {
        return this.birthdayTreeSets.get(requireNonNull(memberRecord).member()
                                                                     .getBirthday()
                                                                     .getMonth())
                                    .add(memberRecord);
    }

    private
    void newFamilyDirectoryPage () throws IOException {
        if (nonNull(this.familyDirectoryPage)) {
            this.familyDirectoryPage.close();
        }
        this.familyDirectoryPage = new PDFamilyDirectoryPageHelper(this.familyDirectoryPdf, new PDPage(), this.familyDirectoryTitle, this.date, ++this.familyDirectoryPageNumber);
        this.logger.log("Create Family Directory Page %d".formatted(this.familyDirectoryPageNumber), LogLevel.INFO);
    }

    @Contract("-> new")
    @NotNull
    public
    PdfBundle getPdfBundle () throws IOException {
        this.newFamilyDirectoryPage();
        this.newBirthdayPage();
        this.traverse(ROOT_MEMBER_ID);
        this.buildBirthdayPdf();
        this.familyDirectoryPage.close();
        this.birthdayPage.close();

        try (final ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {
            this.familyDirectoryPdf.save(pdfOutputStream);
            final byte[] familyDirectoryArr = pdfOutputStream.toByteArray();

            pdfOutputStream.reset();

            this.birthdayPdf.save(pdfOutputStream);
            final byte[] birthdayArr = pdfOutputStream.toByteArray();

            return new PdfBundle(familyDirectoryArr, birthdayArr);
        }
    }

    private
    void buildBirthdayPdf () throws IOException {
        for (final Month month : Month.values()) {
            if (this.birthdayTreeSets.containsKey(month)) {
                final TreeSet<MemberRecord> recordSet = this.birthdayTreeSets.get(month);
                try {
                    this.birthdayPage.addBirthday(recordSet.first(), month);
                } catch (final PDPageHelperModel.NewPageException e) {
                    this.newBirthdayPage();
                    try {
                        this.birthdayPage.addBirthday(recordSet.first(), month);
                    } catch (final PDPageHelperModel.NewPageException x) {
                        final IOException thrown = new IOException(x);
                        thrown.addSuppressed(e);
                        throw thrown;
                    }
                }
                recordSet.removeFirst();
                for (final MemberRecord record : recordSet) {
                    try {
                        this.birthdayPage.addBirthday(record);
                    } catch (final PDPageHelperModel.NewPageException e) {
                        this.newBirthdayPage();
                        try {
                            this.birthdayPage.addBirthday(record);
                        } catch (final PDPageHelperModel.NewPageException x) {
                            final IOException thrown = new IOException(x);
                            thrown.addSuppressed(e);
                            throw thrown;
                        }
                    }
                }
            }
        }
    }

    private
    void newBirthdayPage () throws IOException {
        if (nonNull(this.birthdayPage)) {
            this.birthdayPage.close();
        }
        this.birthdayPage = new PDBirthdayPageHelper(this.birthdayPdf, new PDPage(), this.birthdayTitle, this.date, ++this.birthdayPageNumber);
        this.logger.log("Create Birthday Page %d".formatted(this.birthdayPageNumber), LogLevel.INFO);
    }

    private
    void traverse (final @NotNull String id) throws IOException {
        this.logger.log("Begin Processing Id: %s".formatted(id), LogLevel.INFO);

        final @NotNull Map<String, AttributeValue> family = this.retrieveFamily(id);
        final @NotNull Member member = this.retrieveMember(id)
                                           .member();
        final @Nullable Member spouse = ofNullable(this.retrieveMember(ofNullable(family.get(FamilyTableParameter.SPOUSE.jsonFieldName())).map(AttributeValue::s)
                                                                                                                                          .filter(Predicate.not(String::isBlank))
                                                                                                                                          .orElse(null))).map(MemberRecord::member)
                                                                                                                                                         .orElse(null);
        final @NotNull List<Member> deadEndDescendants = new ArrayList<>();
        final @NotNull List<String> recursiveDescendantIds = new ArrayList<>();
        final @NotNull List<Map.Entry<String, Member>> descendants = new ArrayList<>();
        ofNullable(family.get(FamilyTableParameter.DESCENDANTS.jsonFieldName())).map(AttributeValue::ss)
                                                                                .filter(Predicate.not(List::isEmpty))
                                                                                .ifPresent(ss -> ss.stream()
                                                                                                   .filter(Predicate.not(String::isBlank))
                                                                                                   .forEach(s -> descendants.add(Map.entry(s, this.retrieveMember(s)
                                                                                                                                                  .member()))));

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

        this.logger.log("End Processing Id: %s".formatted(id), LogLevel.INFO);
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
            this.familyDirectoryPage.addBodyTextBlock(member, spouse, descendants, startOfSection);
        } catch (final PDPageHelperModel.NewPageException e) {
            this.newFamilyDirectoryPage();
            try {
                this.familyDirectoryPage.addBodyTextBlock(member, spouse, descendants, startOfSection);
            } catch (final PDPageHelperModel.NewPageException x) {
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
            this.familyDirectoryPdf.close();
            this.birthdayPdf.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public
    record PdfBundle(byte[] familyDirectoryPdf, byte[] birthdayPdf) {
    }
}
