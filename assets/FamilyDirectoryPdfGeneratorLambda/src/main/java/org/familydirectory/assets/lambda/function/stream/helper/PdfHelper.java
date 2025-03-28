package org.familydirectory.assets.lambda.function.stream.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.DEBUG;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static java.lang.System.getenv;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public final
class PdfHelper implements LambdaFunctionHelper {
    private static final @NotNull String ROOT_MEMBER_ID = requireNonNull(getenv(LambdaUtils.EnvVar.ROOT_ID.name()));
    private static final @NotNull Comparator<Map.Entry<String, Member>> DESCENDANT_COMPARATOR = Comparator.comparing(entry -> entry.getValue()
                                                                                                                                   .getBirthday());
    private static final @NotNull Comparator<Map.Entry<PDDayPageHelper.Day, MemberRecord>> DAY_OF_MONTH_COMPARATOR = Comparator.comparing(entry -> entry.getValue()
                                                                                                                                                        .member()
                                                                                                                                                        .getBirthday()
                                                                                                                                                        .getDayOfMonth());
    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final @NotNull PDDocument familyDirectoryPdf = new PDDocument(MemoryUsageSetting.setupMainMemoryOnly());
    private final @NotNull PDDocument dayPdf = new PDDocument(MemoryUsageSetting.setupMainMemoryOnly());
    private final @NotNull LocalDate date = LocalDate.now();
    private final @NotNull LambdaLogger logger;
    private final @NotNull Set<MemberRecord> members;
    private final @NotNull Map<String, Map<String, AttributeValue>> families;
    private final @NotNull String familyDirectoryTitle;
    private final @NotNull String dayTitle;
    private final @NotNull EnumMap<Month, List<Map.Entry<PDDayPageHelper.Day, MemberRecord>>> dayLists;
    private PDDayPageHelper dayPage = null;
    private PDFamilyDirectoryPageHelper familyDirectoryPage = null;
    private int familyDirectoryPageNumber = 0;
    private int dayPageNumber = 0;

    public
    PdfHelper (final @NotNull LambdaLogger logger) throws IOException {
        super();
        this.logger = requireNonNull(logger);
        this.members = new HashSet<>();
        final String rootMemberLastName = this.getRootMemberSurname()
                                              .toUpperCase();
        this.familyDirectoryTitle = "%s FAMILY DIRECTORY".formatted(rootMemberLastName);
        this.dayTitle = "%s FAMILY BIRTHDAYS".formatted(rootMemberLastName);
        this.families = new HashMap<>();
        this.dayLists = new EnumMap<>(Month.class);
        for (final Month value : Month.values()) {
            this.dayLists.put(value, new ArrayList<>());
        }
        this.generateDirectoryPdf();
        this.generateBirthdayPdf();
        this.logger.log("PdfHelper Ctor Complete", DEBUG);
    }

    public
    void saveDirectoryPdf (final @NotNull OutputStream os) throws IOException {
        this.logger.log("Begin Saving Directory Pdf", DEBUG);
        this.familyDirectoryPdf.save(os);
        this.logger.log("End Saving Directory Pdf", DEBUG);
    }

    public
    void saveBirthdayPdf (final @NotNull OutputStream os) throws IOException {
        this.logger.log("Begin Saving Birthday Pdf", DEBUG);
        this.dayPdf.save(os);
        this.logger.log("End Saving Birthday Pdf", DEBUG);
    }

    private
    void generateDirectoryPdf () throws IOException {
        this.newFamilyDirectoryPage();
        this.logger.log("Begin Build Directory Pdf", INFO);
        this.traverse(ROOT_MEMBER_ID);
        this.logger.log("End Build Directory Pdf", INFO);
        this.familyDirectoryPage.close();
    }

    private
    void generateBirthdayPdf () throws IOException {
        this.newDayPage();
        this.buildDayPdf();
        this.dayPage.close();
    }

