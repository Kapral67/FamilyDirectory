package org.familydirectory.assets.ddb.member;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.PhoneType;
import org.familydirectory.assets.ddb.enums.SuffixType;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
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
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toUnmodifiableMap;
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
    }

    @NotNull
    public static
    Member convertDdbMap (final @NotNull Map<String, AttributeValue> memberMap) {
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
                        if (!av.ss()
                               .isEmpty())
                        {
                            memberBuilder.address(av.ss());
                        }
                    }
                    case PHONES -> {
                        if (!av.m()
                               .isEmpty())
                        {
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
    LocalDate getDeathday () {
        return this.deathday;
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
            this.firstName = capitalizeFully(firstName.replaceAll(DdbUtils.NAME_VALIDATOR_REGEX, ""), '-');
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
            this.middleName = capitalizeFully(middleName.replaceAll(DdbUtils.NAME_VALIDATOR_REGEX, ""), '-');
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
            this.lastName = capitalizeFully(lastName.replaceAll(DdbUtils.NAME_VALIDATOR_REGEX, ""), '-');
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
            } else if (isNull(email) || email.isBlank()) {
                this.isEmailSet = true;
                return this;
            } else if (email.contains(String.valueOf(DAGGER))) {
                throw new IllegalArgumentException("Forbidden Character in Email");
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
            phones.forEach((k, v) -> {
                if (v.contains(String.valueOf(DAGGER))) {
                    throw new IllegalArgumentException("Forbidden Character in %s Phone".formatted(k.name()));
                }
            });
            this.phones = phones.entrySet()
                                .stream()
                                .filter(e -> !e.getValue()
                                               .isBlank())
                                .collect(toUnmodifiableMap(Map.Entry::getKey, entry -> DdbUtils.normalizePhoneNumber(entry.getValue())));
            this.isPhonesSet = true;
            return this;
        }

        @JsonProperty("address")
        public
        Builder address (final @Nullable List<String> address) {
            this.checkBuildStatus();
            if (this.isAddressSet) {
                throw new IllegalStateException("Address already set");
            } else if (isNull(address)) {
                this.isAddressSet = true;
                return this;
            } else if (address.size() != 2) {
                throw new IllegalArgumentException("Address Must Be Exactly Two Lines");
            }
            address.forEach(s -> {
                if (s.contains(String.valueOf(DAGGER))) {
                    throw new IllegalArgumentException("Forbidden Character in Address");
                }
            });
            this.address = address.stream()
                                  .filter(Predicate.not(String::isBlank))
                                  .map(String::trim)
                                  .map(s -> s.replaceAll("\\s", " "))
                                  .toList();
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
