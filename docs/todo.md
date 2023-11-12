## Nov 12

### AmplifyStack

- [ ] Needs Unit Testing

### ApiGatewayStack

- [ ] Needs Additional Unit Testing

### README

- [ ] Finish TODOs
- [ ] Add Instructions for AdminClient

### AdminClient

- [x] Maybe create script or alias for `./gradlew -q --console plain run`?

---

### Deferred Tasks

### Changelog

- Maybe CREATE/UPDATE/DELETE should publish their changes to some kind of log so that releases of PDFs/vCard files can
  include the changes?
    - To do this, we'd need to have some kind of locking applied to the log:
        - apis would acquire this lock at the end of their function
        - generating functions would acquire this lock at the start of their function
    - **Look into DDB Streams**

### PDFs

- Birthday Views
    - By Standard Family Traversal
    - By Month