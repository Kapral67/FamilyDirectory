## Mar 08 2024

### Known Bugs

#### AdminClientTui

##### Thread Execution Race Condition (mabe02/lanterna#595)

1. cdk deploy
2. toolkit cleaner
3. pdf generator -> off
4. pdf generator -> on
5. exit

---

### Deferred Tasks

- Maybe CREATE/UPDATE/DELETE should publish their changes to some kind of log so that releases of PDFs/vCard files can
  include the changes?
    - To do this, we'd need to have some kind of locking applied to the log:
        - apis would acquire this lock at the end of their function
        - generating functions would acquire this lock at the start of their function
    - **Look into DDB Streams**
