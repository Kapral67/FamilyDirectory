package org.familydirectory.assets.ddb.models.member;

import org.familydirectory.assets.ddb.enums.SuffixType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.familydirectory.assets.ddb.utils.DdbUtils.DATE_FORMATTER;
import static org.familydirectory.assets.ddb.utils.DdbUtils.isPersonAdult;

public interface MemberReferenceModel {
    @NotNull String getFirstName();

    @NotNull String getLastName();

    @NotNull LocalDate getBirthday();

    @Nullable SuffixType getSuffix();

    /* DERIVED */

    default @NotNull String getBirthdayString() {
        return DATE_FORMATTER.format(this.getBirthday());
    }

    default @NotNull String getFullName() {
        return (isNull(this.getSuffix())) ? format("%s %s", this.getFirstName(), this.getLastName()) :
                format("%s %s %s", this.getFirstName(), this.getLastName(), this.getSuffix().value());
    }

    default @NotNull String getPrimaryKey() {
        return sha256Hex(this.getFullName() + this.getBirthdayString());
    }

    default boolean isAdult() {
        return isPersonAdult(this.getBirthday());
    }

    /* RELATIONAL */

    default boolean equals(final @NotNull MemberReferenceModel ref) {
        return this.getFullName().equals(ref.getFullName()) && this.getBirthdayString().equals(ref.getBirthdayString());
    }
}
