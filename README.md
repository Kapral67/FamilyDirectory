## Assets
`CDK` depends on the local `.mvn` maven-repo in `assets/DynamoDB`. This maven-repo is not tracked in version-control. To publish, go to `assets/DynamoDB` and run `./gradlew publish` if on a `*nix` system, or use the `gradlew.bat` if on Windows.
