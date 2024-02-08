## Feb 06 2024

- update gradle

### AdminClient

- Simplify Event constructors by further abstracting EventHelper (abstract class)

- Implement logic to detect Termux and set /path/to/stty accordingly

- Better Thread Interrupt Handling

    - In PickerModel:

        - FETCH THREAD

            - WAIT

                - REFRESH_CALLED

            - NOTIFY

                - PROCESSING_DONE

        - CALL THREAD

            - WAIT

                - PROCESSING_DONE

            - NOTIFY

                - REFRESH_CALLED

---

### Deferred Tasks

### Changelog

- Maybe CREATE/UPDATE/DELETE should publish their changes to some kind of log so that releases of PDFs/vCard files can
  include the changes?
    - To do this, we'd need to have some kind of locking applied to the log:
        - apis would acquire this lock at the end of their function
        - generating functions would acquire this lock at the start of their function
    - **Look into DDB Streams**
