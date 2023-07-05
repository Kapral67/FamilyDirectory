package org.familydirectory.assets.ddb.models.members;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.commons.text.WordUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.familydirectory.assets.ddb.DdbType;
import org.immutables.value.Value;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.familydirectory.assets.ddb.DdbType.MAP;
import static org.familydirectory.assets.ddb.DdbType.STR;
import static org.familydirectory.assets.ddb.DdbType.STR_SET;

@JsonDeserialize(using = MembersModel.Deserializer.class)
@JsonSerialize(as = ImmutableMembersModel.class)
@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)
@Value.Immutable
public abstract class MembersModel {
    public MembersModel() {
        super();
    }
    private static final Date PREHISTORIC =
            Date.from(LocalDate.of(1915, 7, 11).atStartOfDay(ZoneId.systemDefault()).toInstant());
    private static final String DATE_FORMAT_STRING = "yyyy-MM-dd";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STRING);

    public abstract String getFirstName();

    public abstract String getLastName();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT_STRING)
    public abstract Date getBirthday();

    public abstract Optional<String> getEmail();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT_STRING)
    public abstract Optional<Date> getDeathday();

    public abstract Optional<Map<PhoneType, String>> getPhones();

    public abstract Optional<List<String>> getAddress();

    public abstract Optional<SuffixType> getSuffix();

    @Value.Check
    protected MembersModel normalize() throws IllegalStateException {
        ImmutableMembersModel.Builder cleaned = ImmutableMembersModel.builder();

        if (this.getFirstName().isBlank()) {
            throw new IllegalStateException("First Name Cannot Be Blank");
        }
        cleaned.firstName(WordUtils.capitalize(this.getFirstName().trim()));

        if (this.getLastName().isBlank()) {
            throw new IllegalStateException("Last Name Cannot Be Blank");
        }
        cleaned.lastName(WordUtils.capitalize(this.getLastName().trim()));

        if (this.getBirthday().after(Date.from(Instant.now()))) {
            throw new IllegalStateException("Birthday Cannot Be Future");
        } else if (this.getBirthday().before(PREHISTORIC)) {
            throw new IllegalStateException("Birthday Cannot Be PreHistoric");
        }
        cleaned.birthday(this.getBirthday());

        if (this.getEmail().isPresent() && !EmailValidator.getInstance().isValid(this.getEmail().get())) {
            throw new IllegalStateException("Email Invalid");
        }
        cleaned.email(this.getEmail().map(String::trim).map(WordUtils::capitalize));

        if (this.getDeathday().isPresent()) {
            if (this.getDeathday().get().before(this.getBirthday()))
                throw new IllegalStateException("Cannot Have Died Before Birth");
            if (this.getDeathday().get().after(Date.from(Instant.now())))
                throw new IllegalStateException("Cannot Accept Predicted Death");
        }
        cleaned.deathday(this.getDeathday());

        if (this.getPhones().isPresent()) {
            for (final Map.Entry<PhoneType, String> entry : this.getPhones().get().entrySet()) {
                Phonenumber.PhoneNumber phoneNumber = new Phonenumber.PhoneNumber().setRawInput(entry.getValue());
                if (!PhoneNumberUtil.getInstance().isValidNumber(phoneNumber)) {
                    throw new IllegalStateException(
                            String.format("Invalid Phone Number: '%s' For '%s' Field", entry.getValue(),
                                    entry.getKey().name()));
                }
            }
        }
        cleaned.phones(this.getPhones());
        cleaned.address(this.getAddress());
        cleaned.suffix(this.getSuffix());

        return cleaned.build();
    }

    @Value.Lazy
    public String getBirthdayString() {
        return DATE_FORMAT.format(this.getBirthday());
    }

    /* DyanmoDB Artifacts */

    @Value.Lazy
    public Optional<String> getDeathdayString() {
        return this.getDeathday().map(DATE_FORMAT::format);
    }

    @Value.Lazy
    public Optional<Map<String, AttributeValue>> getPhonesDdbMap() {
        return this.getPhones().map(phoneMap -> phoneMap.entrySet().stream().collect(
                Collectors.toMap(entry -> entry.getKey().name(),
                        entry -> AttributeValue.builder().s(entry.getValue()).build())));
    }

    @Value.Derived
    public String getFullName() {
        final Optional<String> suffix = this.getSuffix().map(SuffixType::value);
        if (suffix.isPresent()) {
            return String.format("%s %s %s", this.getFirstName(), this.getLastName(), suffix.get());
        } else {
            return String.format("%s %s", this.getFirstName(), this.getLastName());
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

    /*
    * Not to be invoked directly. Leave for Jackson.
    * */
    public static class Deserializer extends JsonDeserializer<MembersModel> {
        @Override
        @Deprecated
        public MembersModel deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = parser.getCodec().readTree(parser);
            ImmutableMembersModel.Json json = mapper.treeToValue(node, ImmutableMembersModel.Json.class);
            return ImmutableMembersModel.fromJson(json);
        }
    }
}
