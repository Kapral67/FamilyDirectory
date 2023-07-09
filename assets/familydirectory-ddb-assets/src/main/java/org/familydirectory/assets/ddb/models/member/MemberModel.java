package org.familydirectory.assets.ddb.models.member;

import org.familydirectory.assets.ddb.enums.PhoneType;
import org.familydirectory.assets.ddb.member.MemberReference;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toMap;
import static org.familydirectory.assets.ddb.utils.DdbUtils.DATE_FORMATTER;

public abstract class MemberModel implements MemberReferenceModel {
    public abstract @Nullable String getEmail();

    public abstract @Nullable LocalDate getDeathday();

    public abstract @Nullable Map<PhoneType, String> getPhones();

    public abstract @Nullable List<String> getAddress();

    public abstract @Nullable MemberReference getAncestor();

    public abstract @Nullable Boolean getIsAncestorSpouse();

    public @Nullable String getDeathdayString() {
        return (isNull(this.getDeathday())) ? null : DATE_FORMATTER.format(this.getDeathday());
    }

    public @Nullable Map<String, AttributeValue> getPhonesDdbMap() {
        return (isNull(this.getPhones())) ? null : this.getPhones().entrySet().stream().collect(
                toMap(entry -> entry.getKey().name(), entry -> AttributeValue.builder().s(entry.getValue()).build()));
    }
}
