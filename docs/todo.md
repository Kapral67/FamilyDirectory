## Dec 14 2023

### BEFORE v0.2 DEPLOYMENT

- Fix Weird GET_MEMBER ISSUE (see dev CloudWatch)

- Add CountDown in AdminClient when TOGGLE_PDF_GENERATOR is toggled

    - Also, Better Formatting

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
