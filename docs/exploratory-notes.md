# Exploratory Testing Notes

Section 9.7: 2 sessions of about 30 minutes each, one per app.
Defects found here are logged in Jira (project KAN). Good ideas become designed test cases.

| Session | App | Tester | Date | Length |
|---------|-----|--------|------|--------|
| ET-01 | Employee App (Python) | Kevin | 2026-11-12 | 20 min |
| ET-02 | Manager App (Java) | Aiden | 2026-11-__ | __ min |

---

## ET-01: Employee App (Python)

**Charter:** Explore login, submit, edit and delete in the Employee App with unusual input and interruptions to find validation and error-handling gaps.

**Setup**
- Fresh database built from `sql/schema.sql` and `sql/seed.sql`, with `EM_DB_PATH` set.
- Run from `employee-app-python/`: `python -m employee_app.main`
- Users: `emp_jordan` / `jordan123` (employee), `mgr_asha` / `asha123` (manager).
- Jordan's expenses: pending 1, 13, 17, 25; approved 5, 9, 21. Expense 4 is Omar's (pending).

### What I tried

| # | Area | What I tried | Expected (spec) | What happened | Bug? |
|---|------|--------------|-----------------|---------------|------|
| 1 | Login (FR-A1) | Username with spaces around it: `  emp_jordan  ` | Logs in, or `Invalid username or password.` | | |
| 2 | Login (FR-A1) | Unknown username `emp_nobody` | `Invalid username or password.` | | |
| 3 | Login (FR-A1) | Right username, wrong password | Same message as #2 | | |
| 4 | Login (FR-A1) | Username in a different case: `EMP_JORDAN` | `Invalid username or password.` | | |
| 5 | Login (FR-A2) | Log in as `mgr_asha` | `This account does not have access to this app.`, then the app exits | | |
| 6 | Login (FR-A1) | Spaces only, then a valid login (does a blank count as a failure?) | Re-prompts without counting a failure | | |
| 7 | Startup (FR-S4) | Unset `EM_DB_PATH` (`unset EM_DB_PATH`) and start the app | Clear message naming `EM_DB_PATH`, exit code 1 (`echo $?`) | | |
| 8 | Startup (FR-S4) | Set `EM_DB_PATH` to a folder that doesn't exist | Clear message, exit code 1, no traceback | | |
| 9 | Any prompt (FR-S4) | Press Ctrl+C at the Amount prompt | Short message, no traceback | | |
| 10 | Main menu (VR-09) | `6`, `-1`, `1.0`, ` 1 `, `abc`, empty | Re-prompts for invalid input; ` 1 ` is accepted | | |
| 11 | Main menu (6.2) | Read the menu labels | `Submit expense`, `View my expenses`, `Edit pending expense`, `Delete pending expense`, `Review history`, `Exit` | | |
| 12 | Submit (VR-01) | `1,000`, `+5`, `.5`, `5.`, `007`, `1e3`, `10000.00`, `10000.001` | Only `007` (= 7) and `10000.00` are accepted | | |
| 13 | Submit (VR-02) | 201 characters with 1 leading space, so 200 after trimming | Accepted (200 after trim) | | |
| 14 | Submit (VR-02) | Emoji and an apostrophe: `Client's lunch 🍕` | Saved and shown exactly | | |
| 15 | Submit (VR-03) | Category `06`, ` 3 `, `٣` (Arabic digit) | `06` and ` 3 ` are accepted; `٣` is rejected | | |
| 16 | Submit (VR-04) | `2026-11-12` (today), `2024-02-29`, `2026-2-5`, `2026/11/01` | Today is accepted; the others are rejected with the rule | | |
| 17 | Edit (FR-E3) | Edit expense 4 (Omar's) | Clear refusal, nothing changes | | |
| 18 | Edit (FR-E3) | Edit expense 5 (approved) | `Expense is no longer pending.` | | |
| 19 | Edit (FR-E3) | Edit expense 999 | Clear "not found" message | | |
| 20 | Edit (FR-E3) | Press Enter on every field | Nothing changes; check with View | | |
| 21 | Delete (FR-E4) | Answer `yes`, `Y `, ` y`, `n` | Only `y`/`Y` deletes. Note what happens with spaces around `Y` | | |
| 22 | View (FR-E2) | Filter `5`, `abc`, empty | Re-prompts | | |
| 23 | History (FR-E5) | View history, check expenses 5 and 9 | Both show `(no comment)` | | |
| 24 | Logging (FR-S3) | After the session, open `logs/employee_app.log` | Logins logged, no passwords anywhere | | |

### What I found

| # | Finding | Requirement | Severity | Jira |
|---|---------|-------------|----------|------|
| | | | | |

### Ideas for new test cases

| Idea | Requirement | Turned into |
|------|-------------|-------------|
| | | |

### Notes / questions
-

---

## ET-02: Manager App (Java)

**Charter:** _(Aiden: one line)_

### What I tried

| # | Area | What I tried | Expected (spec) | What happened | Bug? |
|---|------|--------------|-----------------|---------------|------|
| 1 | | | | | |

### What I found

| # | Finding | Requirement | Severity | Jira |
|---|---------|-------------|----------|------|
| | | | | |

### Ideas for new test cases

| Idea | Requirement | Turned into |
|------|-------------|-------------|
| | | |
