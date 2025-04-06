package org.familydirectory.assets.ddb.member;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import org.familydirectory.assets.ddb.enums.PhoneType;
import org.familydirectory.assets.ddb.enums.SuffixType;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import static java.util.Objects.requireNonNull;

public final class Vcard {
    public static final String VERSION = "3.0";

    private static final String HEADER = "BEGIN:VCARD\nVERSION:%s\n".formatted(VERSION);
    private static final String FOOTER = "END:VCARD\n";

    private static final String ITEM_FORMAT = "item%d.";
    private static final String X_ABLABEL_FORMAT = ITEM_FORMAT + "X-ABLabel:%s\n";

    private static final String ADR_FORMAT = ITEM_FORMAT + "ADR:;;%s;;;;\n";
    private static final String ADR_LABEL = "address";

    private static final String N_FORMAT = "N:%s;%s;%s;;%s\n";

    private static final String EMAIL_FORMAT = ITEM_FORMAT + "EMAIL;type=INTERNET:%s\n";
    private static final String EMAIL_LABEL = "email";

    private static final String LANDLINE_FORMAT = ITEM_FORMAT + "TEL:%s\n";
    private static final String MOBILE_FORMAT = ITEM_FORMAT + "TEL;type=CELL:%s\n";

    private static final String BDAY_FORMAT = "BDAY:%s\n";

    private static final String DDAY_FORMAT = ITEM_FORMAT + "X-ABDATE:%s\n";
    private static final String DDAY_LABEL = "deathday";

    @NotNull
    private final MemberRecord member;

    public Vcard(@NotNull final MemberRecord member) {
        this.member = requireNonNull(member);
    }

    @NotNull
    private
    String n () {
        final Member member = this.member.member();
        return N_FORMAT.formatted(
            member.getLastName(),
            member.getFirstName(),
            Optional.ofNullable(member.getMiddleName())
                    .orElse(""),
            Optional.ofNullable(member.getSuffix())
                    .map(SuffixType::value)
                    .orElse("")
        );
    }

    @NotNull
    private
    String fn () {
        return "FN:" + this.member.member().getFullName() + '\n';
    }

    @NotNull
    @UnmodifiableView
    private
    List<String> email(final int item) {
        final List<String> email = new ArrayList<>(2);
        Optional.ofNullable(this.member.member().getEmail())
                .map(address -> EMAIL_FORMAT.formatted(item, address))
                .ifPresent(address -> {
                    email.add(address);
                    email.add(X_ABLABEL_FORMAT.formatted(item, EMAIL_LABEL));
                });
        return Collections.unmodifiableList(email);
    }

    @NotNull
    @UnmodifiableView
    private
    List<String> adr (final int item) {
        final List<String> adr = new ArrayList<>(2);
        Optional.ofNullable(this.member.member()
                                       .getAddress())
                .map(List::stream)
                .flatMap(stream -> stream.reduce("%s\\n%s"::formatted))
                .map(address -> ADR_FORMAT.formatted(item, address))
                .ifPresent(address -> {
                    adr.add(address);
                    adr.add(X_ABLABEL_FORMAT.formatted(item, ADR_LABEL));
                });
        return Collections.unmodifiableList(adr);
    }

    @NotNull
    @UnmodifiableView
    private
    List<String> tel (final int item, @NotNull final PhoneType type) {
        final List<String> tel = new ArrayList<>(2);
        Optional.ofNullable(this.member.member().getPhones())
                .map(phones -> phones.get(type))
                .map(phone -> switch (type) {
                    case MOBILE -> MOBILE_FORMAT.formatted(item, phone);
                    case LANDLINE -> LANDLINE_FORMAT.formatted(item, phone);
                })
                .ifPresent(phone -> {
                    tel.add(phone);
                    tel.add(X_ABLABEL_FORMAT.formatted(item, type.name().toLowerCase(Locale.ROOT)));
                });
        return Collections.unmodifiableList(tel);
    }

    @NotNull
    @UnmodifiableView
    private
    List<String> deathday(final int item) {
        final List<String> deathday = new ArrayList<>(2);
        Optional.ofNullable(this.member.member().getDeathdayString())
                .map(dday -> DDAY_FORMAT.formatted(item, dday))
                .ifPresent(dday -> {
                    deathday.add(dday);
                    deathday.add(X_ABLABEL_FORMAT.formatted(item, DDAY_LABEL));
                });
        return Collections.unmodifiableList(deathday);
    }

    private static void appendItem(
        final int @NotNull [] item,
        @NotNull final StringBuilder vcard,
        @NotNull final Function<Integer, List<String>> function
    ) {
        final List<String> lines = function.apply(item[0]);
        if (!lines.isEmpty()) ++item[0];
        lines.forEach(vcard::append);
    }

    @Override
    @NotNull
    public
    String toString () {
        final int[] item = {1};
        final StringBuilder vcard = new StringBuilder(HEADER);
        vcard.append(n());
        vcard.append(fn());
        appendItem(item, vcard, this::email);
        appendItem(item, vcard, i -> this.tel(i, PhoneType.LANDLINE));
        appendItem(item, vcard, i -> this.tel(i, PhoneType.MOBILE));
        appendItem(item, vcard, this::adr);
        vcard.append(BDAY_FORMAT.formatted(this.member.member().getBirthdayString()));
        appendItem(item, vcard, this::deathday);
        return vcard.append(FOOTER).toString();
    }
}
