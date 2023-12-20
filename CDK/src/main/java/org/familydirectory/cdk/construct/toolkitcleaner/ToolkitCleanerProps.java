package org.familydirectory.cdk.construct.toolkitcleaner;

import org.immutables.value.Value;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.events.Schedule;

@Value.Immutable
public
interface ToolkitCleanerProps {
    Duration DEFAULT_SCHEDULE_DURATION = Duration.days(1);

    /**
     * To Disable Automatic Cleaning, set to null
     *
     * @return {@link Schedule} or null
     */
    @Nullable
    @Value.Default
    default
    Schedule getSchedule () {
        return Schedule.rate(DEFAULT_SCHEDULE_DURATION);
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
