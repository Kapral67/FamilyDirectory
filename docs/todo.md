## Oct 9

### Finish ApiGatewayStack

- Need to Create Api Endpoints for CREATE and UPDATE

### Finish CognitoStack

- Callback URLs, Logout URLs, etc.

### Create & Research SES (see CognitoStack comments)

### CognitoPreSignUp

- Use MemberEmail GSI to approve signup requests when email matches Member-in-ddb email

### CognitoPostConfirmation

- Map Cognito User Id (sub) to UUID of Member whose email matches Cognito User email based upon MemberEmail GSI Query
    - **This must occur after email has been verified and user account created**

### Basic DELETE

- For now, only worry about the case where a NATIVE CALLER wants to DELETE NATURALIZED SPOUSE (i.e. DIVORCE)
    - All other cases of DELETE should be UNAUTHORIZED/FORBIDDEN

### PDF vCard & S3

- Interval? Realtime?

### Changelog

- Maybe CREATE/UPDATE/DELETE should publish their changes to some kind of log so that releases of PDFs/vCard files can
  include the changes?
    - To do this, we'd need to have some kind of locking applied to the log:
        - apis would acquire this lock at the end of their function
        - generating functions would acquire this lock at the start of their function
    - Look into DDB STreams
