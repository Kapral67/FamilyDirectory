package org.familydirectory.assets.ddb.models.families;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface FamiliesModel {
    @Nullable String getSpouse();

    @Nullable List<String> getDescendants();
}
