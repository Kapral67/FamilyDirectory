## Oct 26 Updated

### Finish CognitoStack

- Callback URLs, Logout URLs, etc.

### CDK

- Domain Stack has static reference to A record for `example.com`

### S3

- Need S3 versioned bucket to store generated pdfs
- Need GET_PDF API to facilitate generating 302 redirect-able signed s3 urls to access pdf

### DISPLAY API (MEMBER)

1. Cognito User Logs in
2. Request is made to get their member info and returned as json
3. This api should be proxyable (id can be a url parameter)

### DISPLAY API (FAMILY)

- This is probably a stretch goal at this point, but would make the ui nicer

### UPDATE API

- We should add an id field to the event so that way if changes potentially affect the key of the to-be-updated-member,
  the request can still be processed
- proxyable?

---

### Deferred Tasks

### PDF Realtime vs Scheduled

### Changelog

- Maybe CREATE/UPDATE/DELETE should publish their changes to some kind of log so that releases of PDFs/vCard files can
  include the changes?
    - To do this, we'd need to have some kind of locking applied to the log:
        - apis would acquire this lock at the end of their function
        - generating functions would acquire this lock at the start of their function
    - **Look into DDB Streams**