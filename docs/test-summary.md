# Test Summary Report

**Project:** Revature Expense Manager (Project 1)
**Requirement:** Section 9.9, item 5
**Prepared by:** Aiden and Kevin · **Date:** 2026-11-12 

## 1. Recommendation

**GO.** Every Must requirement has at least one designed test case and one automated test, all automated and manual test cases pass, and there are no open Critical or High defects (Section 9.12). One known issue remains open: comments with emojis or accented letters are saved as `?` (Medium severity, see Section 5). It affects non-English characters only and has a workaround.

## 2. Results

### Automated tests (final run, cycle 2)

| Suite | Unit | Integration | Total | Passed | Failed | Skipped |
|---|---|---|---|---|---|---|
| Python (PyTest) | 52 | 34 | 86 | 86 | 0 | 0 |
| Java (JUnit 5 + Mockito) | 55 | 41 | 96 | 96 | 0 | 0 |

### Designed test cases

| | Count |
|---|---|
| Designed test cases | 39 |
| Negative or boundary | 22 (56%) 
| Automated | 30 |
| Manual | 9 (XW-01 to XW-06, TA-07, TM-06, TS-04) |

| Cycle | Date | Run | Passed | Failed | Blocked |
|---|---|---|---|---|---|
| 1 | 2026-11-11 | all manual and cross-app cases (9), both automated suites| 9 + both suites | 1 | 0 |
| 2 | 2026-11-12 | cross-app cases (6), failed cases (TS-04), both automated suites | 7 + both suites | 0 | 0 |


### Exploratory testing

Two sessions, recorded in `docs/exploratory-notes.md`:

| Session | App | Tester | Length | Defects found |
|---|---|---|---|---|
| ET-01 | Employee App | Kevin | 20 min | 0 |
| ET-02 | Manager App | Aiden | 30 min | 1 |

## 3. Defects

Full details in `docs/defects.md` and Jira project KAN.

### By severity and status

| Severity | New / Open | Fixed | Closed | Total |
|---|---|---|---|---|
| Critical | 0 | 0 | 0 | 0 |
| High | 0 | 0 | 0 | 0 |
| Medium | 1 | 0 | 0 | 1 |
| Low | 0 | 2 | 2 | 2 |
| **Total** | 1 | 2 | 2 | 3 |

### Defect list

| Jira | Summary | Requirement | Severity | Status | Found by |
|---|---|---|---|---|---|
| [KAN-24] | Missing `EM_DB_PATH` shows "Unexpected error. See log." instead of a clear message | FR-S4 | Low | Closed | TS-04 |
| [KAN-29] | Review re-prompt says "Enter a or d." and leaves out `c` (cancel) | FR-M2 | Low | Closed | Code review |
| [KAN-30] | Emojis and accented letters in comments are saved as `?` | FR-M2, FR-S5 | Medium | Open | Exploratory testing |

## 4. Requirement coverage

All 18 functional requirements (FR-A1 to FR-S5) and all 9 validation rules (VR-01 to VR-09) are in the RTM with at least one designed test case.

| | Requirements | Designed case | Automated test |
|---|---|---|---|
| Must | 17 | 17 / 17 | 17 / 17 |
| Should (FR-M4, CSV export) | 1 | 1 / 1 | manual (TM-06) |
| Validation rules | 9 | 9 / 9 | 9 / 9 |

Coverage of the automated tests was checked with `tools/build_rtm.py`, which reads the `Covers:` line of every test in both apps and lists any Must requirement without an automated test, any test without a `Covers:` line, and any unknown requirement ID. Final run: no gaps. Output: `docs/rtm-automated.md`.

## 5. Known issues

| Issue | Impact | Workaround |
|---|---|---|
| **Emojis and accented letters in comments are saved as `?`** (KAN-30, Medium) | A comment like `Reçu manquant 🧾` is stored and shown with `?` in place of those characters. The user is not warned. Plain-text comments are not affected. | Use plain letters without accents or emojis. Likely cause: the Windows terminal and Java read typed characters with different encodings. |

### Environment notes

These are not defects, but anyone running the project should know them:

- **Java 27:** Mockito tests need `-Dnet.bytebuddy.experimental=true`, already set in `pom.xml` (`argLine`). The suite was also run on Java 21.0.1 without it.
- **Python and SQLite version:** The date checks in `schema.sql` and query 5 work on all tested SQLite versions. 
- **Tested on:** Windows 10, Python 3.15.0 and 3.11.2, Java 21.0.1 and 27, Maven 3.10.0.
