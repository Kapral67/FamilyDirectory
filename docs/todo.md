## Dec 19 2023

### v0.3

#### Features

- Expose FamilyDirectory Version as EnvVar to Amplify

    - Frontend code can dynamically change to incorporate new features if available

#### Bug Fixes

- Minor Tweaks

    - Remove unused code, migrate to Java 21 `List::getFirst` instead of `.iterator().next()`

#### Dependency Upgrade Tracking PRs

- #600 - #610

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
