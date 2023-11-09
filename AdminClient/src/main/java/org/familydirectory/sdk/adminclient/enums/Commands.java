package org.familydirectory.sdk.adminclient.enums;

import java.util.Arrays;
import java.util.List;
import org.familydirectory.sdk.adminclient.enums.create.CreateOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;

public
enum Commands {
    CREATE(Arrays.asList(CreateOptions.values())),
    UPDATE(null),
    DELETE(null);

    @Nullable
    private final List<Enum<?>> options;

    Commands (final @Nullable List<Enum<?>> options) {
        this.options = options;
    }

    @NotNull
    public final
    List<Enum<?>> options () {
        return isNull(this.options)
                ? emptyList()
                : this.options;
    }
}
