## Family Directory

Want More Info? [See the Poster](.misc/Poster.pdf)

### Prerequisites

1. [Java JDK 21 or greater](https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html)

2. [AWS CDK ToolKit](https://docs.aws.amazon.com/cdk/v2/guide/cli.html)

    - *For `cdk` usage, see [CDK-GettingStarted.md](docs/CDK-GettingStarted.md)*

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

    4. `ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_NAME`

        - The name of your repository on GitHub containing the UI components

        - Unless you want to write your own UI, just
          fork [FamilyDirectoryUI](https://github.com/Kapral67/FamilyDirectoryUI) and set this Environment Variable
          to `FamilyDirectoryUI`

    5. `ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_OWNER`

        - Your GitHub Username

    6. `ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_OAUTH_TOKEN`

        - Fine-grained GitHub Token that ONLY gives access
          to `${ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_OWNER}/${ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_NAME}` and only
          allows read/write permissions for repository hooks

        - This token only needs to be valid for each time you deploy the `AmplifyStack`

        - To create this, go to GitHub Settings > Developer settings > Personal access tokens > Fine-grained tokens

            - Generate new token

            - Set Expiration to something short like 7 days

            - Repository Access: Only select repositories *Your Fork/Repo Here*

            - Permissions > Repository permissions > Webhooks > Access: Read and write

3. Now is a good time to bootstrap you're aws account for cdk if you haven't already (**Note**: *You must be in
   the `CDK` directory to run `cdk` commands*)

    - This only needs to be done once, before the first deployment:

        - `cdk bootstrap "aws://$AWS_ACCOUNT_ID/$AWS_REGION"`

        - `cdk bootstrap "aws://$AWS_ACCOUNT_ID/us-east-1` (Only needed if your chosen region from step 1 is
          not `us-east-1`)

4. Use the provided gradle script at the repository root to build this project (e.g. `./gradlew build`)

5. Now you can synth and deploy (**Note**: *You must be in the `CDK` directory to run `cdk` commands*)

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

    2. Now, deploy the `FamilyDirectoryCognitoStack`

        - This stack should cause the following stacks to be deployed as well:

            - `FamilyDirectorySesStack`

            - `FamilyDirectoryCognitoUsEastOneStack`

            - `FamilyDirectoryDynamoDbStack`

        - This may take awhile, so please be patient

    3. Before continuing, you need to create the root member

        - The `AdminClient` has the capability to walk you through this

        - Just run `AdminClient`, select **CREATE** command, then **ROOT** option, and fill in the prompts

            - **Note** do not try to create additional members or utilize other functions of `AdminClient` just yet

    4. Now, deploy the `FamilyDirectoryApiGatewayStack`

    5. Now is a good time to use `AdminClient` to prefill your FamilyDirectory with members

    6. Finally, we need to enable Production Access to SES

        1. Go to SES in AWS Management Console

        2. On the sidebar there is a **Get set up** option, click that

        3. Then on the first section of that page there is a **Request Production Access** button, click that

        4. For the **Mail type** select **TRANSACTIONAL**

        5. For the **Website URL** enter `https://${ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME}`

        6. For the **Use case description** put something to the effect
           of `"For emailing account-holders of this application"`

    - *It may take up to 24 hours for your Production Access Request to be approved, users are not able to sign-up until
      this is complete*

### Updating

#### Frontend

The frontend is full CI/CD, so as long as you keep your fork in sync
with [FamilyDirectoryUI](https://github.com/Kapral67/FamilyDirectoryUI), new commits are automatically deployed to your
frontend

#### Backend

For backend updates:

**Note**: *You must be in the `CDK` directory to run `cdk` commands*

1. Follow steps 1, 2, & 4 from **Steps to Deploy** (We don't need to bootstrap, so step 3 is not needed)

2. Deploy the `FamilyDirectoryApiGatewayStack`

    - `cdk deploy FamilyDirectoryApiGatewayStack`

3. Remove Old Cdk Assets

    - `cdk gc --unstable=gc --type=s3 --created-buffer-days=0 --confirm=false`
