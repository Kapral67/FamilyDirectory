## Nov 07

### CognitoStack

- [x] Callback URLs, Logout URLs, etc.

### Admin Initialize Family Directory?

- How should admins initialize a FamilyDirectory?

### AmplifyStack

- [ ] Needs Unit Testing

### ApiGatewayStack

- [ ] Needs Additional Unit Testing

### README

- [ ] Needs Update in Deployment Steps so that LambdaStack may be deployed with DDB Streams enabled

---

### Deferred Tasks

### Changelog

- Maybe CREATE/UPDATE/DELETE should publish their changes to some kind of log so that releases of PDFs/vCard files can
  include the changes?
    - To do this, we'd need to have some kind of locking applied to the log:
        - apis would acquire this lock at the end of their function
        - generating functions would acquire this lock at the start of their function
    - **Look into DDB Streams**