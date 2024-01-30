# FamilyDirectory Release Notes

## v0.6

### IMPORTANT UPDATE INFO

- If Updating from v0.42 or below, please follow **IMPORTANT UPDATE INFO** from v0.5

- If Updating from v0.5, make sure to manually trigger a front-end build for Amplify from the AWS Console

### Bug Fixes

- Normalize throws of type NullPointerException versus NoSuchElementException across codebase

### Features

- Added Ability to Enable certain Cognito Users as Admin from AdminClient

- Admin Cognito Users may call any API upon any Member (essentially Admin Cognito Users have same capabilities as
  AdminClient as far as CREATE, UPDATE, and DELETE go)

- MFA for Cognito Users is now Enforced, Existing Users will be forced to Onboard at Next Sign-In

### Dependency Upgrade Tracking PRs

- ==PLACEHOLDER==

## v0.5

### IMPORTANT UPDATE INFO

- After Updating, please trigger Stream Function Invocation by Toggling PDF Generator OFF and ON (use the AdminClient)

- Also, please manually trigger a build for Amplify from the AWS Console

### Bug Fixes

- Remove Duplicate LogLevel Indicators in CloudWatch

- Toggling PDF Generator ON triggers an asynchronous invocation of the PDF Generator Lambda

### Features

- Added Birthdays Pdf Organizing Members by Birth Month

- Downloads Are Now a Zip file of new Birthdays Pdf and Standard Directory Pdf

### Dependency Upgrade Tracking PRs

- #717 - #749

## v0.42

### Bug Fixes

- Fix suppressed exceptions list in case of double NewPageException (PdfHelper in PdfGeneratorLambda)

- Internal PDDocument now only uses RAM and is limited based on Lambda's configuration in CDK code (PdfHelper in
  PdfGeneratorLambda)

### Features

- Add some logging (PdfHelper in PdfGeneratorLambda)

### Dependency Upgrade Tracking PRs

- #673
- #676 - #677
- #680
- #683
- #685
- #687
- #690 - #691
- #693 - #715

## v0.41

### Bug Fixes

- Incorporate HttpApi's new `arnForExecuteApi` method introduced in CDK `2.117.0`

### Dependency Upgrade Tracking PRs

- #649 - #672

## v0.4

### Bug Fixes

- Removing Unused/Unnecessary Code

- Incorporate ToolkitCleaner logic entirely within AdminClient

- Deprecates CdkGarbageCollection stack from v0.3

    - If you deployed v0.3, delete the CdkGarbageCollectionStack and all its resources

### Dependency Upgrade Tracking PRs

- #623 - #626

- #628 - #648

## v0.3

### Features

- Expose FamilyDirectory Version as EnvVar to Amplify

    - Frontend code can dynamically change to incorporate new features if available

- Garbage Collect CDK Assets

    - Keep Costs Low by Garbage Collecting Unused CDK Assets

        - see [aws-cdk-rfcs#64](https://github.com/aws/aws-cdk-rfcs/issues/64)

    - Requires Manually Invocation Using `AdminClient`, see [README](../README.md)

### Bug Fixes

- Convert Emails to Lowercase before comparing in TriggerFunctions

- Minor Tweaks

    - Remove unused code, migrate to Java 21 `List::getFirst` instead of `.iterator().next()`

### Dependency Upgrade Tracking PRs

- ~~#600 - #610~~

    - superseded by #611, #613 - #622

- #612

## v0.2

### Features

- Upgrade to Java 21

    - Upgrade Lambda to new Amazon Linux 2023 runtime

- PdfGenerator Enabled By Default

- AdminClient has Wait Counter before Allowing PdfGenerator to flip from OFF to ON

### Bug Fixes

- PdfGenerator Processing Stale Streams

    - Prevent AdminClient from turning on PdfGenerator before records expire

- Improve Stack Trace Exception formatting in CloudWatch Logs (Lambda Functions)

- Erroneous LF chars printed to stderr by JsiiRuntime in AdminClient

    - Remove all usages of aws-cdk's Duration class from familydirectory-service-assets package to prevent creation of
      JsiiRuntime threads outside CDK code

- Double NewPageException in PdfHelper might not be obvious, added second NewPageException to suppressed

- PdfGeneratorLambda could execute for longer than maximum record's age

    - race condition where concurrent executions would "fight" over the same record

### Dependency Upgrades

- Tracked by PRs:

    - #565

    - #566

    - #577 thru #599

- CDK version is now `2.115.0` please use the
  respective [AWS CDK Toolkit](https://docs.aws.amazon.com/cdk/v2/guide/cli.html)
