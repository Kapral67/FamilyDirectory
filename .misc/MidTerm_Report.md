# Design & Development Update

## Current Progress

- Entire backend stacks are finished and have Infrastructure unit testing

These stacks are as follows:

Domain (DNS Records)
SES (Email Notifications)
Cognito (Authentication)
DynamoDb (NoSQL Tables for managing Members, Family Relationships between Members, and Mapping Cognito Users to Members)
S3 (Static Object Storage used for storing PDFs)
Lambda (Serverless Logic for APIs)
ApiGateway (Routing Logic for APIs)

### CognitoStack

PreSignUpTrigger - Rejects signup requests when registering email is not found in Member Table; or if email is found and Member has already previously signed up

PostConfirmationTrigger - After a user has verified ownership of their email address and their account is created, Handles cases where the account may be duplicate or invalid and needs to be disabled to prevent security implications

### DynamoDbStack

PdfGenerator - Generates Formatted Pdf of the Members entries in Familial Descending Order. Descendant Paths are followed all the way to latest Member before moving on to next Descendant. Pdfs are stored in S3 and accessed via GetPdf api. [Apache PDFBox](https://pdfbox.apache.org/)

### LambdaStack

CreateMember - [POST] Allows Users to Create a new Descendant or Significant Other (Not Idempotent; Errors when Member already exists)

UpdateMember - [PUT] Allows Users to Edit their Entry or entries of their Spouse or Descendants < 18 years old (Idempotent)

GetMember - [GET] Returns the MEMBER Table and FAMILY Table of Entries of the Provided ID (If no Id is provided, the caller's entries are returned)

GetPdf - [GET] Always returns 302 pointing to temporary, signed, request to download a PDF of the FamilyDirectory (10 minute validity)

## Future Plans

- Implement a simple, single-page (webpage) frontend using React

- Host this Frontend as a separate GitHub Repository and then deployments can be handled as a CI/CD workflow by AWS Amplify whenever a commit is detected

## Portfolio Submission

- A demo FamilyDirectory with an example Family will be made for the final project demonstration.

## Blocking Issues

**None**

## [Code Developed](https://github.com/Kapral67/FamilyDirectory) (Repository Not Yet Public)