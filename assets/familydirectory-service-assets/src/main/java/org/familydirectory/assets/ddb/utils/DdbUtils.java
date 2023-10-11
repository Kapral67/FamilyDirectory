package org.familydirectory.assets.ddb.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.apache.commons.validator.routines.EmailValidator;
import org.jetbrains.annotations.NotNull;
import static com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL;
import static java.time.LocalDate.now;
import static java.time.Period.between;
import static java.time.format.DateTimeFormatter.ofPattern;

public final
class DdbUtils {
    public static final String DATE_FORMAT_STRING = "yyyy-MM-dd";
    public static final DateTimeFormatter DATE_FORMATTER = ofPattern(DATE_FORMAT_STRING);
    public static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();
    public static final EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance();
    public static final int AGE_OF_MAJORITY = 18;

    private
    DdbUtils () {
    }

    public static @NotNull
    String normalizePhoneNumber (final @NotNull String uncheckedPhoneNumber) {
        final String errorMessage = "Invalid Phone Number: '%s'".formatted(uncheckedPhoneNumber);
        String region = (uncheckedPhoneNumber.contains("+"))
                ? null
                : "US";
        final Phonenumber.PhoneNumber phoneNumber;
        try {
            phoneNumber = PHONE_NUMBER_UTIL.parse(uncheckedPhoneNumber, region);
        } catch (final NumberParseException e) {
            throw new IllegalArgumentException(errorMessage, e);
        }
        if (!PHONE_NUMBER_UTIL.isValidNumber(phoneNumber)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return PHONE_NUMBER_UTIL.format(phoneNumber, INTERNATIONAL);
    }

    public static
    boolean isPersonAdult (final @NotNull LocalDate birthday) {
        return (between(birthday, now()).getYears() >= AGE_OF_MAJORITY);
    }
}
