package org.familydirectory.assets.ddb.models.family;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toUnmodifiableSet;

public
record FamilyRecord(@NotNull UUID id, @NotNull UUID ancestor, UUID spouse, Set<UUID> descendants) {
    public FamilyRecord {
        requireNonNull(id);
        requireNonNull(ancestor);
        descendants = ofNullable(descendants).orElse(emptySet());
    }

    @NotNull
    public static FamilyRecord convertDdbMap(final @NotNull Map<String, AttributeValue> familyMap) {
        final UUID id = ofNullable(familyMap.get(FamilyTableParameter.ID.jsonFieldName()))
            .map(AttributeValue::s)
            .map(UUID::fromString)
            .orElseThrow();
        final UUID ancestor = ofNullable(familyMap.get(FamilyTableParameter.ANCESTOR.jsonFieldName()))
            .map(AttributeValue::s)
            .map(UUID::fromString)
            .orElseThrow();
        final UUID spouse = ofNullable(familyMap.get(FamilyTableParameter.SPOUSE.jsonFieldName()))
            .map(AttributeValue::s)
            .map(UUID::fromString)
            .orElse(null);
        final Set<UUID> descendants = ofNullable(familyMap.get(FamilyTableParameter.DESCENDANTS.jsonFieldName()))
            .map(AttributeValue::ss)
            .stream()
            .flatMap(List::stream)
            .map(UUID::fromString)
            .collect(toUnmodifiableSet());
        return new FamilyRecord(id, ancestor, spouse, descendants);
    }

    @Override
    public
    boolean equals (final Object o) {
        if (this == o) {
            return true;
        } else if (o == null || !this.getClass().equals(o.getClass())) {
            return false;
        }
        return this.id().equals(((FamilyRecord)o).id());
    }

    @Override
    public
    int hashCode () {
        return this.id.hashCode();
    }
}
