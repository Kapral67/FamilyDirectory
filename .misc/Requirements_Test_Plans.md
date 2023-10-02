# FamilyDirectory

## Disambiguating Notes

*Only Basic User Flows are considered requirements as admins (myself) can always change aspects of the application directly through the aws console*

*The terms `User` and `Member` used in this document are distinct and not interchangeable*

*A `User` describes a `Member` who has an account that is distinct from their information lying in the database and serves authentication purposes for calling APIs*

*A `Member` is any person whose information exists as entries within the FamilyDirectory*

## Requirements

1. Users have access to several apis related with their FamilyDirector(y|ies)
   1. CREATE: Users should be able to create family members and add them to the database for members         that are direct descendants of themselves
   2. UPDATE: Users should be able to update data in the database for themselves, their significant-other (if applicable), and their descendants who are still under the Age of Majority (18 years old)
   3. DELETE: Users who are apart of a FamilyDirectory by birth or other indecision, should have the ability to delete the entry in the database for their Significant Other (covers cases like divorce, etc.)
   4. GET: Retrieve any single or multiple MEMBER(s) in the FamilyDirectory as a `vCard` file and/or retrieve  all MEMBER(s) in the FamilyDirectory in a formatted `pdf` file
2. Users shall adhere to input validation when CREATING, UPDATING, and/or DELETING Members
3. Users shall have confidence that their data within the database tables is not subject to management by unauthorized users
   1. Users must provide authorization to use apis and this authorization grants them access to only certain members in the FamilyDirectory
   2. Users must have an uniquely identifiable account by email-address as well as entries in the Database tables
4. Members shall receive updates to the FamilyDirectory either via email or website and the medium shall be either `vCard` or `pdf`
   1. Users can opt out of email communications
   2. Members with a `null` or invalid email will not recieve email communications
5. Users shall have a sufficient interface designed for the layperson to call apis in an intuitive manner
   1. The interface shall be a value-add for Users that could not be achieved with APIs alone



## Test Plans

1. APIs are implemented as AWS Lambda functions whose logic is proxied to an http endpoint via AWS ApiGateway, so their java classes can be unit tested through a framework such as JUnit and also a potential mocking framework such as Mockito.
2. Input validation for apis that accept them is also unit-testable
3. Generation of `pdf` and `vCard` files for GET requests can be regression tested by having a demo project in a separate availability zone containing fake data, these files can be verified manually or some automatic verification system can be implemented and these tests run on a schedule (time permitting)
4. Automated emails are testable easily as I can just subscribe myself to AWS SES stream and verify emails sent to Members are appropriate myself
5. The interface can be tested by other users and feedback from the class. I can also research some automated methods for testing interfaces, like using python selenium
6. Whether or not the interface is a value-add is testable by reaching out to potential users and/or classmates and surveying whether they feel the interface adds value to the project