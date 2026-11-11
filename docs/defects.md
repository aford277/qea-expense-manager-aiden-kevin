# Defect Log

**Project:** Revature Expense Manager (Project 1)
**Requirement:** Section 9.10 (fields from Appendix C)

## Summary

| ID | Summary | Requirement | Severity | Priority | Status |
|----|---------|-------------|----------|----------|--------|
| KAN-24 | Missing `EM_DB_PATH` shows "Unexpected error. See log." instead of a clear message | FR-S4 | Low | Medium | [New] |
| KAN-29 | Review re-prompt says "Enter a or d." and leaves out the `c` (cancel) option | FR-M2 | Low | Low | Fixed |

---

## KAN-24: Missing `EM_DB_PATH` shows "Unexpected error. See log." instead of a clear message

| Field | Value |
|-------|-------|
| **Requirement** | FR-S4 (Friendly error handling) |
| **Test case** | TS-04 |
| **Severity** | Low: the app still refuses to start and exits with code 1; only the message is wrong. Workaround: the log and README point to the cause. |
| **Priority** | Medium: it breaks part of a Must requirement, and the fix is small. |
| **Environment** | OS: Windows 10 · Java: 21.0.1 · Maven: 3.10.0 · Commit: 38e6473 |
| **Status** | [New] |
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

**Retest result:** [not retested yet]

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

**Retest result:** System.out.println("Enter a, d, or c.");