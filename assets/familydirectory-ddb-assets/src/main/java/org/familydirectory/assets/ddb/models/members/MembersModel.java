package org.familydirectory.assets.ddb.models.members;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.commons.text.WordUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.familydirectory.assets.ddb.DdbType;
import org.immutables.value.Value;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.familydirectory.assets.ddb.DdbType.MAP;
import static org.familydirectory.assets.ddb.DdbType.STR;
import static org.familydirectory.assets.ddb.DdbType.STR_SET;

@Value.Immutable
public abstract class MembersModel {
    private static final Date PREHISTORIC =
            Date.from(LocalDate.now().minusYears(100).atStartOfDay(ZoneId.systemDefault()).toInstant());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    protected abstract String getFirstName();

    public final String firstName() {
        return WordUtils.capitalize(WordUtils.capitalize(this.getFirstName().trim()));
    }

    protected abstract String getLastName();

    public final String lastName() {
        return WordUtils.capitalize(WordUtils.capitalize(this.getLastName().trim()));
    }

    public abstract Date getBirthday();

    public final String birthday() {
        return DATE_FORMAT.format(this.getBirthday());
    }

    @Nullable
    protected abstract String getEmail();

    public final Optional<String> email() {
        return Optional.ofNullable(this.getEmail()).map(String::trim).map(WordUtils::capitalize);
    }

    @Nullable
    public abstract Date getDeathday();

    public final Optional<String> deathday() {
        return (Objects.nonNull(this.getDeathday())) ? Optional.of(DATE_FORMAT.format(this.getDeathday())) :
                Optional.empty();
    }

    @Nullable
    public abstract Map<PhoneType, String> getPhones();

    public final Optional<Map<String, AttributeValue>> getPhonesDdbMap() {
        return Optional.ofNullable(this.getPhones()).map(phoneMap -> phoneMap.entrySet().stream().collect(
                Collectors.toMap(entry -> entry.getKey().name(),
                        entry -> AttributeValue.builder().s(entry.getValue()).build())));
    }

    @Nullable
    protected abstract List<String> getAddress();

    public final Optional<List<String>> address() {
        return Optional.ofNullable(getAddress())
                .map(i -> i.stream().filter(Objects::nonNull).map(String::trim).map(WordUtils::capitalize).toList());
    }

    @Nullable
    public abstract SuffixType getSuffix();

    public final Optional<String> suffix() {
        return Optional.ofNullable(this.getSuffix()).map(SuffixType::value);
    }

    public final String getFullName() {
        final Optional<String> suffix = this.suffix();
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
        FIRST_NAME(STR), LAST_NAME(STR), SUFFIX(STR), BIRTHDAY(STR), DEATHDAY(STR), EMAIL(STR), PHONES(MAP), ADDRESS(
                STR_SET);

        private final DdbType ddbType;

        Params(final DdbType ddbType) {
            this.ddbType = ddbType;
        }

        public final DdbType ddbType() {
            return this.ddbType;
        }
    }
}
