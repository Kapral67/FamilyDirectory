# Brainstorm

## Critical Caveats

- Think through the UPDATE api endpoint carefully since PK for MEMBERS and FAMILIES table entries will need to be
  regenerated if any of `firstName`, `lastName`, `birthday` or `suffix` are allowed to be modified.
- Let's leave it tentative for now, but DELETE api endpoint should probably be leaved for developer-level authorization
  only

## Flow

- We can use Amazon Cognito to shoulder the burden of api and website authentication
- Using Cognito will require some sign-up validation as not just anyone is allowed to invoke the api
- Furthermore, different users should have different scopes of access
    - E.g.
        - Parents should have authorization over:
            - themselves
            - each-other
            - their minor kids
        - Whereas an eighteen-year-old should have authorization over:
            - only themselves
- To do this we could
  have [Pre sign-up Lambda Trigger](https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-lambda-pre-sign-up.html)
  that checks a DynamoDb table if the email address in the signup request is valid
- We can add a [Global Secondary Index](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GSI.html)
  to the MEMBERS table where email address is the partition key for this index
    - Obviously, non-members cannot onboard to access the website or any of the apis
    - To check whom they are authorized for:
        1. Check that they meet some minimum age requirement
        2. If they are PK or SPOUSE in the FAMILIES table entry pointed to by their `ancestor` attribute in MEMBERS
           table
           entry:
            - They have ability to edit/create all members of that Family (themselves (PK), spouse, descendants (younger
              than Age of Majority))
        3. Else they can only edit themselves with no create permission