package org.familydirectory.assets.ddb.models.member;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.familydirectory.assets.ddb.enums.PhoneType;
import org.familydirectory.assets.ddb.enums.SuffixType;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

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

    public abstract @Nullable
    String getEmail ();

    public abstract @Nullable
    List<String> getAddress ();

    public @Nullable
    Map<String, AttributeValue> getPhonesDdbMap () {
        return (isNull(this.getPhones()))
                ? null
                : this.getPhones()
                      .entrySet()
                      .stream()
                      .collect(toUnmodifiableMap(entry -> entry.getKey()
                                                               .name(), entry -> AttributeValue.fromS(entry.getValue())));
    }

    public abstract @Nullable
    Map<PhoneType, String> getPhones ();

    public
    boolean isAdult () {
        return DdbUtils.isPersonAdult(this.getBirthday(), this.getDeathday());
    }

    public abstract @NotNull
    LocalDate getBirthday ();

    public abstract @Nullable
    LocalDate getDeathday ();

    public @NotNull
    String getKey () {
        return sha256Hex("%s %s".formatted(this.getFullName(), this.getBirthdayString()));
    }

    public @NotNull
    String getBirthdayString () {
        return DdbUtils.DATE_FORMATTER.format(this.getBirthday());
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

    public @Nullable
    String getDeathdayString () {
        return ofNullable(this.getDeathday()).map(DdbUtils.DATE_FORMATTER::format)
                                             .orElse(null);
    }

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
}
