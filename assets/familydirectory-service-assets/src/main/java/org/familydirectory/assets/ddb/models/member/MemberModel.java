package org.familydirectory.assets.ddb.models.member;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.digest.DigestUtils;
import org.familydirectory.assets.ddb.enums.PhoneType;
import org.familydirectory.assets.ddb.enums.SuffixType;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableMap;

public abstract
class MemberModel {
    public static final char DAGGER = '†';

    public static @Nullable
    Map<PhoneType, String> convertPhonesDdbMap (final @NotNull Map<String, AttributeValue> phonesDdbMap) {
        final Set<String> phoneTypeNames = Arrays.stream(PhoneType.values())
                                                 .map(PhoneType::name)
                                                 .collect(toSet());
        return phonesDdbMap.entrySet()
                           .stream()
                           .filter(entry -> phoneTypeNames.contains(entry.getKey()) && nonNull(entry.getValue()
                                                                                                    .s()) && !entry.getValue()
                                                                                                                   .s()
                                                                                                                   .isBlank())
                           .collect(collectingAndThen(toUnmodifiableMap(entry -> PhoneType.valueOf(entry.getKey()), entry -> entry.getValue()
                                                                                                                                  .s()), map -> map.isEmpty()
                                   ? null
                                   : map));
    }

    /**
     * @param date Must be a date formatted as defined by {@link DdbUtils#DATE_FORMAT_STRING}
     */
    public static @NotNull
    LocalDate convertStringToDate (final @NotNull String date) {
        return LocalDate.parse(date, DdbUtils.DATE_FORMATTER);
    }

    public static @NotNull
    List<String> convertAddressDdb (final @NotNull List<AttributeValue> ddb) {
        return ddb.stream()
                  .map(AttributeValue::s)
                  .toList();
    }

    public
    boolean isAdult () {
        return DdbUtils.isPersonAdult(this.getBirthday(), this.getDeathday());
    }

    public abstract @NotNull
    LocalDate getBirthday ();

    public abstract @Nullable
    LocalDate getDeathday ();

    public final
    int getAge () {
        return DdbUtils.getPersonAge(this.getBirthday(), this.getDeathday());
    }

    public @NotNull
    String getFullName () {
        final StringBuilder fullName = new StringBuilder();
        fullName.append("%s ".formatted(this.getFirstName()));
        if (nonNull(this.getMiddleName())) {
            fullName.append("%s ".formatted(this.getMiddleName()));
        }
        fullName.append(this.getLastName());
        if (nonNull(this.getSuffix())) {
            fullName.append(" %s".formatted(this.getSuffix()
                                                .value()));
        }
        return fullName.toString();
    }

    public abstract @NotNull
    String getFirstName ();

    public abstract @Nullable
    String getMiddleName ();

    public abstract @NotNull
    String getLastName ();

    public abstract @Nullable
    SuffixType getSuffix ();

    public @NotNull
    String getDisplayName () {
        final StringBuilder displayName = new StringBuilder();
        displayName.append("%s %s".formatted(this.getFirstName(), this.getLastName()));
        if (nonNull(this.getSuffix())) {
            displayName.append(" %s".formatted(this.getSuffix()
                                                   .value()));
        }
        return displayName.toString();
    }

    @Override
    public
    String toString () {
        final StringBuilder stringBuilder = new StringBuilder("{");

        for (final MemberTableParameter field : MemberTableParameter.values()) {
            switch (field) {
                case ID, FAMILY_ID, VCARD, ETAG -> {
                    continue;
                }
                default -> stringBuilder.append("%s: ".formatted(field.jsonFieldName()));
            }
            switch (field) {
                case FIRST_NAME -> stringBuilder.append("\"%s\"".formatted(this.getFirstName()));
                case MIDDLE_NAME -> {
                    final String middleName = this.getMiddleName();
                    stringBuilder.append(isNull(middleName)
                                                 ? "null"
                                                 : "\"%s\"".formatted(middleName));
                }
                case LAST_NAME -> stringBuilder.append("\"%s\"".formatted(this.getLastName()));
                case SUFFIX -> {
                    final SuffixType suffix = this.getSuffix();
                    stringBuilder.append(isNull(suffix)
                                                 ? "null"
                                                 : "\"%s\"".formatted(suffix.value()));
                }
                case BIRTHDAY -> stringBuilder.append("\"%s\"".formatted(this.getBirthdayString()));
                case DEATHDAY -> {
                    final String deathdate = this.getDeathdayString();
                    stringBuilder.append(isNull(deathdate)
                                                 ? "null"
                                                 : "\"%s\"".formatted(deathdate));
                }
                case EMAIL -> {
                    final String email = this.getEmail();
                    stringBuilder.append(isNull(email)
                                                 ? "null"
                                                 : "\"%s\"".formatted(email));
                }
                case PHONES -> {
                    final Map<PhoneType, String> phones = this.getPhones();
                    if (isNull(phones)) {
                        stringBuilder.append("null");
                    } else {
                        stringBuilder.append('{');

                        for (final PhoneType phoneType : PhoneType.values()) {
                            stringBuilder.append("%s: ".formatted(phoneType.getJson()));
                            final String phone = phones.get(phoneType);
                            stringBuilder.append(isNull(phone)
                                                         ? "null"
                                                         : "\"%s\"".formatted(phone))
                                         .append(',');
                        }

                        stringBuilder.append('}');
                    }
                }
                case ADDRESS -> {
                    final List<String> addressLines = this.getAddress();
                    if (isNull(addressLines)) {
                        stringBuilder.append("null");
                    } else {
                        stringBuilder.append('[');

                        for (final String line : addressLines) {
                            stringBuilder.append(isNull(line)
                                                         ? "null"
                                                         : "\"%s\"".formatted(line))
                                         .append(',');
                        }

                        stringBuilder.append(']');
                    }
                }
                default -> throw new IllegalStateException("Unhandled Member Parameter: `%s`".formatted(field.jsonFieldName()));
            }
            stringBuilder.append(',');
        }

        return stringBuilder.append('}')
                            .toString();
    }

    public String getEtag() {
        return DigestUtils.sha256Hex(this.toString());
    }

    public abstract @Nullable @UnmodifiableView
    Map<PhoneType, String> getPhones ();

    public abstract @Nullable @UnmodifiableView
    List<String> getAddress ();

    public abstract @Nullable
    String getEmail ();

    public @Nullable
    String getDeathdayString () {
        return ofNullable(this.getDeathday()).map(DdbUtils.DATE_FORMATTER::format)
                                             .orElse(null);
    }

    public @NotNull
    String getBirthdayString () {
        return DdbUtils.DATE_FORMATTER.format(this.getBirthday());
    }

    public @Nullable
    List<AttributeValue> getAddressDdb () {
        return MemberModel.revertAddressDdb(this.getAddress());
    }

    public static @Nullable
    List<AttributeValue> revertAddressDdb (final @Nullable List<String> address) {
        return ofNullable(address).map(l -> l.stream()
                                             .map(AttributeValue::fromS)
                                             .toList())
                                  .orElse(null);
    }

    public @Nullable @UnmodifiableView
    Map<String, AttributeValue> getPhonesDdbMap () {
        return (isNull(this.getPhones()))
                ? null
                : this.getPhones()
                      .entrySet()
                      .stream()
                      .collect(toUnmodifiableMap(entry -> entry.getKey()
                                                               .name(), entry -> AttributeValue.fromS(entry.getValue())));
    }
}
