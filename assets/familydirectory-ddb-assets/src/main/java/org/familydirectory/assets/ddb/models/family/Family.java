package org.familydirectory.assets.ddb.models.family;

import org.immutables.value.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Value.Immutable
public interface Family {
    @Nullable String getSpouse();

    @Nullable List<String> getDescendants();
}
