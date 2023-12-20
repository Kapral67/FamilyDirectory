package org.familydirectory.cdk.constructs.toolkitcleaner;

import org.immutables.value.Value;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.events.Schedule;

@Value.Immutable
public
interface ToolkitCleanerProps {
    Schedule DEFAULT_SCHEDULE = Schedule.rate(Duration.days(30));

    /**
     * To Disable Automatic Cleaning, set to null
     *
     * @return {@link Schedule} or null
     */
    @Nullable
    @Value.Default
    default
    Schedule getSchedule () {
        return DEFAULT_SCHEDULE;
    }

    @Value.Default
    default
    boolean getDryRun () {
        return false;
    }

    @Nullable
    @Value.Default
    default
    Duration getRetainAssetsNewerThan () {
        return null;
    }
}
