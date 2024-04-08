## Apr 07 2024

- rearrange CdkApp Stacks, so we can provide Amplify appId and branchName to ApiFunction::UpdateMember
- update LambdaFunctionConstructUtility to handle amplify permissions
- bump version
- ensure that AmplifyUtils works for both AdminClient and ApiFunction::UpdateMember
- ensure to update any CDK tests
- run stream function automatically on cdk deployment

---

### Deferred Tasks

- Maybe CREATE/UPDATE/DELETE should publish their changes to some kind of log so that releases of PDFs/vCard files can
  include the changes?
    - To do this, we'd need to have some kind of locking applied to the log:
        - apis would acquire this lock at the end of their function
        - generating functions would acquire this lock at the start of their function
    - **Look into DDB Streams**
