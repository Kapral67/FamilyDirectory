package org.familydirectory.assets.amplify.utility;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.amplify.AmplifyClient;
import software.amazon.awssdk.services.amplify.model.App;
import software.amazon.awssdk.services.amplify.model.GetAppRequest;
import software.amazon.awssdk.services.amplify.model.JobType;
import software.amazon.awssdk.services.amplify.model.ListAppsRequest;
import software.amazon.awssdk.services.amplify.model.ListBranchesRequest;
import software.amazon.awssdk.services.amplify.model.StartJobRequest;
import software.amazon.awssdk.services.amplify.model.UpdateAppRequest;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public
enum AmplifyUtils {
    ;

    public static
    void appDeployment (final @NotNull AmplifyClient amplifyClient, final @NotNull String jobReason, final @Nullable String rootMemberSurname,
                        @Nullable String appId,
                        @Nullable String branchName)
    {
        if (jobReason.isBlank()) {
            throw new IllegalArgumentException("Must Provide Job Reason");
        }

        Map<String, String> environmentVariables = null;

        if (isNull(appId)) {
            final App app = amplifyClient.listApps(ListAppsRequest.builder()
                                                                  .maxResults(1)
                                                                  .build())
                                         .apps()
                                         .getFirst();
            appId = app.appId();
            if (nonNull(rootMemberSurname)) {
                environmentVariables = new HashMap<>(app.environmentVariables());
            }
        }

        if (isNull(branchName)) {
            branchName = amplifyClient.listBranches(ListBranchesRequest.builder()
                                                                       .appId(appId)
                                                                       .maxResults(1)
                                                                       .build())
                                      .branches()
                                      .getFirst()
                                      .branchName();
        }

        if (nonNull(rootMemberSurname)) {
            if (isNull(environmentVariables)) {
                environmentVariables = new HashMap<>(amplifyClient.getApp(GetAppRequest.builder()
                                                                                       .appId(appId)
                                                                                       .build())
                                                                  .app()
                                                                  .environmentVariables());
            }
            environmentVariables.remove(ReactEnvVar.SURNAME.toString());
            environmentVariables.put(ReactEnvVar.SURNAME.toString(), rootMemberSurname);

            amplifyClient.updateApp(UpdateAppRequest.builder()
                                                    .appId(appId)
                                                    .environmentVariables(Collections.unmodifiableMap(environmentVariables))
                                                    .build());
        }

        amplifyClient.startJob(StartJobRequest.builder()
                                              .appId(appId)
                                              .branchName(branchName)
                                              .jobReason(jobReason)
                                              .jobType(JobType.RELEASE)
                                              .build());
    }

    public
    enum ReactEnvVar {
        BACKEND_VERSION,
        REDIRECT_URI,
        API_DOMAIN,
        AUTH_DOMAIN,
        CLIENT_ID,
        SURNAME,
        AWS_REGION,
        USER_POOL_ID,
        AGE_OF_MAJORITY;

        public static final String PREFIX = "REACT_APP_";

        @Override
        @Contract(pure = true)
        @NotNull
        public final
        String toString () {
            return PREFIX + this.name();
        }
    }
}
