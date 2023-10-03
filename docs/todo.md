## Oct 3

Get Rid of FAMILIES Table

We can accomplish the same thing by having ANCESTOR, SPOUSE, DESCENDANTS attributes in the MEMBERS Table Entries:

SPOUSES can be differentiated by their ANCESTOR & SPOUSE attributes:

- SPOUSE & ANCESTOR will be the same when the MEMBER chose this family
- SPOUSE & ANCESTOR will be different when MEMBER is native to this family

Can we make finding both parents from a descending member easier?

Currently from a DESCENDANT:

- the parent native to the FamilyDirectory is located at DESCENDANT's ANCESTOR attribute
- the parent naturalized to the FamilyDirectory is located at DESCENDANT's ANCESTOR's SPOUSE attribute
