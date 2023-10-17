package org.familydirectory.assets.ddb.member;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.PhoneType;
import org.familydirectory.assets.ddb.enums.SuffixType;
import org.familydirectory.assets.ddb.models.member.MemberModel;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.assets.ddb.utils.LocalDateDeserializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static java.time.LocalDate.now;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.text.WordUtils.capitalizeFully;

@JsonDeserialize(builder = Member.Builder.class)
public final
class Member extends MemberModel {
    @JsonProperty("firstName")
    @NotNull
    private final String firstName;

    @JsonProperty("middleName")
    @Nullable
    private final String middleName;

    @JsonProperty("lastName")
    @NotNull
    private final String lastName;

    @JsonProperty("birthday")
    @JsonFormat(shape = STRING, pattern = DdbUtils.DATE_FORMAT_STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @NotNull
    private final LocalDate birthday;

    @JsonProperty("email")
    @Nullable
    private final String email;

    @JsonProperty("deathday")
    @JsonFormat(shape = STRING, pattern = DdbUtils.DATE_FORMAT_STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @Nullable
    private final LocalDate deathday;

    @JsonProperty("phones")
    @Nullable
    private final Map<PhoneType, String> phones;

    @JsonProperty("address")
    @Nullable
    private final List<String> address;

    @JsonProperty("suffix")
    @Nullable
    private final SuffixType suffix;

    @JsonIgnore
    @NotNull
    private final String fullName;

    @JsonIgnore
    @NotNull
    private final String birthdayString;

    @JsonIgnore
    @Nullable
    private final Map<String, AttributeValue> phonesDdbMap;

    @JsonIgnore
    @Nullable
    private final String deathdayString;

    private
    Member (final @NotNull String firstName, final @Nullable String middleName, final @NotNull String lastName, final @NotNull LocalDate birthday, final @Nullable String email,
            final @Nullable LocalDate deathday, final @Nullable Map<PhoneType, String> phones, final @Nullable List<String> address, final @Nullable SuffixType suffix)
    {
        super();
        this.firstName = requireNonNull(firstName);
        this.middleName = middleName;
        this.lastName = requireNonNull(lastName);
        this.birthday = requireNonNull(birthday);
        this.email = email;
        this.deathday = deathday;
        this.phones = phones;
        this.address = address;
        this.suffix = suffix;
        // Derived
        this.fullName = super.getFullName();
        this.birthdayString = super.getBirthdayString();
        this.deathdayString = super.getDeathdayString();
        this.phonesDdbMap = super.getPhonesDdbMap();
    }

    @Contract(" -> new")
    @NotNull
    public static
    Builder builder () {
        return new Builder();
    }

    @Override
    @Nullable
    public
    String getEmail () {
        return this.email;
    }

    @Override
    @Nullable
    public
    List<String> getAddress () {
        return this.address;
    }

    @Override
    @Nullable
    public
    Map<String, AttributeValue> getPhonesDdbMap () {
        return this.phonesDdbMap;
    }

    @Override
    @Nullable
    public
    Map<PhoneType, String> getPhones () {
        return this.phones;
    }

    @Override
    @NotNull
    public
    LocalDate getBirthday () {
        return this.birthday;
    }

    @Override
    @Nullable
    public
    String getDeathdayString () {
        return this.deathdayString;
    }

    @Override
    @Nullable
    public
    LocalDate getDeathday () {
        return this.deathday;
    }

    @Override
    @NotNull
    public
    String getBirthdayString () {
        return this.birthdayString;
    }

    @Override
    @NotNull
    public
    String getFullName () {
        return this.fullName;
    }

    @Override
    @NotNull
    public
    String getFirstName () {
        return this.firstName;
    }

    @Override
    @Nullable
    public
    String getMiddleName () {
        return this.middleName;
    }

    @Override
    @NotNull
    public
    String getLastName () {
        return this.lastName;
    }

    @Override
    @Nullable
    public
    SuffixType getSuffix () {
        return this.suffix;
    }

    public static final
    class Builder {
        private final LocalDate builderBegan;
        private String firstName = null;
        private boolean isFirstNameSet = false;
        private String middleName = null;
        private boolean isMiddleNameSet = false;
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

        public
        Builder () {
            super();
            this.builderBegan = now();
        }

        @JsonProperty("firstName")
        public
        Builder firstName (final @NotNull String firstName) {
            this.checkBuildStatus();
            if (this.isFirstNameSet) {
                throw new IllegalStateException("First Name already set");
            } else if (requireNonNull(firstName).isBlank()) {
                throw new IllegalArgumentException("First Name cannot be blank");
            }
            this.firstName = capitalizeFully(firstName.replaceAll("\\s", ""), '-');
            this.isFirstNameSet = true;
            return this;
        }

        private
        void checkBuildStatus () {
            if (this.isBuilt) {
                throw new IllegalStateException("Member already created");
            }
        }

        @JsonProperty("middleName")
        public
        Builder middleName (final @Nullable String middleName) {
            this.checkBuildStatus();
            if (this.isMiddleNameSet) {
                throw new IllegalStateException("Middle Name already set");
            } else if (isNull(middleName) || middleName.isBlank()) {
                this.isMiddleNameSet = true;
                return this;
            }
            this.middleName = capitalizeFully(middleName.replaceAll("\\s", ""), '-');
            this.isMiddleNameSet = true;
            return this;
        }

        @JsonProperty("lastName")
        public
        Builder lastName (final @NotNull String lastName) {
            this.checkBuildStatus();
            if (this.isLastNameSet) {
                throw new IllegalStateException("Last Name already set");
            } else if (requireNonNull(lastName).isBlank()) {
                throw new IllegalArgumentException("Last Name cannot be blank");
            }
            this.lastName = capitalizeFully(lastName.replaceAll("\\s", ""), '-');
            this.isLastNameSet = true;
            return this;
        }

        @JsonProperty("birthday")
        @JsonFormat(shape = STRING, pattern = DdbUtils.DATE_FORMAT_STRING)
        @JsonDeserialize(using = LocalDateDeserializer.class)
        public
        Builder birthday (final @NotNull LocalDate birthday) {
            this.checkBuildStatus();
            if (this.isBirthdaySet) {
                throw new IllegalStateException("Birthday already set");
            } else if (requireNonNull(birthday).isAfter(this.builderBegan)) {
                throw new IllegalArgumentException("Birthday cannot be future");
            }
            this.birthday = birthday;
            this.isBirthdaySet = true;
            return this;
        }

        @JsonProperty("email")
        public
        Builder email (final @Nullable String email) {
            this.checkBuildStatus();
            if (this.isEmailSet) {
                throw new IllegalStateException("Email already set");
            } else if (isNull(email)) {
                this.isEmailSet = true;
                return this;
            }
            final String e_mail = email.replaceAll("\\s", "")
                                       .toLowerCase();
            if (!DdbUtils.EMAIL_VALIDATOR.isValid(e_mail)) {
                throw new IllegalArgumentException("Email Invalid");
            }
            this.email = e_mail;
            this.isEmailSet = true;
            return this;
        }

        @JsonProperty("deathday")
        @JsonFormat(shape = STRING, pattern = DdbUtils.DATE_FORMAT_STRING)
        @JsonDeserialize(using = LocalDateDeserializer.class)
        public
        Builder deathday (final @Nullable LocalDate deathday) {
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
        public
        Builder phones (final @Nullable Map<PhoneType, String> phones) {
            this.checkBuildStatus();
            if (this.isPhonesSet) {
                throw new IllegalStateException("Phones already set");
            } else if (isNull(phones)) {
                this.isPhonesSet = true;
                return this;
            }
            this.phones = phones.entrySet()
                                .stream()
                                .collect(toMap(Map.Entry::getKey, entry -> DdbUtils.normalizePhoneNumber(entry.getValue())));
            this.isPhonesSet = true;
            return this;
        }

        @JsonProperty("address")
        public
        Builder address (final @Nullable List<String> address) {
            this.checkBuildStatus();
            if (this.isAddressSet) {
                throw new IllegalStateException("Address already set");
            }
            this.address = address;
            this.isAddressSet = true;
            return this;
        }

        @JsonProperty("suffix")
        public
        Builder suffix (final @Nullable SuffixType suffix) {
            this.checkBuildStatus();
            if (this.isSuffixSet) {
                throw new IllegalStateException("Suffix already set");
            }
            this.suffix = suffix;
            this.isSuffixSet = true;
            return this;
        }

        @Contract(" -> new")
        @NotNull
        public
        Member build () {
            this.checkBuildStatus();
            this.isBuilt = true;
            if (isNull(this.deathday)) {
                return new Member(this.firstName, this.middleName, this.lastName, this.birthday, this.email, null, this.phones, this.address, this.suffix);
            } else {
                return new Member(this.firstName, this.middleName, this.lastName, this.birthday, null, this.deathday, null, null, this.suffix);
            }
        }
    }
}
