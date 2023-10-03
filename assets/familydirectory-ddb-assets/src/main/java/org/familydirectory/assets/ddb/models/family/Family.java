package org.familydirectory.assets.ddb.models.family;

import java.util.List;
import org.immutables.value.Value;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
public
interface Family {
    @Nullable
    String getAncestor ();

    @Nullable
    String getSpouse ();

    @Nullable
    List<String> getDescendants ();
}
