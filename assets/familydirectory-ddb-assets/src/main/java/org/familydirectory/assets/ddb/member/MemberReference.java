package org.familydirectory.assets.ddb.member;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.familydirectory.assets.ddb.enums.SuffixType;
import org.familydirectory.assets.ddb.models.member.MemberReferenceModel;
import org.familydirectory.assets.ddb.utils.LocalDateDeserializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static java.time.LocalDate.now;
import static org.apache.commons.text.WordUtils.capitalizeFully;
import static org.familydirectory.assets.ddb.utils.DdbUtils.DATE_FORMAT_STRING;

@JsonDeserialize(builder = MemberReference.Builder.class)
public final class MemberReference implements MemberReferenceModel {

    @JsonProperty("firstName")
    private final @NotNull String firstName;

    @JsonProperty("lastName")
    private final @NotNull String lastName;

    @JsonProperty("birthday")
    @JsonFormat(shape = STRING, pattern = DATE_FORMAT_STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private final @NotNull LocalDate birthday;

    @JsonProperty("suffix")
    private final @Nullable SuffixType suffix;

    /* DERIVED */

    @JsonIgnore
    private final @NotNull String fullName;
    @JsonIgnore
    private final @NotNull String birthdayString;
    @JsonIgnore
    private final @NotNull String primaryKey;

    private MemberReference(final @NotNull String firstName, final @NotNull String lastName,
                            final @NotNull LocalDate birthday, final @Nullable SuffixType suffix) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthday = birthday;
        this.suffix = suffix;
        // Derived
        this.fullName = MemberReferenceModel.super.getFullName();
        this.birthdayString = MemberReferenceModel.super.getBirthdayString();
        this.primaryKey = MemberReferenceModel.super.getPrimaryKey();
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

    /* DERIVED */

    @Override
    public @Nullable SuffixType getSuffix() {
        return this.suffix;
    }

    @Override
    public @NotNull String getFullName() {
        return this.fullName;
    }

    @Override
    public @NotNull String getBirthdayString() {
        return this.birthdayString;
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
        private SuffixType suffix = null;
        private boolean isSuffixSet = false;
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

        public MemberReference build() {
            this.checkBuildStatus();
            this.isBuilt = true;
            return new MemberReference(this.firstName, this.lastName, this.birthday, this.suffix);
        }
    }
}
