package org.familydirectory.sdk.adminclient.events.model;

import software.amazon.awssdk.utils.SdkAutoCloseable;

public
interface EventHelper extends Runnable, SdkAutoCloseable {
    @Override
    default
    void close () {
    }
}
