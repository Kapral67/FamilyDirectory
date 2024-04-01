package org.familydirectory.sdk.adminclient.enums;

import org.familydirectory.sdk.adminclient.enums.cognito.CognitoManagementOptions;
import org.familydirectory.sdk.adminclient.enums.create.CreateOptions;
import org.familydirectory.sdk.adminclient.enums.flags.Flags;
import org.jetbrains.annotations.Nullable;

public
enum Commands {
    CREATE(CreateOptions.values()),
    UPDATE(null),
    DELETE(null),
    TOGGLE_PDF_GENERATOR(null),
    COGNITO_MANAGEMENT(CognitoManagementOptions.values()),
    TOOLKIT_CLEANER(null),
    FLAGS(Flags.values()),
    EXIT(null);

    @Nullable
    private final Enum<?>[] options;

    Commands (final @Nullable Enum<?>[] options) {
        this.options = options;
    }

    @Nullable
    public final
    Enum<?>[] options () {
        return this.options;
    }
}