    private
    void buildDayPdf () throws IOException {
        this.logger.log("Begin Build Day Pdf", INFO);
        for (final Month month : Month.values()) {
            this.dayLists.get(month)
                         .sort(DAY_OF_MONTH_COMPARATOR);
            this.logger.log("Begin Processing Month: `%s`".formatted(month.name()), INFO);
            final List<Map.Entry<PDDayPageHelper.Day, MemberRecord>> monthDayList = this.dayLists.get(month);
            for (int i = 0; i < monthDayList.size(); ++i) {
                final Map.Entry<PDDayPageHelper.Day, MemberRecord> monthDayListEntry = monthDayList.get(i);
                final PDDayPageHelper.Day day = monthDayListEntry.getKey();
                final MemberRecord currentMemberRecord = monthDayListEntry.getValue();
                final LocalDate date = (day.equals(PDDayPageHelper.Day.BIRTH))
                        ? currentMemberRecord.member()
                                             .getBirthday()
                        : requireNonNull(currentMemberRecord.member()
                                                            .getDeathday());
                this.logger.log("Encountered %s: `%s` for Member: `%s`".formatted(day.name(), date, currentMemberRecord.id()
                                                                                                                       .toString()), INFO);

                try {
                    this.dayPage.addDay(currentMemberRecord, (i == 0)
                            ? month
                            : null, day);
                } catch (final PDPageHelperModel.NewPageException e) {
                    this.newDayPage();
                    try {
                        this.dayPage.addDay(currentMemberRecord, (i == 0)
                                ? month
                                : null, day);
                    } catch (final PDPageHelperModel.NewPageException x) {
                        final IOException thrown = new IOException(x);
                        thrown.addSuppressed(e);
                        throw thrown;
                    }
                }
            }
        }
        this.logger.log("End Build Day Pdf", INFO);
    }

    private
    void newDayPage () throws IOException {
        if (nonNull(this.dayPage)) {
            this.dayPage.close();
        }
        this.dayPage = new PDDayPageHelper(this.dayPdf, new PDPage(), this.dayTitle, this.date, ++this.dayPageNumber);
        this.logger.log("Create Birthday Page %d".formatted(this.dayPageNumber), INFO);
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
                            throw new IllegalStateException("Member: `%s` Not Present And Can't Be Added To Members Set".formatted(id));
                        }
                        this.addToDayLists(result[0]);
                    });
        return result[0];
    }

    private
    void addToDayLists (final @NotNull MemberRecord memberRecord) {
        this.dayLists.get(requireNonNull(memberRecord).member()
                                                      .getBirthday()
                                                      .getMonth())
                     .add(Map.entry(PDDayPageHelper.Day.BIRTH, memberRecord));
        final LocalDate deathday = memberRecord.member()
                                               .getDeathday();
        if (nonNull(deathday)) {
            this.dayLists.get(deathday.getMonth())
                         .add(Map.entry(PDDayPageHelper.Day.DEATH, memberRecord));
        }
    }

    private
    void newFamilyDirectoryPage () throws IOException {
        if (nonNull(this.familyDirectoryPage)) {
            this.familyDirectoryPage.close();
        }
        this.familyDirectoryPage = new PDFamilyDirectoryPageHelper(this.familyDirectoryPdf, new PDPage(), this.familyDirectoryTitle, this.date, ++this.familyDirectoryPageNumber);
        this.logger.log("Create Family Directory Page %d".formatted(this.familyDirectoryPageNumber), INFO);
    }

    private
    void traverse (final @NotNull String id) throws IOException {
        this.logger.log("Begin Processing Id: %s".formatted(id), INFO);

        final @NotNull Map<String, AttributeValue> family = this.retrieveFamily(id);
        final @NotNull Member member = requireNonNull(this.retrieveMember(id)).member();
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

        this.logger.log("End Processing Id: %s".formatted(id), INFO);
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
    void addFamily (final @NotNull Member member, final @Nullable Member spouse, final @Nullable List<Member> descendants, final boolean startOfSection) throws IOException
    {
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

    @SuppressFBWarnings("EI_EXPOSE_REP")
    @Override
    public @NotNull
    DynamoDbClient getDynamoDbClient () {
        return this.dynamoDbClient;
    }

    @Override
    public
    void close () {
        final Deque<Exception> closeExceptions = new ArrayDeque<>(3);
        try {
            LambdaFunctionHelper.super.close();
        } catch (final Exception e) {
            closeExceptions.push(e);
        }
        try {
            this.familyDirectoryPdf.close();
        } catch (final Exception e) {
            closeExceptions.push(e);
        }
        try {
            this.dayPdf.close();
        } catch (final Exception e) {
            closeExceptions.push(e);
        }
        if (!closeExceptions.isEmpty()) {
            final RuntimeException closeException = new RuntimeException(closeExceptions.pop());
            closeExceptions.forEach(closeException::addSuppressed);
            throw closeException;
        }
    }
}
