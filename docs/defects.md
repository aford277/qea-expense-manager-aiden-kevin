# Defect Log

**Project:** Revature Expense Manager (Project 1)
**Requirement:** Section 9.10 (fields from Appendix C)

## Summary

| ID | Summary | Requirement | Severity | Priority | Status |
|----|---------|-------------|----------|----------|--------|
| KAN-24 | Missing `EM_DB_PATH` shows "Unexpected error. See log." instead of a clear message | FR-S4 | Low | Medium | Fixed |
| KAN-29 | Review re-prompt says "Enter a or d." and leaves out the `c` (cancel) option | FR-M2 | Low | Low | Fixed |
| KAN-30 | Approve and deny comments replace characters like é or emojis without mentioning it. | FR-M2 | Low | Low | [New] |
| KAN-32 | Employee App shows a full traceback when the user presses Ctrl+C at a prompt | FR-S4 | Low | Medium | [New] |

---

## KAN-24: Missing `EM_DB_PATH` shows "Unexpected error. See log." instead of a clear message

| Field | Value |
|-------|-------|
| **Requirement** | FR-S4 (Friendly error handling) |
| **Test case** | TS-04 |
| **Severity** | Low: the app still refuses to start and exits with code 1; only the message is wrong. Workaround: the log and README point to the cause. |
| **Priority** | Medium: it breaks part of a Must requirement, and the fix is small. |
| **Environment** | OS: Windows 10 · Java: 21.0.1 · Maven: 3.10.0 · Commit: 38e6473 |
| **Status** | Fixed |
| **Reporter** | Aiden |
| **Assignee** | Aiden |

**Steps to reproduce**
1. Open a new terminal and make sure `EM_DB_PATH` is not set (PowerShell: `Remove-Item Env:EM_DB_PATH`).
2. From `manager-app-java/`, run `mvn compile exec:java`.
3. Note the message and the exit code (PowerShell: `$LASTEXITCODE`).

**Expected:** A clear message saying `EM_DB_PATH` is not set, and exit code 1 (FR-S4: "If `EM_DB_PATH` is missing ... show a clear message and exit with code 1").

**Actual:** `Unexpected error. See log.` is shown, and the exit code is 1.

**Cause:** `Database.dbPath()` throws `IllegalStateException("EM_DB_PATH is not set.")`, but `Main` only has a general `catch (Exception e)`, so it is reported as an unexpected error.

**Evidence:** KAN_24_Error.png in docs\evidence

**Retest result:** KAN_24_Error.png in docs\evidence

---

## KAN-29: Review re-prompt says "Enter a or d." and leaves out the `c` (cancel) option

| Field | Value |
|-------|-------|
| **Requirement** | FR-M2 (Approve or deny a pending expense) |
| **Test case** | Found during manual testing |
| **Severity** | Low: cosmetic; `c` still cancels when typed. |
| **Priority** | Low: usability only, no requirement is broken. |
| **Environment** | OS: Windows 10 · Java: 21.0.1 · Maven: 3.10.0 · Commit: 38e6473 |
| **Status** | Fixed |
| **Reporter** | Aiden |
| **Assignee** | Aiden |

**Steps to reproduce**
1. Run the Manager App and log in as `mgr_asha` / `asha123`.
2. Choose `2` (Review an expense) and enter `25` (a pending expense in the seed data).
3. At `Approve or deny? (a/d, c to cancel):`, type `x`.

**Expected:** The re-prompt lists every valid option, the same as the prompt: `a`, `d`, or `c` to cancel.

**Actual:** `Enter a or d.` is shown. `c` is not mentioned, even though it still works.

**Cause:** In `Menu.reviewExpense()`, the error message for an invalid choice is `"Enter a or d."`.

**Evidence:** KAN_24_Error.png in docs\evidence

**Retest result:** KAN_24_Error.png in docs\evidence

---

## KAN-30: Approve and deny comments replace characters like é or emojis without mentioning it.

| Field | Value |
|-------|-------|
| **Requirement** | FR-M2 (Approve or deny a pending expense) |
| **Test case** | Found during exploratory testing |
| **Severity** | Medium: The app silently changes what the user typed. |
| **Priority** | Medium: It is something that is common, especially in other languages or names. |
| **Environment** | OS: Windows 10 · Java: 21.0.1 · Maven: 3.10.0 · Commit: 38e6473 |
| **Status** | New |
| **Reporter** | Aiden |
| **Assignee** |  |

**Steps to reproduce**
1. Log in as a manager
2. Approve or deny an expense
3. Write é  as a part of the comment
4. Log in as the employee the expense belongs to
5. Review the employees expenses

**Expected:** The comment to be saved exactly as it was written.

**Actual:** Actual: é  is replaced by �.

**Cause:** Terminal converts characters to bytes, not as UTF-8.

**Evidence:** KAN_30_Error1.png and KAN_30_Error2.png in docs\evidence

**Retest result:** 

---

## KAN-32: Employee App shows a full traceback when the user presses Ctrl+C at a prompt

| Field | Value |
|-------|-------|
| **Requirement** | FR-S4 (Friendly error handling) |
| **Test case** | Found during exploratory testing |
| **Severity** | Low: the user chose to quit; nothing is saved halfway and no data is lost, but the traceback breaks FR-S4 and exposes local file paths. |
| **Priority** | Medium: it clearly breaks the spec, and the fix is small (one extra `except` block). |
| **Environment** | OS: Windows 11 · Python: 3.15.2 · Shell: PowerShell · Commit: 6a929d5 |
| **Status** | New |
| **Reporter** | Kevin |
| **Assignee** |  |

**Steps to reproduce**
1. Set `EM_DB_PATH` to the database and start the Employee App: `python -m employee_app.main`
2. Log in as `emp_jordan` / `jordan123`
3. Choose `1` (Submit expense)
4. At the `Amount:` prompt, press Ctrl+C

**Expected:** A short message (for example `Exiting.`) and the app closes, with no traceback.

**Actual:** A full Python traceback ending in `KeyboardInterrupt` is printed, showing the local file paths (`main.py`, `menu.py`), and then the app closes.

**Cause:** `KeyboardInterrupt` is not a subclass of `Exception`, so the `except Exception:` block in `main.py` does not catch it.

**Evidence:** KAN_32_Error.png in docs\evidence

**Retest result:**