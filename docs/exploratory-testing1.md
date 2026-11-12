# Exploratory Testing Notes

## Session 1: Manager App

**Charter:** Explore reviewing expenses with unusual IDs and comments to find validation gaps.
**Tester:** Aiden · **Date:** 2026-11-11 · **Time:** 4:00–4:30

### What I tried
| What I tried | What happened | Bug? |
|---|---|---|
| 0 as expense id | Expense ID must be a positive whole number. | No |
| -1 as expense id | Expense ID must be a positive whole number. | No |
| 1.5 as expense id | Expense ID must be a positive whole number. | No |
| abc as expense id | Expense ID must be a positive whole number. | No |
| "  7  " as expense id  | Expense is no longer pending. (valid input) | No |
| 007 as expense id | Expense is no longer pending. (valid input) | No |
| 999999999999999999999999999999999999 as expense id | Expense ID must be a positive whole number. | No? (it is a positive whole number but unrealistic and too high of an int) |
| 26 as expense id | Expense 26 not found. | No |
| A to approve expense | Expense was approved | No |
| D to deny expense | Expense was denied | No |
| C to cancel decision | Decision was cancelled | No |
| approve to approve expense | Valid options reprinted | No |
| Empty input to approve expense | Valid options reprinted | No |
| Four character deny comment | Comment too short message and reprompted | No |
| Four characters and a space deny comment | Comment too short message and reprompted | No |
| 200 characters deny comment | Expense denied | No |
| 201 character deny comment | Comment too long message and reprompted | No |
| 10 spaces deny comment | Comment too short message and reprompted | No? (Could be better error message) |
| Spaces surrounding valid comment | Comment saved without extra spaces | No |
| Comment containing é | Comment saved with � replacing the é | Yes (comment not saved properly) |
| Comment containing 😀 | Comment saved with ?? replacing the 😀 | Yes (comment not saved properly) |
| Comment with special characters (, and ?) | Comment saved properly | No |



### Findings
- **Defects:** KAN-30: Comments that are written with special characters such as é or emojis are replaced in the database but the app does not mention it to the user or have them reenter a comment without it. (FR-M2)
- **New test case ideas:**  Testing for emojis in comments, testing with just empty spaces (in comments or other places)