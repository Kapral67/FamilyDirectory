# FamilyDirectory

## Summary of Completed Requirements

> ### Requirements
> 
> 1. Users have access to several apis related with their FamilyDirector(y|ies)
> 
>    1. CREATE: Users should be able to create family members and add them to the database for members that are direct descendants of themselves
>    
>    2. UPDATE: Users should be able to update data in the database for themselves, their significant-other (if applicable), and their descendants who are still under the Age of Majority (18 years old)
>    
>    3. DELETE: Users who are apart of a FamilyDirectory by birth or other indecision, should have the ability to delete the entry in the database for their Significant Other (covers cases like divorce, etc.)
>    
>    4. GET: Retrieve any single or multiple MEMBER(s) in the FamilyDirectory as a `vCard` file and/or retrieve  all MEMBER(s) in the FamilyDirectory in a formatted `pdf` file
>    
> 2. Users shall adhere to input validation when CREATING, UPDATING, and/or DELETING Members
> 
> 3. Users shall have confidence that their data within the database tables is not subject to management by unauthorized users
> 
>    1. Users must provide authorization to use apis and this authorization grants them access to only certain members in the FamilyDirectory
>    
>    2. Users must have an uniquely identifiable account by email-address as well as entries in the Database tables
>    
> 4. Members shall receive updates to the FamilyDirectory either via email or website and the medium shall be either `vCard` or `pdf`
> 
>    1. Users can opt out of email communications
>    
>    2. Members with a `null` or invalid email will not recieve email communications
>    
> 5. Users shall have a sufficient interface designed for the layperson to call apis in an intuitive manner
> 
>    1. The interface shall be a value-add for Users that could not be achieved with APIs alone
>

### Requirements Assessment

1.1 CREATE: **100% COMPLETE** 

  - An additional feature was implemented to allow Native Members to a FamilyDirectory CREATE a MEMBER Table for their SPOUSE if not present

1.2 UPDATE: **100% COMPLETE**

1.3 DELETE: **100% COMPLETE**

1.4 GET: **100% COMPLETE**

  - The original GET api was intended for retrieving MEMBER Table entries in an exportable format (e.g. vCard or pdf)
  
  - This api is implemented as GET_PDF to disambiguate itself from GET_MEMBER, an api I implemented as there existed an oversight in my requirements that failed to grasp the need for retrieving MEMBER Table entries in a RESTful Manner (e.g. json)

2. Input Validation for API Input: **100% Complete**

  - This aspect is best demonstrated via the raw source code, but several validation techniques are used depending on the field (e.g. regex for firstName, middleName, lastName, Apache Common Validator for emails, Google libphonenumber for phones)

