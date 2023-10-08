package org.familydirectory.assets.ddb.models.member;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.familydirectory.assets.ddb.enums.PhoneType;
import org.familydirectory.assets.ddb.enums.SuffixType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.familydirectory.assets.ddb.utils.DdbUtils.DATE_FORMATTER;
import static org.familydirectory.assets.ddb.utils.DdbUtils.isPersonAdult;

public abstract
class MemberModel {
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
                      .collect(toMap(entry -> entry.getKey()
                                                   .name(), entry -> AttributeValue.fromS(entry.getValue())));
    }

    public abstract @Nullable
    Map<PhoneType, String> getPhones ();

    public
    boolean isAdult () {
        return isPersonAdult(this.getBirthday());
    }

    public abstract @NotNull
    LocalDate getBirthday ();

    public @NotNull
    String getKey () {
        return sha256Hex((isNull(this.getDeathdayString()))
                                 ? format("%s %s", this.getFullName(), this.getBirthdayString())
                                 : format("%s %s %s", this.getFullName(), this.getBirthdayString(), this.getDeathdayString()));
    }

    public @Nullable
    String getDeathdayString () {
        return Optional.ofNullable(this.getDeathday())
                       .map(DATE_FORMATTER::format)
                       .orElse(null);
    }

    public abstract @Nullable
    LocalDate getDeathday ();

    public @NotNull
    String getBirthdayString () {
        return DATE_FORMATTER.format(this.getBirthday());
    }

    public @NotNull
    String getFullName () {
        final StringBuilder fullName = new StringBuilder();
        fullName.append(format("%s ", this.getFirstName()));
        if (nonNull(this.getMiddleName())) {
            fullName.append(format("%s ", this.getMiddleName()));
        }
        fullName.append(format("%s", this.getLastName()));
        if (nonNull(this.getSuffix())) {
            fullName.append(format(" %s", this.getSuffix()
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
}
