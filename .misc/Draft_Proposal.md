<div style="text-align: right">Maxwell Kapral</div>

## Summary

The goal of this project is to create an organization of contacts where-by the contacts hold familial relationships to each-other.

At the high-level, the backend database will contain two tables: `Member` & `Family`.

This project is not to categorize an entire family tree, per se, but rather the genetic descendants of a common ancestor. So for example one FamilyDirectory of mine could be centered around my great-grandmother from my mother's side. In this case, both my mother and father would have entries in the Member table, but the Family table would have my mother as its Primary Key and Father as Spouse attribute. Certain cases like a divorce best highlight the organization of this. 

For example, if my grandmother and grandfather on my mother's side are divorced then only the grandparent who is a child of my mother's side great-grandmother would persist in this FamilyDirectory. A different FamilyDirectory would be necessary maintaining the other grandparent's ancestral history.

Of course, the "ostracized" grandparent in the above example could still be included as a contact (Member Table Entry), but they wouldn't be apart of the FamilyDirectory which is a term I made up to describe the relationship between the Member Table, the Family Table, and their entries. In other words, 

### Member (Database Perspective)

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

*From the client's perspective, there is no "Family" Table, it is mainly a mechanism to organize data internally*

Family Table is used to store MemberReferences (Primary Keys of elements in the Member table) in a fashion that denotes the nuclear family relationships within a larger family-tree

#### PRIMARY_KEY

The familial-side's nuclear family member

|SPOUSE|DESCENDANTS|
|:-:|:-:|
|@Nullable|@Nullable|
|STRING|LIST of STRING|
|Primary Key of a Member|List of Primary Key's of Members|

## Major Goals & Objectives

## Envisioned Deliverables/Demonstration

## Value Proposition