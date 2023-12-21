package org.familydirectory.sdk.adminclient.enums;

import java.util.List;
import org.familydirectory.sdk.adminclient.enums.create.CreateOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;

public
enum Commands {
    CREATE(List.of(CreateOptions.values())),
    UPDATE(null),
    DELETE(null),
    TOGGLE_PDF_GENERATOR(null),
    TOOLKIT_CLEANER(null),
    EXIT(null);

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
