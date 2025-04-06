package org.familydirectory.sdk.adminclient.enums;

import org.familydirectory.sdk.adminclient.enums.cognito.CognitoManagementOptions;
import org.familydirectory.sdk.adminclient.enums.create.CreateOptions;
import org.jetbrains.annotations.Nullable;

public
enum Commands {
    BACKFILL(null),
    CREATE(CreateOptions.values()),
    UPDATE(null),
    DELETE(null),
    TOGGLE_PDF_GENERATOR(null),
    COGNITO_MANAGEMENT(CognitoManagementOptions.values()),
    AMPLIFY_DEPLOYMENT(null),
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
