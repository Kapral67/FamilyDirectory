## Oct 9

### Finish ApiGatewayStack

- Needs Review

### Finish CognitoStack

- Callback URLs, Logout URLs, etc.

### Basic DELETE

- For now, only worry about the case where a NATIVE CALLER wants to DELETE NATURALIZED SPOUSE (i.e. DIVORCE)
    - All other cases of DELETE should be UNAUTHORIZED/FORBIDDEN
    - DELETED MEMBER should be notified via email that their account was deleted (only if they are registered in the
      UserPool)

### PDF vCard & S3

- Interval? Realtime?

### Changelog

- Maybe CREATE/UPDATE/DELETE should publish their changes to some kind of log so that releases of PDFs/vCard files can
  include the changes?
    - To do this, we'd need to have some kind of locking applied to the log:
        - apis would acquire this lock at the end of their function
        - generating functions would acquire this lock at the start of their function
    - **Look into DDB Streams**
