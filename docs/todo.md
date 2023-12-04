## Dec 03

### AmplifyStack

- [ ] Needs Unit Testing

### README

- [ ] Finish TODOs

### apigatewayv2-*-alpha

- [ ] migrate to stable once published

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