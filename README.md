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

        - Should be an externally-managed email address that captures responses to cognito *no-reply* emails (
          e.g. `familydirectory@gmail.com`)

    5. `ORG_FAMILYDIRECTORY_SES_MAIL_FROM_SUBDOMAIN_NAME`

        - Should be the subdomain of `ORG_FAMILY_DIRECTORY_HOSTED_ZONE_NAME` where emails are sent from (e.g. `support`)

    6. `ORG_FAMILYDIRECTORY_ROOT_MEMBER_ID`

        - The `ROOT MEMBER` of this FamilyDirectory must be known and so an Environment Variable is set for it.
        - A good value for this variable is `"00000000-0000-0000-0000-000000000000"`

    7. `CDK_DEFAULT_ACCOUNT`

        - The AWS Account Id

    8. `CDK_DEFAULT_REGION`

        - The AWS Region

3. Now is a good time to bootstrap you're aws account for cdk if you haven't already

    - This only needs to be done once before the first deployment:

        - `cdk bootstrap "aws://$CDK_DEFAULT_ACCOUNT/$CDK_DEFAULT_REGION"`

        - `cdk bootstrap "aws://$CDK_DEFAULT_ACCOUNT/us-east-1` (Only needed if your `$CDK_DEFAULT_REGION` is
          not `us-east-1`)


4. If on a `*nix` system, you can use the `stage.bash` script to build this project in the correct order

    - If on Windows or other `non *nix` system, you will have to use the `stage.bash` script as a guide to build this
      project manually

        - Do Not Attempt to use `stage.bash` on a system that does not use `/` as
          the [name-separator character](https://docs.oracle.com/javase/8/docs/api/java/io/File.html#separatorChar)

5. Now you can synth and deploy

    1. First, deploy the `FamilyDirectoryDomainStack` solely (e.g. `cdk deploy FamilyDirectoryDomainStack`)

        - Before moving forward, login to the aws console and navigate to Route53

        - Here, you need to copy the NS records for `${ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME}` and apply them at your
          registrar

            - If `${ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME}` is a subdomain (e.g. `subdomain.example.com`):

                - Make sure that you are applying these records for the subdomain, not the root domain

                - *Note that some DNS Providers/Registrars don't work very well for delegating domains. I had success
                  when using Cloudflare as the Nameserver for my root domain. For these purposes, Cloudflare is free if
                  you already own the root domain.*

        - Wait Until DNS Propagates, Then Continue

    2. Now, deploy the `FamilyDirectorySesStack` solely (e.g. `cdk deploy FamilyDirectorySesStack`)

        - Since the domain used for SES is attached to the HostedZone defined in `FamilyDirectoryDomainStack`, DNS
          records are created automatically

        - Wait Until DNS Propagates, Then Continue

    3. Now, deploy the `FamilyDirectoryCognitoStack`, this stack should automatically deploy
       the `FamilyDirectoryCognitoUsEastOneStack` as it depends on it

        - After this stack deploys, you'll have to wait around an hour, but then you can view your Cognito page
          (
          see [Step 3](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-pools-add-custom-domain.html#cognito-user-pools-add-custom-domain-console-step-3))

***TODO** Finish Deployment Order*