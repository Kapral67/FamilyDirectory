## Oct 25

### Finish CognitoStack

- Callback URLs, Logout URLs, etc.

### PDF vCard & S3

- Interval? Realtime?

### Changelog

- Maybe CREATE/UPDATE/DELETE should publish their changes to some kind of log so that releases of PDFs/vCard files can
  include the changes?
    - To do this, we'd need to have some kind of locking applied to the log:
        - apis would acquire this lock at the end of their function
        - generating functions would acquire this lock at the start of their function
    - **Look into DDB Streams**

### CDK

- Domain Stack has static reference to A record for `example.com`

- Cognito Stack cannot create the triggers because of circular dependency. Should be moved to Lambda Stack
