package org.familydirectory.assets.amplify.utility;

import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.amplify.AmplifyClient;
import software.amazon.awssdk.services.amplify.model.JobType;
import software.amazon.awssdk.services.amplify.model.ListAppsRequest;
import software.amazon.awssdk.services.amplify.model.ListBranchesRequest;
import software.amazon.awssdk.services.amplify.model.StartJobRequest;

public
enum AmplifyUtils {
    ;

    public static
    void appDeployment (final @NotNull AmplifyClient amplifyClient, final @NotNull String jobReason) {
        if (jobReason.isBlank()) {
            throw new IllegalArgumentException("Must Provide Job Reason");
        }
        final String appId = amplifyClient.listApps(ListAppsRequest.builder()
                                                                   .maxResults(1)
                                                                   .build())
                                          .apps()
                                          .getFirst()
                                          .appId();
        final String branchName = amplifyClient.listBranches(ListBranchesRequest.builder()
                                                                                .appId(appId)
                                                                                .maxResults(1)
                                                                                .build())
                                               .branches()
                                               .getFirst()
                                               .branchName();
        amplifyClient.startJob(StartJobRequest.builder()
                                              .appId(appId)
                                              .branchName(branchName)
                                              .jobReason(jobReason)
                                              .jobType(JobType.RELEASE)
                                              .build());
    }
}
