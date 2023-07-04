package org.familydirectory.assets.ddb.models.members;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.commons.text.WordUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.familydirectory.assets.ddb.DdbType;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.familydirectory.assets.ddb.DdbType.MAP;
import static org.familydirectory.assets.ddb.DdbType.STR;
import static org.familydirectory.assets.ddb.DdbType.STR_SET;

@JsonDeserialize(as = ImmutableMembersModel.class)
@Value.Immutable
public abstract class MembersModel {
    private static final Date PREHISTORIC =
            Date.from(LocalDate.of(1915, 7, 11).atStartOfDay(ZoneId.systemDefault()).toInstant());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @JsonCreator
    public static ImmutableMembersModel create(@NotNull @JsonProperty("firstName") final String firstName,
                                               @NotNull @JsonProperty("lastName") final String lastName,
                                               @Nullable @JsonProperty("suffix") final String suffix,
                                               @NotNull @JsonProperty("birthday") final String birthday,
                                               @Nullable @JsonProperty("deathday") final String deathday,
                                               @Nullable @JsonProperty("email") final String email,
                                               @Nullable @JsonProperty("phones") final Map<String, String> phones,
                                               @Nullable @JsonProperty("address") final List<String> address)
            throws ParseException {
        ImmutableMembersModel.Builder builder = ImmutableMembersModel.builder().firstName(firstName).lastName(lastName)
                .birthday(DATE_FORMAT.parse(birthday));

        if (Objects.nonNull(suffix)) {
            builder.suffix(SuffixType.valueOf(suffix));
        }

        if (Objects.nonNull(deathday)) {
            builder.deathday(DATE_FORMAT.parse(deathday));
        }

        if (Objects.nonNull(email)) {
            builder.email(email);
        }

        if (Objects.nonNull(phones)) {
            Map<PhoneType, String> telephones = new HashMap<>();
            for (Map.Entry<String, String> entry : phones.entrySet()) {
                telephones.put(PhoneType.valueOf(entry.getKey()), entry.getValue());
            }
            builder.phones(telephones);
        }

        if (Objects.nonNull(address)) {
            builder.address(address);
        }

        return builder.build();
    }

    @NotNull
    protected abstract String getFirstName();

    @NotNull
    @JsonProperty("firstName")
    public final String firstName() {
        return WordUtils.capitalize(WordUtils.capitalize(this.getFirstName().trim()));
    }

    @NotNull
    protected abstract String getLastName();

    @NotNull
    @JsonProperty("lastName")
    public final String lastName() {
        return WordUtils.capitalize(WordUtils.capitalize(this.getLastName().trim()));
    }

    @NotNull
    public abstract Date getBirthday();

    @NotNull
    @JsonProperty("birthday")
    public final String birthday() {
        return DATE_FORMAT.format(this.getBirthday());
    }

    @Nullable
    protected abstract String getEmail();

    @Nullable
    @JsonProperty("email")
    public final String email() {
        return Optional.ofNullable(this.getEmail()).map(String::trim).map(WordUtils::capitalize).orElse(null);
    }

    @Nullable
    public abstract Date getDeathday();

    @Nullable
    @JsonProperty("deathday")
    public final String deathday() {
        return (Objects.nonNull(this.getDeathday())) ? DATE_FORMAT.format(this.getDeathday()) : null;
    }

    @Nullable
    public abstract Map<PhoneType, String> getPhones();

    @NotNull
    public final Optional<Map<String, AttributeValue>> getPhonesDdbMap() {
        return Optional.ofNullable(this.getPhones()).map(phoneMap -> phoneMap.entrySet().stream().collect(
                Collectors.toMap(entry -> entry.getKey().name(),
                        entry -> AttributeValue.builder().s(entry.getValue()).build())));
    }

    @Nullable
    @JsonProperty("phones")
    public final Map<String, String> phones() {
        return Optional.ofNullable(this.getPhones()).map(phoneMap -> phoneMap.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue))).orElse(null);
    }

    @Nullable
    protected abstract List<String> getAddress();

    @Nullable
    @JsonProperty("address")
    public final List<String> address() {
        return Optional.ofNullable(getAddress())
                .map(i -> i.stream().filter(Objects::nonNull).map(String::trim).map(WordUtils::capitalize).toList())
                .orElse(null);
    }

    @Nullable
    public abstract SuffixType getSuffix();

    @Nullable
    @JsonProperty("suffix")
    public final String suffix() {
        return Optional.ofNullable(this.getSuffix()).map(SuffixType::value).orElse(null);
    }

    @NotNull
    public final String getFullName() {
        final Optional<String> suffix = Optional.ofNullable(this.suffix());
        if (suffix.isPresent()) {
            return String.format("%s %s %s", this.firstName(), this.lastName(), suffix.get());
        } else {
            return String.format("%s %s", this.firstName(), this.lastName());
        }
    }

    @Value.Check
    protected void validateInstance() throws IllegalStateException {
        if (this.getFirstName().isBlank()) {
            throw new IllegalStateException("First Name Cannot Be Blank");
        }
        if (this.getLastName().isBlank()) {
            throw new IllegalStateException("Last Name Cannot Be Blank");
        }
        if (this.getBirthday().after(Date.from(Instant.now()))) {
            throw new IllegalStateException("Birthday Cannot Be Future");
        }
        if (this.getBirthday().before(PREHISTORIC)) {
            throw new IllegalStateException("Birthday Cannot Be PreHistoric");
        }
        if (Objects.nonNull(this.getEmail()) && !EmailValidator.getInstance().isValid(this.getEmail())) {
            throw new IllegalStateException("Email Invalid");
        }
        if (Objects.nonNull(this.getDeathday()) && this.getDeathday().before(this.getBirthday())) {
            throw new IllegalStateException("Cannot Have Died Before Birth");
        }
        if (Objects.nonNull(this.getPhones())) {
            for (final Map.Entry<PhoneType, String> entry : this.getPhones().entrySet()) {
                Phonenumber.PhoneNumber phoneNumber = new Phonenumber.PhoneNumber().setRawInput(entry.getValue());
                if (!PhoneNumberUtil.getInstance().isValidNumber(phoneNumber)) {
                    throw new IllegalStateException(
                            String.format("Invalid Phone Number: '%s' For '%s' Field", entry.getValue(),
                                    entry.getKey().name()));
                }
            }
        }
    }


    public enum Params {
        FIRST_NAME(STR, "firstName"), LAST_NAME(STR, "lastName"), SUFFIX(STR, "suffix"), BIRTHDAY(STR,
                "birthday"), DEATHDAY(STR, "deathday"), EMAIL(STR, "email"), PHONES(MAP, "phones"), ADDRESS(STR_SET,
                "address");

        private final DdbType ddbType;
        private final String jsonFieldName;

        Params(final DdbType ddbType, final String jsonFieldName) {
            this.ddbType = ddbType;
            this.jsonFieldName = jsonFieldName;
        }

        public final DdbType ddbType() {
            return this.ddbType;
        }

        public final String jsonFieldName() {
            return this.jsonFieldName;
        }
    }
}
