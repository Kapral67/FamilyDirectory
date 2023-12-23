package org.familydirectory.sdk.adminclient.records;

import org.familydirectory.sdk.adminclient.enums.ByteDivisor;
import org.jetbrains.annotations.NotNull;

public
record ToolkitCleanerResponse(long deletedItems, long reclaimedBytes) {
    public
    double reclaimed (final @NotNull ByteDivisor byteDivisor) {
        if (byteDivisor.equals(ByteDivisor.NONE)) {
            return (double) this.reclaimedBytes();
        }
        return (double) this.reclaimedBytes() / byteDivisor.divisor();
    }
}
