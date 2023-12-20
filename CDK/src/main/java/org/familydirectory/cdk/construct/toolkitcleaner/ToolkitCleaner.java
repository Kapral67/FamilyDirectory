package org.familydirectory.cdk.construct.toolkitcleaner;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecr.assets.DockerImageAssetProps;
import software.amazon.awscdk.services.s3.assets.Asset;
import software.amazon.awscdk.services.s3.assets.AssetProps;
import software.constructs.Construct;

public final
class ToolkitCleaner extends Construct {
    private static final Path BASE_PATH = Paths.get(System.getProperty("user.dir"))
                                               .resolve(Paths.get("assets", "toolkit-cleaner", "docker"))
                                               .toAbsolutePath()
                                               .normalize();
    private static final String FILE_ASSET_RESOURCE_ID = "FileAsset";
    private static final String DOCKER_IMAGE_ASSET_RESOURCE_ID = "DockerImageAsset";
    private static final String FILE_ASSET_PATH = BASE_PATH.resolve(Paths.get("dummy.txt"))
                                                           .toString();
    private static final String DOCKER_IMAGE_ASSET_PATH = BASE_PATH.toString();

    public
    ToolkitCleaner (final @NotNull Construct scope, final @NotNull String id, final @NotNull ToolkitCleanerProps props) {
        super(scope, id);

        final Asset fileAsset = new Asset(this, FILE_ASSET_RESOURCE_ID, AssetProps.builder()
                                                                                  .path(FILE_ASSET_PATH)
                                                                                  .build());

        final DockerImageAsset dockerImageAsset = new DockerImageAsset(this, DOCKER_IMAGE_ASSET_RESOURCE_ID, DockerImageAssetProps.builder()
                                                                                                                                  .directory(DOCKER_IMAGE_ASSET_PATH)
                                                                                                                                  .build());
    }
}
