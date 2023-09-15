# FamilyDirectory

## Summary

The goal of this project is to create an organization of contacts where-by the contacts hold familial relationships to each-other.

At the high-level, the backend database will contain two tables: `Member` & `Family`.

This project is not to categorize an entire family tree, per se, but rather the genetic descendants of a common ancestor. So for example one FamilyDirectory of mine could be centered around my great-grandmother from my mother's side. In this case, both my mother and father would have entries in the Member table, but the Family table entry for our nuclear family would have my mother as its Primary Key and my father as her Spouse attribute. Certain cases like a divorce best highlight the organization of this. 

For example, if my grandmother and grandfather on my mother's side are divorced then only the grandparent who is a child of my mother's side great-grandmother (FamilyDirectory root ancestor) would persist in this FamilyDirectory. A different FamilyDirectory would be necessary maintaining the other grandparent's ancestral history.

Of course, the "ostracized" grandparent in the above example could still be included as a contact (Member Table Entry), but they wouldn't be apart of the FamilyDirectory.

### Architecture

- ==The backend of this project will be AWS CDK & SDK code.==
- ==These aspects are all written in Java. All "functionality" (RESTful APIs, etc.) is served via AWS Lambda functions.==
   - ==This reduces the need to mantain any backend server==
- ==AWS Lambda functions will also be written in Java.==
- ==The frontend, which is most likely going to entail some sort of web-interface, will be in .NET Blazor==
- ==Blazor uses razor files (similar to how Django combines html & python, this allows the combination of html & C#)==

### Member (Database Perspective)

==This table stores the contact information for members in the directory==

#### PRIMARY_KEY

SHA256 Hexadecimal Hash of Member's `FULL_NAME` + `BIRTHDAY`

`FULL_NAME` is a space-separated STRING of `FIRST_NAME` + `LAST_NAME` + `SUFFIX` (if the Member's `SUFFIX` is Null, `FULL_NAME` is then `FIRST_NAME` + `LAST_NAME`)

So John Doe Sr. born January 3, 1992 would be `sha256hex("John Doe Sr" + "1992-01-03")` whereas Jane Doe born April 2, 1989 would be `sha256hex("Jane Doe" + "1989-04-02")`

|FIRST_NAME|LAST_NAME|SUFFIX|BIRTHDAY|DEATHDAY|EMAIL|PHONES|ADDRESS|
|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
|@NotNull|@NotNull|@Nullable|@NotNull|@Nullable|@Nullable|@Nullable|@Nullable|
|STRING|STRING|ENUM-STRING|STRING|STRING|STRING|ENUM-MAP of STRING|LIST of STRING|
|||{"JR", "SR"}|"yyyy-MM-dd" (e.g. "1970-12-31")|"yyyy-MM-dd" (e.g. "1970-12-31")|[EmailValidator](https://commons.apache.org/proper/commons-validator/apidocs/org/apache/commons/validator/routines/EmailValidator.html)|{"MOBILE", "LANDLINE", "WORK"}|Each index is a line of the address|
|||||||"+<country_code><phone_number>" (E.g. "+1800[8675309](https://en.wikipedia.org/wiki/867-5309/Jenny)")|E.g. "400 W 1st St, Chico, CA 95929" would be:|
|||||||[PhoneNumberUtil](https://javadoc.io/doc/com.googlecode.libphonenumber/libphonenumber/latest/com/google/i18n/phonenumbers/PhoneNumberUtil.html)|["400 W 1st St", "Chico, CA 95929"]|

### Family

*From the client's perspective, there is no "Family" Table, it is mainly a mechanism to organize relationships internally*

Family Table is used to store MemberReferences (Primary Keys of elements in the Member table) in a fashion that denotes the nuclear family relationships within a larger family-tree

- ==This table enables traversing the directory==

   - ==Beginning at the root ancestor, all members should be accessible via repeated recursion through descendants lists==

#### PRIMARY_KEY

The familial-side's nuclear family member

|SPOUSE|DESCENDANTS|
|:-:|:-:|
|@Nullable|@Nullable|
|STRING|LIST of STRING|
|Primary Key of a Member|List of Primary Key's of Members|

## Major Goals & Objectives

- Interface for members to edit their contact information
- Permission system granting certain users authority over other users (e.g. parents can change info of their minor children)
- Interface for administrators to create new members and family tree links
- logic to automatically generate [vCard](https://en.wikipedia.org/wiki/VCard) and pdf files containing nicely formatted/importable contacts for each member in the directory
  - ==The pdf files are the main objective, since these can be formatted such that nuclear families within the directory are grouped==
  - ==Also, we can order the pdf by generations==

- Automated system to deliver family directory updates (email and/or push notification)
- Backend databases and frontend interface are connected through RESTful API (accomplished via [AWS ApiGateway](https://aws.amazon.com/api-gateway/) and [AWS Lambda](https://aws.amazon.com/lambda/))

## Envisioned Deliverables/Demonstration

- Main deliverable is the client-side interface for [upserting](https://en.wikipedia.org/wiki/Merge_(SQL)#upsert) member table entries
  - this interface is planned as either an email base system or website

### Email

For email, input would need to be submitted in a machine-readable way. An example template would be provided and then users would simply fill out the form and send the email to the appropriate address.

```yaml
*first_name*:
*last_name*:
suffix:
*birthday*:
deathday:
address_line_1:
address_line_2:
...
email:
*ancestor_first_name*:
*ancestor_last_name*:
ancestor_suffix:
*ancestor_birthday*:
*ancestor_is_spouse*:
landline_phone:
work_phone:
mobile_phone:
```
<div style="text-align: right"><i>fields surrounded with '*' are required</i></div>

Authorization for the email interface would check against the sender email address. Additionally, to protect against spoofing, [SPF](https://en.wikipedia.org/wiki/Sender_Policy_Framework)/[DKIM](https://en.wikipedia.org/wiki/DomainKeys_Identified_Mail) could be checked as well.

### Website

Very similar to the email, instead authorization would be more conventional, [AWS Cognito](https://aws.amazon.com/cognito/) can handle this.

Additionally, the form would be on a website and client-side checks could be integrated in addition to server-side checks. With the email system, only server-side sanitization and validation is possible.

## Value Proposition

- Allows people to manage their known/living family tree easily
- Provides a central location for always-up-to-date contact info for larger extended families
- Preserves the hierarchy of a known family tree without the need for expensive DNA tests
  - Some may also be adverse to mailing DNA to a company because of privacy concerns