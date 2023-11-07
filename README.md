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

    6. `ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_NAME`

        - The name of your repository on GitHub containing the UI components
        - **==TODO==** *Add Fork Instructions*

    7. `ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_OWNER`

        - Your GitHub Username

    8. `ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_OAUTH_TOKEN`

        - Fine-grained GitHub Token that ONLY gives access to `${ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_OWNER}/${ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_NAME}` and only allows read/write permissions for repository hooks
        - **==TODO==** *Add More Detailed Instructions*
        - **==TODO==** *Add Disclaimer About How This Token Is Used*

    9. `ORG_FAMILYDIRECTORY_ROOT_MEMBER_ID`
    
        - The `ROOT MEMBER` of this FamilyDirectory must be known and so an Environment Variable is set for it.
        - A good value for this variable is `"00000000-0000-0000-0000-000000000000"`
    
    10. `CDK_DEFAULT_ACCOUNT`
    
        - The AWS Account Id
    
    11. `CDK_DEFAULT_REGION`
    
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

        - Before moving forward, login to the aws console and navigate to Route53, click on Hosted Zones in the right side-bar, then click on the Hosted Zone

        - Here, you need to copy the NS records for `${ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME}` and apply them at your
          registrar

            - If `${ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME}` is a subdomain (e.g. `subdomain.example.com`):

                - Make sure that you are applying these records for the subdomain, not the root domain

                - *Note that some DNS Providers/Registrars don't work very well for delegating domains. I had success
                  when using Cloudflare as the Nameserver for my root domain. For these purposes, Cloudflare is free if
                  you already own the root domain.*

        - You will also need to set a temporary A record for `${ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME}` in your hosted zone's record table select **Create record** button and leave the record name blank, make sure Record type is **A** and set the TTL to something short like 300 seconds. The routing policy should be **Simple routing** and the Value of the record can be anything (this is a dummy record so that Route53 allows us to attach A records to subdomains and is overwritten by the `FamilyDirectoryAmplifyStack`). You can use the value `93.184.216.34` (which is the A record value of `example.com` at the time of writing)

        - Wait Until DNS Propagates, Then Continue

    2. Now, deploy the `FamilyDirectoryApiGatewayStack`
        - This stack should cause all stacks except `FamilyDirectoryAmplifyStack` to deploy along with because they are all dependents of this stack.
        - Since there lots of artifacts being deployed and dns validation occurring on some stacks, this will take awhile
    
    3. **==TODO==** *Need Instructions for adding Root Member to Database before deploying `FamilyDirectoryAmplifyStack`*
    

***TODO** Finish Deployment Order*