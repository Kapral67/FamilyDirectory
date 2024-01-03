## Jan 03 2024

### Unreleased Changes

#### Bug Fixes

- Fix suppressed exceptions list in case of double NewPageException (PdfHelper in PdfGeneratorLambda)

- Internal PDDocument now only uses RAM and is limited based on Lambda's configuration in CDK code (PdfHelper in
  PdfGeneratorLambda)

#### Features

- Add some logging (PdfHelper in PdfGeneratorLambda)

#### Dependency Upgrade Tracking PRs

- #673 - #692

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
