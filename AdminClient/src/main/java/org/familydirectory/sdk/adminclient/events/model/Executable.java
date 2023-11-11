package org.familydirectory.sdk.adminclient.events.model;

import software.amazon.awssdk.utils.SdkAutoCloseable;

public
interface Executable extends SdkAutoCloseable {
    void execute ();
}
