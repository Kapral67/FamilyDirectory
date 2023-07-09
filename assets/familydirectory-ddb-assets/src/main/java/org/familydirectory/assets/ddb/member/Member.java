package org.familydirectory.assets.ddb.member;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.familydirectory.assets.ddb.enums.PhoneType;
import org.familydirectory.assets.ddb.enums.SuffixType;
import org.familydirectory.assets.ddb.models.member.MemberModel;
import org.familydirectory.assets.ddb.utils.LocalDateDeserializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static java.time.LocalDate.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.text.WordUtils.capitalizeFully;
import static org.familydirectory.assets.ddb.utils.DdbUtils.DATE_FORMAT_STRING;
import static org.familydirectory.assets.ddb.utils.DdbUtils.EMAIL_VALIDATOR;
import static org.familydirectory.assets.ddb.utils.DdbUtils.isPersonAdult;
import static org.familydirectory.assets.ddb.utils.DdbUtils.normalizePhoneNumber;

@JsonDeserialize(builder = Member.Builder.class)
public final class Member extends MemberModel {
    @JsonProperty("firstName")
    private final @NotNull String firstName;

    @JsonProperty("lastName")
    private final @NotNull String lastName;

    @JsonProperty("birthday")
    @JsonFormat(shape = STRING, pattern = DATE_FORMAT_STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private final @NotNull LocalDate birthday;

    @JsonProperty("email")
    private final @Nullable String email;

    @JsonProperty("deathday")
    @JsonFormat(shape = STRING, pattern = DATE_FORMAT_STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private final @Nullable LocalDate deathday;

    @JsonProperty("phones")
    private final @Nullable Map<PhoneType, String> phones;

    @JsonProperty("address")
    private final @Nullable List<String> address;

    @JsonProperty("suffix")
    private final @Nullable SuffixType suffix;

    @JsonProperty("ancestor")
    private final @Nullable MemberReference ancestor;

    @JsonProperty("isAncestorSpouse")
    private final @Nullable Boolean isAncestorSpouse;

    /* DERIVED */

    @JsonIgnore
    private final @NotNull String fullName;

    @JsonIgnore
    private final @NotNull String birthdayString;

    @JsonIgnore
    private final @Nullable Map<String, AttributeValue> phonesDdbMap;

    @JsonIgnore
    private final @Nullable String deathdayString;

    @JsonIgnore
    private final @NotNull String primaryKey;

    private Member(final @NotNull String firstName, final @NotNull String lastName, final @NotNull LocalDate birthday,
                   final @Nullable String email, final @Nullable LocalDate deathday,
                   final @Nullable Map<PhoneType, String> phones, final @Nullable List<String> address,
                   final @Nullable SuffixType suffix, final @Nullable MemberReference ancestor,
                   final @Nullable Boolean isAncestorSpouse) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthday = birthday;
        this.email = email;
        this.deathday = deathday;
        this.phones = phones;
        this.address = address;
        this.suffix = suffix;
        this.ancestor = ancestor;
        this.isAncestorSpouse = isAncestorSpouse;
        // Derived
        this.fullName = super.getFullName();
        this.birthdayString = super.getBirthdayString();
        this.primaryKey = super.getPrimaryKey();
        this.deathdayString = super.getDeathdayString();
        this.phonesDdbMap = super.getPhonesDdbMap();
    }

    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    @Override
    public @NotNull String getFirstName() {
        return this.firstName;
    }

    @Override
    public @NotNull String getLastName() {
        return this.lastName;
    }

    @Override
    public @NotNull LocalDate getBirthday() {
        return this.birthday;
    }

    @Override
    public @Nullable String getEmail() {
        return this.email;
    }

    @Override
    public @Nullable LocalDate getDeathday() {
        return this.deathday;
    }

    @Override
    public @Nullable Map<PhoneType, String> getPhones() {
        return this.phones;
    }

    @Override
    public @Nullable List<String> getAddress() {
        return this.address;
    }

    @Override
    public @Nullable SuffixType getSuffix() {
        return this.suffix;
    }

    @Override
    public @Nullable MemberReference getAncestor() {
        return this.ancestor;
    }

    @Override
    public @Nullable Boolean getIsAncestorSpouse() {
        return this.isAncestorSpouse;
    }

    /* DERIVED */

    @Override
    public @NotNull String getBirthdayString() {
        return this.birthdayString;
    }

    @Override
    public @Nullable String getDeathdayString() {
        return this.deathdayString;
    }

    @Override
    public @Nullable Map<String, AttributeValue> getPhonesDdbMap() {
        return this.phonesDdbMap;
    }

    @Override
    public @NotNull String getFullName() {
        return this.fullName;
    }

    @Override
    public @NotNull String getPrimaryKey() {
        return this.primaryKey;
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
        private MemberReference ancestor = null;
        private boolean isAncestorSet = false;
        private Boolean isAncestorSpouse = null;
        private boolean isIsAncestorSpouseSet = false;
        private boolean isBuilt = false;

        public Builder() {
            this.builderBegan = now();
        }

        private void checkBuildStatus() {
            if (isBuilt) {
                throw new IllegalStateException("Member already created");
            }
        }

        @JsonProperty("firstName")
        public Builder firstName(final @NotNull String firstName) {
            this.checkBuildStatus();
            if (this.isFirstNameSet) {
                throw new IllegalStateException("First Name already set");
            } else if (firstName.isBlank()) {
                throw new IllegalArgumentException("First Name cannot be blank");
            }
            this.firstName = capitalizeFully(firstName.replaceAll("\\s", ""), '-');
            this.isFirstNameSet = true;
            return this;
        }

        @JsonProperty("lastName")
        public Builder lastName(final @NotNull String lastName) {
            this.checkBuildStatus();
            if (this.isLastNameSet) {
                throw new IllegalStateException("Last Name already set");
            } else if (lastName.isBlank()) {
                throw new IllegalArgumentException("Last Name cannot be blank");
            }
            this.lastName = capitalizeFully(lastName.replaceAll("\\s", ""), '-');
            this.isLastNameSet = true;
            return this;
        }

        @JsonProperty("birthday")
        @JsonFormat(shape = STRING, pattern = DATE_FORMAT_STRING)
        @JsonDeserialize(using = LocalDateDeserializer.class)
        public Builder birthday(final @NotNull LocalDate birthday) {
            this.checkBuildStatus();
            if (this.isBirthdaySet) {
                throw new IllegalStateException("Birthday already set");
            } else if (birthday.isAfter(this.builderBegan)) {
                throw new IllegalArgumentException("Birthday cannot be future");
            }
            this.birthday = birthday;
            this.isBirthdaySet = true;
            return this;
        }

        @JsonProperty("email")
        public Builder email(final @Nullable String email) {
            this.checkBuildStatus();
            if (this.isEmailSet) {
                throw new IllegalStateException("Email already set");
            } else if (isNull(email)) {
                this.isEmailSet = true;
                return this;
            }
            final String e_mail = email.replaceAll("\\s", "").toLowerCase();
            if (!EMAIL_VALIDATOR.isValid(e_mail)) {
                throw new IllegalArgumentException("Email Invalid");
            }
            this.email = e_mail;
            this.isEmailSet = true;
            return this;
        }

        @JsonProperty("deathday")
        @JsonFormat(shape = STRING, pattern = DATE_FORMAT_STRING)
        @JsonDeserialize(using = LocalDateDeserializer.class)
        public Builder deathday(final @Nullable LocalDate deathday) {
            this.checkBuildStatus();
            if (this.isDeathdaySet) {
                throw new IllegalStateException("Deathday already set");
            } else if (isNull(deathday)) {
                this.isDeathdaySet = true;
                return this;
            } else if (!this.isBirthdaySet) {
                throw new IllegalStateException("Birthday must be set before Deathday");
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
            this.checkBuildStatus();
            if (this.isPhonesSet) {
                throw new IllegalStateException("Phones already set");
            } else if (isNull(phones)) {
                this.isPhonesSet = true;
                return this;
            }
            this.phones = phones.entrySet().stream()
                    .collect(toMap(Map.Entry::getKey, entry -> normalizePhoneNumber(entry.getValue())));
            this.isPhonesSet = true;
            return this;
        }

        @JsonProperty("address")
        public Builder address(final @Nullable List<String> address) {
            this.checkBuildStatus();
            if (this.isAddressSet) {
                throw new IllegalStateException("Address already set");
            }
            this.address = address;
            this.isAddressSet = true;
            return this;
        }

        @JsonProperty("suffix")
        public Builder suffix(final @Nullable SuffixType suffix) {
            this.checkBuildStatus();
            if (this.isSuffixSet) {
                throw new IllegalStateException("Suffix already set");
            }
            this.suffix = suffix;
            this.isSuffixSet = true;
            return this;
        }

        @JsonProperty("ancestor")
        public Builder ancestor(final @Nullable MemberReference ancestor) {
            this.checkBuildStatus();
            if (this.isAncestorSet) {
                throw new IllegalStateException("Ancestor already set");
            } else if (!this.isBirthdaySet) {
                throw new IllegalStateException("Birthday must be set before setting Ancestor");
            } else if (isNull(ancestor) && this.isIsAncestorSpouseSet && nonNull(this.isAncestorSpouse)) {
                throw new IllegalArgumentException("Ancestor cannot be Null when IsAncestorSpouse is Not Null");
            } else if (nonNull(ancestor) && this.isIsAncestorSpouseSet && isNull(this.isAncestorSpouse)) {
                throw new IllegalArgumentException("Ancestor must be Null when IsAncestorSpouse is Null");
            } else if (nonNull(ancestor) && !ancestor.isAdult()) {
                throw new IllegalArgumentException("Ancestor must be an Adult");
            } else if (!isPersonAdult(this.birthday) && isNull(ancestor)) {
                throw new IllegalArgumentException("Ancestor cannot be Null for Minors");
            }
            this.ancestor = ancestor;
            this.isAncestorSet = true;
            return this;
        }

        @JsonProperty("isAncestorSpouse")
        public Builder isAncestorSpouse(final @Nullable Boolean isAncestorSpouse) {
            this.checkBuildStatus();
            if (this.isIsAncestorSpouseSet) {
                throw new IllegalStateException("IsAncestorSpouse already set");
            } else if (!this.isBirthdaySet) {
                throw new IllegalStateException("Birthday must be set before setting IsAncestorSpouse");
            } else if (isNull(isAncestorSpouse) && this.isAncestorSet && nonNull(this.ancestor)) {
                throw new IllegalArgumentException("IsAncestorSpouse cannot be Null when Ancestor is Not Null");
            } else if (nonNull(isAncestorSpouse) && this.isAncestorSet && isNull(this.ancestor)) {
                throw new IllegalArgumentException("IsAncestorSpouse must be Null when Ancestor is Null");
            } else if (nonNull(isAncestorSpouse) && isAncestorSpouse && !isPersonAdult(this.birthday)) {
                throw new IllegalArgumentException("IsAncestorSpouse must be False or Null for Minors");
            }
            this.isAncestorSpouse = isAncestorSpouse;
            this.isIsAncestorSpouseSet = true;
            return this;
        }

        public Member build() {
            this.checkBuildStatus();
            this.isBuilt = true;
            if (isNull(this.deathday)) {
                return new Member(this.firstName, this.lastName, this.birthday, this.email, null, this.phones,
                        this.address, this.suffix, this.ancestor, this.isAncestorSpouse);
            } else {
                return new Member(this.firstName, this.lastName, this.birthday, null, this.deathday, null, null,
                        this.suffix, this.ancestor, this.isAncestorSpouse);
            }
        }
    }
}
