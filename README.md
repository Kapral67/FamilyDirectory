## Family Directory

### Steps to Deploy

1. Your AWS account and region info must be stored in
   an [aws configuration/credential file](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)

    - The easiest way to do this is to use [aws cli](https://aws.amazon.com/cli/)
      or [aws toolkit plugin for IntelliJ](https://plugins.jetbrains.com/plugin/11349-aws-toolkit)

2. Next you need to define the following environment variables:

    1. `ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME`

        - Should be a Fully-Qualified-Domain-Name (e.g. `example.com`, `aws.example.com`, etc.) whose DNS should be
          controlled by Route53

    2. `ORG_FAMILYDIRECTORY_API_SUBDOMAIN_NAME`

        - Should be the subdomain of `ORG_FAMILY_DIRECTORY_HOSTED_ZONE_NAME` where api endpoints are accessed (
          e.g. `api`)

    3. `ORG_FAMILYDIRECTORY_COGNITO_SUBDOMAIN_NAME`

        - Should be the subdomain of `ORG_FAMILY_DIRECTORY_HOSTED_ZONE_NAME` where authentication is handled (
          e.g. `auth`)

    4. `ORG_FAMILYDIRECTORY_COGNITO_REPLY_TO_EMAIL_ADDRESS`

        - Should be an externally-managed email address that captures responses to cognito *noreply* emails (
          e.g. `familydirectory@gmail.com`)

3. Now is a good time to bootstrap you're aws account for cdk if you haven't already

    - This only needs to be done once before the first deployment

4. If on a `*nix` system, you can use the `stage.bash` script to build this project in the correct order

    - If on Windows or other `non *nix` system, you will have to use the `stage.bash` script as a guide to build this
      project manually

        - Do Not Attempt to use `stage.bash` on a system that does not use `/` as
          the [name-separator character](https://docs.oracle.com/javase/8/docs/api/java/io/File.html#separatorChar)

5. Now you can synth and deploy

    - **TODO** Add Deployment Order