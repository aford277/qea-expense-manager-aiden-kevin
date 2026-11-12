# Exploratory Testing Notes

## Session 1: Employee App

**Charter:** Explore login, submit, edit and delete in the Employee App with unusual input and interruptions to find validation and error-handling gaps.
**Tester:** Kevin · **Date:** 2026-11-11 · **Time:** 3:00-3:20 (20 min)

### What I tried
| What I tried | Expected | What happened | Bug? |
|---|---|---|---|
| Login with spaces around username: `  emp_jordan  ` | Logs in, or `Invalid username or password.` | | |
| Unknown username `emp_nobody` | `Invalid username or password.` | | |
| Right username, wrong password | Same message as unknown username | | |
| Username in another case: `EMP_JORDAN` | `Invalid username or password.` | | |
| Log in as `mgr_asha` | `This account does not have access to this app.`, then exits | | |
| Spaces only, then a valid login | Re-prompts without counting a failure | | |
| Start with `EM_DB_PATH` unset | Clear message naming `EM_DB_PATH`, exit code 1 | | |
| `EM_DB_PATH` set to a folder that doesn't exist | Clear message, exit code 1, no traceback | | |
| Ctrl+C at the Amount prompt | Short message, no traceback | | |
| Main menu: `6`, `-1`, `1.0`, ` 1 `, `abc`, empty | Re-prompts for invalid; ` 1 ` accepted | | |
| Read the main menu labels | Labels match §6.2 | | |
| Amount: `1,000`, `+5`, `.5`, `5.`, `007`, `1e3`, `10000.00`, `10000.001` | Only `007` and `10000.00` accepted | | |
| Description: 201 chars with 1 leading space | Accepted (200 after trim) | | |
| Description with emoji and apostrophe: `Client's lunch 🍕` | Saved and shown exactly | | |
| Category `06`, ` 3 `, `٣` | `06` and ` 3 ` accepted; `٣` rejected | | |
| Date `2026-11-12`, `2024-02-29`, `2026-2-5`, `2026/11/01` | Today accepted; others rejected | | |
| Edit expense 4 (Omar's) | Clear refusal, nothing changes | | |
| Edit expense 5 (approved) | `Expense is no longer pending.` | | |
| Edit expense 999 | Clear "not found" message | | |
| Edit with Enter on every field | Nothing changes | | |
| Delete answers `yes`, `Y `, ` y`, `n` | Only `y`/`Y` deletes | | |
| View filter `5`, `abc`, empty | Re-prompts | | |
| History: expenses 5 and 9 | Both show `(no comment)` | | |
| Open `logs/employee_app.log` after the session | Logins logged, no passwords | | |

### Findings
- **Defects:** [KAN key]: [summary] (FR-xx)
- **New test case ideas:** [idea] → [TC ID or "not added"]

## Session 2: Manager App

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