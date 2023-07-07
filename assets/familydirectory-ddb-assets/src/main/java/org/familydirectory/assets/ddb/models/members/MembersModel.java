package org.familydirectory.assets.ddb.models.members;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.commons.text.WordUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.familydirectory.assets.ddb.DdbType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.familydirectory.assets.ddb.DdbType.MAP;
import static org.familydirectory.assets.ddb.DdbType.STR;
import static org.familydirectory.assets.ddb.DdbType.STR_SET;

@JsonDeserialize(builder = MembersModel.Builder.class)
public final class MembersModel {
    private static final LocalDate PREHISTORIC = LocalDate.of(1915, 7, 11);
    private static final String DATE_FORMAT_STRING = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT_STRING);
    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

    @JsonProperty("firstName")
    private final @NotNull String firstName;

    @JsonProperty("lastName")
    private final @NotNull String lastName;

    @JsonProperty("birthday")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT_STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private final @NotNull LocalDate birthday;

    @JsonProperty("email")
    private final @Nullable String email;

    @JsonProperty("deathday")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT_STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private final @Nullable LocalDate deathday;

    @JsonProperty("phones")
    private final @Nullable Map<PhoneType, String> phones;

    @JsonProperty("address")
    private final @Nullable List<String> address;

    @JsonProperty("suffix")
    private final @Nullable SuffixType suffix;

    @JsonIgnore
    private final @NotNull String fullName;

    @JsonIgnore
    private final @NotNull String birthdayString;

    @JsonIgnore
    private final @Nullable Map<String, AttributeValue> phonesDdbMap;

    @JsonIgnore
    private final @Nullable String deathdayString;

    private MembersModel(final @NotNull String firstName, final @NotNull String lastName,
                         final @NotNull LocalDate birthday, final @Nullable String email,
                         final @Nullable LocalDate deathday, final @Nullable Map<PhoneType, String> phones,
                         final @Nullable List<String> address, final @Nullable SuffixType suffix) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthday = birthday;
        this.email = email;
        this.deathday = deathday;
        this.phones = phones;
        this.address = address;
        this.suffix = suffix;
        // Derived
        this.fullName =
                (Objects.isNull(this.getSuffix())) ? String.format("%s %s", this.getFirstName(), this.getLastName()) :
                        String.format("%s %s %s", this.getFirstName(), this.getLastName(), this.getSuffix().value());
        this.birthdayString = DATE_FORMATTER.format(this.getBirthday());
        this.deathdayString = (Objects.isNull(this.getDeathday())) ? null : DATE_FORMATTER.format(this.getDeathday());
        this.phonesDdbMap = (Objects.isNull(this.getPhones())) ? null : this.getPhones().entrySet().stream().collect(
                Collectors.toMap(entry -> entry.getKey().name(),
                        entry -> AttributeValue.builder().s(entry.getValue()).build()));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static @NotNull String getPhoneNumberIfValidElseThrow(final @NotNull String uncheckedPhoneNumber) {
        final String errorMessage = String.format("Invalid Phone Number: '%s'", uncheckedPhoneNumber);
        String region = (uncheckedPhoneNumber.contains("+")) ? null : "US";
        final Phonenumber.PhoneNumber phoneNumber;
        try {
            phoneNumber = PHONE_NUMBER_UTIL.parse(uncheckedPhoneNumber, region);
        } catch (final NumberParseException e) {
            throw new IllegalArgumentException(errorMessage, e);
        }
        if (!PHONE_NUMBER_UTIL.isValidNumber(phoneNumber)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return String.format("+%s%s", phoneNumber.getCountryCode(), phoneNumber.getNationalNumber());
    }

    public @NotNull String getFirstName() {
        return this.firstName;
    }

    public @NotNull String getLastName() {
        return this.lastName;
    }

    public @NotNull LocalDate getBirthday() {
        return this.birthday;
    }

    public @Nullable String getEmail() {
        return this.email;
    }

    public @Nullable LocalDate getDeathday() {
        return this.deathday;
    }

    public @Nullable Map<PhoneType, String> getPhones() {
        return this.phones;
    }

    public @Nullable List<String> getAddress() {
        return this.address;
    }

    public @Nullable SuffixType getSuffix() {
        return this.suffix;
    }

    /* DyanmoDB Artifacts */
    public @NotNull String getBirthdayString() {
        return this.birthdayString;
    }

    public @Nullable String getDeathdayString() {
        return this.deathdayString;
    }

    public @Nullable Map<String, AttributeValue> getPhonesDdbMap() {
        return this.phonesDdbMap;
    }

    public @NotNull String getFullName() {
        return this.fullName;
    }

    public boolean equals(final @NotNull MembersModel member) {
        return this.getFullName().equals(member.getFullName()) &&
                this.getBirthdayString().equals(member.getBirthdayString());
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

    public static final class Builder {
        private final LocalDate builderBegan;
        private String firstName = null;
        private boolean isFirstNameSet = false;
        private String lastName = null;
        private boolean isLastNameSet = false;
        private LocalDate birthday = null;
        private boolean isBirthdaySet = false;
        private String email = null;
        private boolean isEmailSet = false;
        private LocalDate deathday = null;
        private boolean isDeathdaySet = false;
        private Map<PhoneType, String> phones = null;
        private boolean isPhonesSet = false;
        private List<String> address = null;
        private boolean isAddressSet = false;
        private SuffixType suffix = null;
        private boolean isSuffixSet = false;
        private boolean isBuilt = false;

        public Builder() {
            this.builderBegan = LocalDate.now();
        }

        @JsonProperty("firstName")
        public Builder firstName(final @NotNull String firstName) {
            if (this.isFirstNameSet) {
                throw new IllegalStateException("First Name already set");
            } else if (firstName.isBlank()) {
                throw new IllegalArgumentException("First Name cannot be blank");
            }
            this.firstName = WordUtils.capitalizeFully(firstName.replaceAll("\\s", ""), '-');
            this.isFirstNameSet = true;
            return this;
        }

        @JsonProperty("lastName")
        public Builder lastName(final @NotNull String lastName) {
            if (this.isLastNameSet) {
                throw new IllegalStateException("Last Name already set");
            } else if (lastName.isBlank()) {
                throw new IllegalArgumentException("Last Name cannot be blank");
            }
            this.lastName = WordUtils.capitalizeFully(lastName.replaceAll("\\s", ""), '-');
            this.isLastNameSet = true;
            return this;
        }

        @JsonProperty("birthday")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT_STRING)
        @JsonDeserialize(using = LocalDateDeserializer.class)
        public Builder birthday(final @NotNull LocalDate birthday) {
            if (this.isBirthdaySet) {
                throw new IllegalStateException("Birthday already set");
            } else if (birthday.isAfter(this.builderBegan)) {
                throw new IllegalArgumentException("Birthday cannot be future");
            } else if (birthday.isBefore(PREHISTORIC)) {
                throw new IllegalArgumentException("Birthday cannot be prehistoric");
            }
            this.birthday = birthday;
            this.isBirthdaySet = true;
            return this;
        }

        @JsonProperty("email")
        public Builder email(final @Nullable String email) {
            if (this.isEmailSet) {
                throw new IllegalStateException("Email already set");
            } else if (Objects.isNull(email)) {
                this.isEmailSet = true;
                return this;
            }
            final String e_mail = email.replaceAll("\\s", "").toLowerCase();
            if (!EmailValidator.getInstance().isValid(e_mail)) {
                throw new IllegalArgumentException("Email Invalid");
            }
            this.email = e_mail;
            this.isEmailSet = true;
            return this;
        }

        @JsonProperty("deathday")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT_STRING)
        @JsonDeserialize(using = LocalDateDeserializer.class)
        public Builder deathday(final @Nullable LocalDate deathday) {
            if (this.isDeathdaySet) {
                throw new IllegalStateException("Deathday already set");
            } else if (!this.isBirthdaySet) {
                throw new IllegalStateException("Birthday must be set before Deathday");
            } else if (Objects.isNull(deathday)) {
                this.isDeathdaySet = true;
                return this;
            } else if (deathday.isBefore(this.birthday)) {
                throw new IllegalArgumentException("Cannot Have Died Before Birth");
            } else if (deathday.isAfter(this.builderBegan)) {
                throw new IllegalArgumentException("Cannot Accept Predicted Death");
            }
            this.deathday = deathday;
            this.isDeathdaySet = true;
            return this;
        }

        @JsonProperty("phones")
        public Builder phones(final @Nullable Map<PhoneType, String> phones) {
            if (this.isPhonesSet) {
                throw new IllegalStateException("Phones already set");
            } else if (Objects.isNull(phones)) {
                this.isPhonesSet = true;
                return this;
            }
            this.phones = phones.entrySet().stream().collect(
                    Collectors.toMap(Map.Entry::getKey, entry -> getPhoneNumberIfValidElseThrow(entry.getValue())));
            this.isPhonesSet = true;
            return this;
        }

        @JsonProperty("address")
        public Builder address(final @Nullable List<String> address) {
            if (this.isAddressSet) {
                throw new IllegalStateException("Address already set");
            }
            this.address = address;
            this.isAddressSet = true;
            return this;
        }

        @JsonProperty("suffix")
        public Builder suffix(final @Nullable SuffixType suffix) {
            if (this.isSuffixSet) {
                throw new IllegalStateException("Suffix already set");
            }
            this.suffix = suffix;
            this.isSuffixSet = true;
            return this;
        }

        public MembersModel build() {
            if (isBuilt) {
                throw new IllegalStateException("Member already created");
            }
            this.isBuilt = true;
            if (Objects.isNull(this.deathday)) {
                return new MembersModel(this.firstName, this.lastName, this.birthday, this.email, null, this.phones,
                        this.address, this.suffix);
            } else {
                return new MembersModel(this.firstName, this.lastName, this.birthday, null, this.deathday, null, null,
                        this.suffix);
            }
        }
    }

    public static class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
        @Override
        public LocalDate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {
            return LocalDate.parse(jsonParser.getText(), DATE_FORMATTER);
        }
    }
}