3.1 Authorization-Backed APIs: **100% Complete**

  - APIs are protected by OAuth tokens that must be retrieved by the industry-standard [OAuth 2.0 Authorization Code Grant Flow](https://www.rfc-editor.org/rfc/rfc6749.html#section-4.1)

    - This mechanism only establishes permission to call APIs

    - This feature is managed by AWS Cognito

  - APIs can only be referred by the Hosted Zone within which they reside (e.g. an api at https://demo.familydirectory.org/get can be referred by https://demo.familydirectory.org but cannot be referred by https://google.com or http://example.com etc.)

    - This feature is configured as CORS rules as part of AWS ApiGateway

  - Additional logic is implemented once an API is called retrieve a MEMBER based on the caller's (principal) OAuth access token and verify their permissions upon the resource (MEMBER)

    - e.g. All MEMBERS can call GET_MEMBER on any other MEMBER, but only NATIVE MEMBERS can call DELETE and the resource MEMBER of that DELETE API call must be upon an existing SPOUSE within that principal MEMBER's FAMILY

3.2 MEMBERS' email address must be unique within the FamilyDirectory: **100% complete**

  - The email attribute in the MEMBER table is implemented as a pseudo-key field

  - While not a literal key, because the database is NoSQL and therefore non-relational, the MEMBER Table is configured whereby email is an indexed attribute, meaning that it is queryable, and queries against this index are made before any change to an email of any MEMBER, whether that be via CREATE or UPDATE

4. Updates to Directory via PDF using Website: **100% Complete**

  - If a MEMBER chooses to (A) add an email address to their MEMBER Table entry AND (B) sign-up to website, THEN they can download pdfs of the FamilyDirectory which are auto-generated whenever the state of the MEMBER Table is changed (CREATE, UPDATE, DELETE)

  1. Email Communications: **100% Complete**

    - Only USERS (MEMBERS with an email address who choose to sign-up so they can make API calls) are subject to automated email communications, all other MEMBERS will never receive an email communication, so it is opt-out by default

5. Intuitive Interface: **100% Complete**

  - The Interface follows Material UI guidelines as standard on the Android OS

  1. Interface Adds Value to USERS: **100% Complete**

    - MEMBERS without a USER account are not able to view the FamilyDirectory whatsoever, not through API calls or downloading pdfs, so the interface adds value, at a bare minimum, out of necessity; although it is my personal belief that it is a decent UI that would receive more praise than compliants

## Presentation Plan

- First I want to introduce this project as a contacts app that manages relationships

- Then, the majority of my presentation will be spent demonstrating a USER's perspective via the demo project I have hosted at https://demo.familydirectory.org.

  - Here I can display the sign-up/in flows, and demonstrate all the available API calls such as GET_MEMBER, GET_PDF, CREATE, UPDATE, & DELETE 

- This will lead nicely into how the different front-end components translate into the back-end

  - I can display how the sign-up/in flows are directly associated with AWS Cognito

    - AWS Management Console: Cognito UserPool & UserPoolClient, AWS Lambda: PreSignUp & PostConfirmation Triggers

  - I can show the back-end APIs and how they go from the front-end to being executed on AWS

    - UI -> AWS ApiGateway -> AWS Lambda Function

    - I don't think code walkthroughs are very enjoyable as an audience member, so I think the AWS Management console will be better for the presentation (but I would love to demonstrate any code to curious people)

- In demonstrating the UI and AWS Management Console (back-end) I can show that all requirements for this project have been exceeded

- Some portion of the presentation I want to dive into what makes this project reproducible

  - AWS CDK

    - Defining back-end infrastructure as code

  - AWS Amplify

    - Allows the front-end to be full CI/CD and connected to a GitHub repo via a GitHub app (webhook listener)

      - This enables new commits to be automatically built and pushed to the front-end

    - Technically AWS Amplify is back-end infrastructure as its configuration is managed in AWS CDK, front-end code is all that is present in https://github.com/Kapral67/FamilyDirectoryUI

- Leftover time before the Q&A can be spent going over the CDK stacks in deployment order:

  1. Domain (Route53)

  2. Email (Simple Email Service)

  3. Auth Certificates (Cognito East)

  4. Database (DynamoDb)

  5. Cognito

  6. Storage For Generated PDFs (S3)

  7. Serverless Execution Logic for APIs, PDF Generator, Cognito Triggers (Lambda)

  8. API Routes, Mapping, Rules, General Configuration (ApiGateway)

  9. Hosting and Continuous Integration/Deployment for Front-End (Amplify)

- One thing that I would like to present but may not have time for is the attributes and organization of the three database tables: MEMBER, FAMILY, & COGNITO

## Final Comments

Overall there is far too much code to have any sort of comprehensive code walkthrough during a 25 minute presentation, but there are certain portions of code that might be interesting:

1. The PDF Generator Lambda traverses the database and understanding is traversal routine is probably the best way to understand how the MEMBER and FAMILY tables work in tandem

2. There is a lot less code in the front-end, so walking through portions of that code might give an audience member a nice high-level understanding of the purposes of each API

*Non-Exhaustive List*