# Exploratory Testing Notes

## Session 1: Employee App

**Charter:** Explore login, submit, edit and delete in the Employee App with unusual input and interruptions to find validation and error-handling gaps.
**Tester:** Kevin · **Date:** 2026-11-11 · **Time:** 3:00–3:20 (20 min)

### What I tried

| What I tried | What happened | Bug? |
|---|---|---|
| Login with spaces around username: `emp_jordan ` | Logs in | No |
| Unknown username `emp_nobody` | `Username not found!` | No |
| Right username, wrong password | `Incorrect Password` | No |
| Username in another case: `EMP_JORDAN` | `Username not found!` | No |
| Log in as `mgr_asha` | `You are using the wrong app. This app is for employees only.`, then exits | No |
| Spaces only, then a valid login | Re-prompts without counting a failure: `Please enter a username and password` | No |
| Start with `EM_DB_PATH` unset | Clear message: `EM_DB_PATH is not set! See README.`, exit code 1 | No |
| `EM_DB_PATH` set to a folder that doesn't exist | Clear message: `Could not access the database.`, exit code 1, no traceback | No |
| Ctrl+C at the Amount prompt | Expected: a short message, no traceback. Actual: full Python traceback ending in `KeyboardInterrupt`, showing file paths. The app closes. | **Yes** |
| Main menu: `6`, `-1`, `1.0`, `1`, `abc`, empty | Invalid input re-prompts with `Menu choice must be a whole number` or `Menu choice must be between 0 and 5`; `1` accepted | No |
| Read the main menu labels | Menu labels match the project's description | No |
| Amount: `1,000`, `+5`, `.5`, `5.`, `007`, `1e3`, `10000.00`, `10000.001` | Only `007` and `10000.00` accepted | No |
| Description: 201 chars with 1 leading space | Accepted (200 after trim) | No |
| Description with emoji and apostrophe: `Client's lunch 🍕` | Saved and shows `Pizza lunch 🍕` in the Employee App | No |
| Category `06`, `3`, `٣` (Arabic digit) | `06` and `3` accepted; `٣` rejected | No |
| Date `2026-11-12`, `2024-02-29`, `2026-2-5`, `2026/11/01` | `2026-11-12` accepted; others rejected | No |
| Edit expense 4 (Omar's) | Clear refusal: `You can only change your own expenses`, nothing changes | No |
| Edit expense 5 (approved) | Clear message: `Expense is no longer pending.` | No |
| Edit expense 999 | Clear not-found message: `Expense #999 not found` | No |
| Edit expense 13, Enter on every field | Nothing changes | No |
| Delete answers `yes`, `Y `, `y`, `n` | Only `y`/`Y` deletes | No |
| View filter `5`, `abc`, empty | Each one re-prompts with `Please enter a number from the menu.` and shows the filter menu again | No |
| History: expenses 5 and 9 | Both show `(no comment)` | No |

### Findings

- **Defects:**
  - KAN-32: Employee App shows a full traceback when the user presses Ctrl+C at a prompt (FR-S4)
- **New test case ideas:**
  - Ctrl+C at any input prompt exits cleanly → TS-05
  - Edit/Delete ID prompt and the View filter have no way to go back to the menu → not added (not in the spec)

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