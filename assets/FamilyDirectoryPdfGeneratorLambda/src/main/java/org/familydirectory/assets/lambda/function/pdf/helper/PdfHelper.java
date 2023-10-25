package org.familydirectory.assets.lambda.function.pdf.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.SuffixType;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.lambda.function.helper.LambdaFunctionHelper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public final
class PdfHelper implements LambdaFunctionHelper {
    @NotNull
    private final PDDocument pdf = new PDDocument();
    @NotNull
    private final LocalDate date = LocalDate.now();
    @NotNull
    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    @NotNull
    private final LambdaLogger logger;
    @NotNull
    private final String rootMemberId;
    @NotNull
    private final Map<String, Member> members;
    @NotNull
    private final Map<String, Map<String, AttributeValue>> families;
    @NotNull
    private final String title;
    private int pageNumber = 0;
    private PDPageHelper page = null;

    public
    PdfHelper (final @NotNull LambdaLogger logger, final @NotNull String rootMemberId) throws IOException {
        super();
        this.logger = requireNonNull(logger);
        this.rootMemberId = requireNonNull(rootMemberId);
        this.members = new HashMap<>();
        this.members.put(this.rootMemberId, this.getMemberById(this.rootMemberId));
        this.title = "%s FAMILY DIRECTORY".formatted(Optional.of(this.members.get(this.rootMemberId)
                                                                             .getLastName())
                                                             .filter(Predicate.not(String::isBlank))
                                                             .map(String::toUpperCase)
                                                             .orElseThrow());
        this.families = new HashMap<>();
        this.newPage();
    }

    private
    void newPage () throws IOException {
        if (nonNull(this.page)) {
            this.page.close();
        }
        this.page = new PDPageHelper(this.pdf, new PDPage(), this.title, this.date, ++this.pageNumber);
    }

    @NotNull
    private
    Member getMemberById (final @NotNull String id) {
        final Map<String, AttributeValue> memberMap = requireNonNull(this.getDdbItem(id, DdbTable.MEMBER));
        final Member.Builder memberBuilder = Member.builder();
        for (final MemberTableParameter param : MemberTableParameter.values()) {
            ofNullable(memberMap.get(param.jsonFieldName())).ifPresent(av -> {
                switch (param) {
                    case FIRST_NAME -> memberBuilder.firstName(av.s());
                    case MIDDLE_NAME -> memberBuilder.middleName(av.s());
                    case LAST_NAME -> memberBuilder.lastName(av.s());
                    case BIRTHDAY -> memberBuilder.birthday(Member.convertStringToDate(av.s()));
                    case DEATHDAY -> memberBuilder.deathday(Member.convertStringToDate(av.s()));
                    case EMAIL -> memberBuilder.email(av.s());
                    case ADDRESS -> {
                        if (av.hasSs()) {
                            memberBuilder.address(av.ss());
                        }
                    }
                    case PHONES -> {
                        if (av.hasM()) {
                            memberBuilder.phones(Member.convertPhonesDdbMap(av.m()));
                        }
                    }
                    case SUFFIX -> memberBuilder.suffix(SuffixType.forValue(av.s()));
                    default -> {
                    }
                }
            });
        }
        return memberBuilder.build();
    }

    /*
     * FIXME: How to know if needs endOfSection set when calling addFamily()?
     * INSIGHT:
     * Instead of having addFamily() be endOfSection, have it be startOfSection.
     * That way we can print the section line before the body block whenever we are printing one of ROOT's direct-descendants
     * (Do Not Print Section Lines when location.y equals bodyContentStartY)
     */
    private
    void traverse (final @NotNull String id) throws IOException {
        final @NotNull Map<String, AttributeValue> family = this.retrieveFamily(id);
        final @NotNull Member member = this.retrieveMember(id);
        final @Nullable Member spouse = this.retrieveMember(ofNullable(family.get(FamilyTableParameter.SPOUSE.jsonFieldName())).map(AttributeValue::s)
                                                                                                                               .filter(Predicate.not(String::isBlank))
                                                                                                                               .orElse(null));
        final @NotNull List<Member> deadEndDescendants = new ArrayList<>(0);

        final @Nullable List<String> descendantIds = ofNullable(family.get(FamilyTableParameter.DESCENDANTS.jsonFieldName())).filter(AttributeValue::hasSs)
                                                                                                                             .map(AttributeValue::ss)
                                                                                                                             .filter(Predicate.not(List::isEmpty))
                                                                                                                             .filter(l -> !l.stream()
                                                                                                                                            .allMatch(String::isBlank))
                                                                                                                             .orElse(null);
        if (nonNull(descendantIds)) {
//            boolean isLastDescendant = true;
            for (final String descendantId : descendantIds) {
                if (descendantId.isBlank()) {
                    continue;
                }
//                isLastDescendant = false;

                final @NotNull Map<String, AttributeValue> descendantFamily = this.retrieveFamily(descendantId);
                final @NotNull Member descendant = this.retrieveMember(descendantId);
                if (!descendant.isAdult() && ofNullable(descendantFamily.get(FamilyTableParameter.DESCENDANTS.jsonFieldName())).filter(AttributeValue::hasSs)
                                                                                                                               .map(AttributeValue::ss)
                                                                                                                               .filter(Predicate.not(List::isEmpty))
                                                                                                                               .filter(l -> !l.stream()
                                                                                                                                              .allMatch(String::isBlank))
                                                                                                                               .isEmpty())
                {
                    deadEndDescendants.add(descendant);
                } else {
//                    this.traverse(descendantId, false);
                }
            }
//            if (endOfSection) {
//                this.traverse(id, true);
//            }
        }

//        if (id.equals(this.rootMemberId)) {
//            this.addFamily(member, spouse, (deadEndDescendants.isEmpty())
//                    ? null
//                    : deadEndDescendants, true);
//        } else {
//
//        }
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
    void addFamily (final @NotNull Member member, final @Nullable Member spouse, final @Nullable List<Member> descendants, final boolean endOfSection) throws IOException {
        try {
            this.page.addBodyTextBlock(member, spouse, descendants, endOfSection);
        } catch (final PDPageHelper.NewPageException e) {
            this.newPage();
            try {
                this.page.addBodyTextBlock(member, spouse, descendants, endOfSection);
            } catch (final PDPageHelper.NewPageException x) {
                throw new RuntimeException(e);
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
}
