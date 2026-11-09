# AI Test Review Log

**Project:** Revature Expense Manager (Project 1)
**Requirement:** Section 9.8, AI-assisted test design
**AI tool:** Claude
**Review date:** 2026-11-09
**Shared with the AI:** project requirements document and our designed test cases only (no real passwords or personal data)

## Process

1. We wrote our own 36 designed test cases first.
2. We asked the AI to review them and suggest what we missed.
3. We checked every suggestion against the requirements document: the requirement, the validation rule, and the exact message.
4. Every suggestion we kept or changed is marked `Source = AI-assisted` in the test case sheet.

## Review log

| # | Suggestion | Requirement ID | Decision | Reason | Resulting Test Case ID |
|---|------------|----------------|----------|--------|------------------------|
| 1 | Wrong username or wrong password shows the same error: `Invalid username or password.` | FR-A1 | Kept | Our FR-A1 cases (TA-01, TA-05) only tested a valid login. FR-A1 also says a wrong username or password must show `Invalid username or password.`, and neither case checked it. | TA-09 |
| 2 | Two failed logins and then a correct login succeeds (lockout boundary) | FR-A3 | Kept | TA-03 and TA-06 only test the third failure. BVA should test both sides of the boundary, so this checks that 2 failures do not lock the user out. | TA-07 |
| 3 | Employee edits or deletes another employee's expense through the service and gets `AuthorizationError` | FR-A4, FR-E3, FR-E4 | Kept | FR-A4 applies to both apps, but our only FR-A4 case (TA-04) was in the Java app. This covers the Python side, and also the "someone else's expense" refusal in FR-E3 and FR-E4. | TA-08 |
| 4 | Edit a non-pending expense; it is refused with `Expense is no longer pending.` | FR-E3, FR-S2 | Kept | Our only negative FR-E3 case was XW-03, which is manual. This gives FR-E3 an automated negative test (Section 13) and checks the FR-S2 rule that edits are only allowed while `pending`. | TE-09 |
| 5 | Employee with no expenses sees `No expenses found.` | FR-E2 | Changed | Kept the case, but changed the expected result to "no rows returned". The test calls the service, and printing `No expenses found.` is the menu's job (Section 4.3), so the service should return an empty list rather than a message. | TE-10 |
| 6 | Review history shows `(no comment)` for an empty comment and does not show pending expenses | FR-E5 | Kept | Our original history case only checked that decided expenses appear. FR-E5 also says history is approved and denied only, and an empty comment shows `(no comment)`. TE-11 covers all of this, so it replaced our original history case, which was removed. | TE-11 |
| 7 | Manager sees `No pending expenses.` when nothing is pending | FR-M1 | Kept | FR-M1 lists this exact message and we had no case for it. Our only negative FR-M1 case was XW-04, which is manual. | TM-07 |
| 8 | Run the Section 9.3 validation queries (3, 4, 5) after manager decisions as a designed test case (data contract check) | FR-S5, FR-S2 | Rejected | Duplicate. Section 9.3 already requires these queries in `sql/validation/`, run automatically from PyTest. Adding a designed test case for the same queries adds no new coverage. FR-S5 is already covered by the cross-app cases XW-01 to XW-06. | — |
| 9 | CSV export when the `reports/` folder cannot be written; friendly message shown and report stays on screen | FR-M4 | Rejected | FR-M4 is a Should requirement, and the exit criteria (Section 9.12) only require coverage of Must requirements. TM-06 already covers FR-M4 (run manually). Making a folder read-only works differently on each OS, so this case would be hard to run the same way every cycle. | — |
| 10 | Submit is all-or-nothing: force the `approvals` insert to fail with a mock and check no `expenses` row is left | FR-E1, FR-S4 | Rejected | The rollback comes from the `get_connection` context manager given in Appendix A, so this would mostly test starter code, not our own logic. The FR-E1 rule that both rows are saved together is already checked by TE-01, which asserts both the `expenses` and `approvals` rows exist after a submit. | — |

## Summary

| Decision | Count |
|----------|-------|
| Kept | 6 |
| Changed | 1 |
| Rejected | 3 |
| **Total** | **10** |
