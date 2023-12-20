# FamilyDirectory Release Notes

## v0.3

### Features

- Expose FamilyDirectory Version as EnvVar to Amplify

    - Frontend code can dynamically change to incorporate new features if available

- Garbage Collect CDK Assets

    - Keep Costs Low by Garbage Collecting Unused CDK Assets

        - see [aws-cdk-rfcs#64](https://github.com/aws/aws-cdk-rfcs/issues/64)

### Bug Fixes

- Convert Emails to Lowercase before comparing in TriggerFunctions

- Minor Tweaks

    - Remove unused code, migrate to Java 21 `List::getFirst` instead of `.iterator().next()`

### Dependency Upgrade Tracking PRs

- #600 - #610

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
