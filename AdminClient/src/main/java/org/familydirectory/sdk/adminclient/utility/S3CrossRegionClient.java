package org.familydirectory.sdk.adminclient.utility;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.s3.S3Client;

public final
class S3CrossRegionClient implements S3Client {

    private
    S3CrossRegionClient () {
        super();
    }

    @Contract("-> new")
    @NotNull
    public static
    S3Client create () {
        return S3Client.builder()
                       .crossRegionAccessEnabled(true)
                       .build();
    }

    @Override
    public
    String serviceName () {
        return SERVICE_NAME;
    }

    @Override
    public
    void close () {
    }
}
