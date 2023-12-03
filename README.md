## Family Directory

### Steps to Deploy

1. Your AWS account and region info must be stored in
   an [aws configuration & credential file](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)

    - The easiest way to get the access key and secret key for your credential file:

        1. Go to AWS Console and Find IAM Service
        2. Access management -> Users, then Create user
        3. This user does not need access to the AWS Management Console
        4. When setting permissions, Attach policies directly
        5. Add the `AdministratorAccess` Policy
        6. Once the User is created, go to their Security credentials tab and Create an access key
        7. Disable this access key after deployment, or after performing actions with the `AdminClient`

    - The easiest way to create the credential and config file is to use [aws cli](https://aws.amazon.com/cli/)
      or [aws toolkit plugin for IntelliJ](https://plugins.jetbrains.com/plugin/11349-aws-toolkit)

        - The config file at a location like `~/.aws/config` should look like (replace `us-east-1` with whatever your
          desired region):
        ```
      [default]
		region = us-east-1
		output = json
        ```

        - The credential file at a location like `~/.aws/credentials` should look like:
        ```
      [default]
		aws_access_key_id = YOUR_ACCESS_KEY_HERE
		aws_secret_access_key = YOUR_SECRET_KEY_HERE
        ```

2. Next you need to define the following environment variables:

    1. `AWS_ACCOUNT_ID`
        - The AWS Account Id
        - In AWS Console, at the top right, click the drop-down to see your Account ID
            - Set this environment variable to that number excluding any dashes

    2. `AWS_REGION`
        - The AWS Region (See Step 1)

    3. `ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME`
        - Should be a Fully-Qualified-Domain-Name (e.g. `example.com`, `aws.example.com`, etc.) whose DNS should be
          controlled by Route53

    4. `ORG_FAMILYDIRECTORY_API_SUBDOMAIN_NAME`

        - Should be the subdomain of `ORG_FAMILY_DIRECTORY_HOSTED_ZONE_NAME` where api endpoints are accessed (
          e.g. `api`)

    5. `ORG_FAMILYDIRECTORY_COGNITO_SUBDOMAIN_NAME`

        - Should be the subdomain of `ORG_FAMILY_DIRECTORY_HOSTED_ZONE_NAME` where authentication is handled (
          e.g. `auth`)

    6. `ORG_FAMILYDIRECTORY_COGNITO_REPLY_TO_EMAIL_ADDRESS`

        - Should be an externally-managed email address that captures responses to cognito *no-reply* emails (
          e.g. `familydirectory@gmail.com`)

    7. `ORG_FAMILYDIRECTORY_SES_MAIL_FROM_SUBDOMAIN_NAME`

        - Should be the subdomain of `ORG_FAMILY_DIRECTORY_HOSTED_ZONE_NAME` where emails are sent from (e.g. `support`)

    8. `ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_NAME`

        - The name of your repository on GitHub containing the UI components
        - Unless you want to write your own UI, just
          fork [FamilyDirectoryUI](https://github.com/Kapral67/FamilyDirectoryUI) and set this Environment Variable
          to `FamilyDirectoryUI`

    9. `ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_OWNER`

        - Your GitHub Username

    10. `ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_OAUTH_TOKEN`

        - Fine-grained GitHub Token that ONLY gives access
          to `${ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_OWNER}/${ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_NAME}` and only
          allows read/write permissions for repository hooks
        - This token only needs to be valid for each time you deploy the `AmplifyStack`
        - To create this, go to GitHub Settings > Developer settings > Personal access tokens > Fine-grained tokens
            - Generate new token
            - Set Expiration to something short like 7 days
            - Repository Access: Only select repositories *Your Fork/Repo Here*
            - Permissions > Repository permissions > Webhooks > Access: Read and write

3. Now is a good time to bootstrap you're aws account for cdk if you haven't already

    - This only needs to be done once before the first deployment:

        - `cdk bootstrap "aws://$AWS_ACCOUNT_ID/$AWS_REGION"`

        - `cdk bootstrap "aws://$AWS_ACCOUNT_ID/us-east-1` (Only needed if your chosen region from step 1 is
          not `us-east-1`)


4. If on a `*nix` system, you can use the `stage.bash` script to build this project in the correct order

    - If on Windows or other `non *nix` system, you will have to use the `stage.bash` script as a guide to build this
      project manually

        - Do Not Attempt to use `stage.bash` on a system that does not use `/` as
          the [name-separator character](https://docs.oracle.com/javase/8/docs/api/java/io/File.html#separatorChar)

5. Now you can synth and deploy

    1. First, deploy the `FamilyDirectoryDomainStack` solely (e.g. `cdk deploy FamilyDirectoryDomainStack`)

        - Before moving forward, login to the aws console and navigate to Route53, click on Hosted Zones in the right
          sidebar, then click on the Hosted Zone

        - Here, you need to copy the NS records for `${ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME}` and apply them at your
          registrar

            - If `${ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME}` is a subdomain (e.g. `subdomain.example.com`):

                - Make sure that you are applying these records for the subdomain, not the root domain

                - *Note that some DNS Providers/Registrars don't work very well for delegating domains. I had success
                  when using Cloudflare as the Nameserver for my root domain. For these purposes, Cloudflare is free if
                  you already own the root domain.*

        - You will also need to set a temporary A record for `${ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME}` in your hosted
          zone's record table select **Create record** button and leave the record name blank, make sure Record type is
          **A** and set the TTL to something short like 300 seconds. The routing policy should be **Simple routing** and
          the Value of the record can be anything (this is a dummy record so that Route53 allows us to attach A records
          to subdomains and is overwritten by the `FamilyDirectoryAmplifyStack`). You can use the
          value `93.184.216.34` (which is the A record value of `example.com` at the time of writing)

        - Wait Until DNS Propagates, Then Continue

    2. Now, deploy the `FamilyDirectoryApiGatewayStack`
        - This stack should cause all stacks except `FamilyDirectoryAmplifyStack` to deploy along with because they are
          all dependents of this stack.
        - Since there lots of artifacts being deployed and dns validation occurring on some stacks, this will take
          a while

    3. If you used the `stage.bash` script to build this repo, then `AdminClient` is already built for you
        - Before deploying the `FamilyDirectoryAmplifyStack`, you need to create the root member
        - The `AdminClient` has the capability to walk you through this
        - Just run `AdminClient` and select **CREATE** command and **ROOT** option

***==TODO==** Finish Deployment Order*