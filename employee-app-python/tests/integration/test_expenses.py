"""Integration tests: submitting, viewing, editing and deleting expenses, and review history.

Seed data used (from sql/seed.sql):
  emp_jordan (id 3) owns expenses 1, 13, 17, 25 (pending) and 5, 9, 21 (approved).
Dates are checked against seed_today (2026-10-28), the day the seed was written for.
"""

import re

import pytest

from employee_app import services
from employee_app.exceptions import InvalidStateError, NotFoundError
from employee_app.services import User


def _expense(conn, expense_id):
    """The expenses + approvals row for one expense, or None if it's gone."""
    return conn.execute(
        """SELECT e.user_id, e.category_id, e.amount, e.description, e.expense_date, e.created_at,
                  a.status, a.reviewer_id, a.comment, a.review_date
           FROM expenses e LEFT JOIN approvals a ON a.expense_id = e.id
           WHERE e.id = ?""",
        (expense_id,),
    ).fetchone()


# ---------------------------------------------------------------- FR-E1 submit

@pytest.mark.smoke
def test_submit_expense_saves_expense_and_pending_approval(conn, employee, seed_today):
    """Covers: FR-E1 (TE-01) - one expenses row and one pending approvals row are
    saved together, and the new ID is returned."""
    new_id = services.submit_expense(conn, employee, "12.5", "2", "Client lunch", "", today=seed_today)

    assert new_id == 26   # seed has 25 expenses
    row = _expense(conn, new_id)
    assert (row["user_id"], row["category_id"], row["amount"], row["description"], row["expense_date"]) \
        == (3, 2, 12.5, "Client lunch", "2026-10-28")   # Enter = today
    assert re.fullmatch(r"\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}", row["created_at"])
    
    # pending: no reviewer, comment or review date (Section 8.3)
    assert (row["status"], row["reviewer_id"], row["comment"], row["review_date"]) \
        == ("pending", None, None, None)
    assert conn.execute("SELECT COUNT(*) FROM approvals WHERE expense_id = ?", (new_id,)).fetchone()[0] == 1


# ---------------------------------------------------------------- FR-E2 view

@pytest.mark.smoke
def test_view_all_my_expenses_newest_first(conn, employee):
    """Covers: FR-E2 (TE-02) - only my expenses, newest date first, with the FR-E2 columns."""
    expenses = services.list_my_expenses(conn, employee)

    assert [e["id"] for e in expenses] == [25, 17, 13, 1, 9, 21, 5]
    assert expenses[0] == {"id": 25, "date": "2026-10-26", "category": "Training",
                           "amount": 300.0, "status": "pending"}


@pytest.mark.negative
@pytest.mark.parametrize("status", [None, "pending", "approved", "denied"])
def test_employee_with_no_expenses_gets_empty_list(conn, status):
    """Covers: FR-E2 (TE-10) - an employee with no expenses gets an empty list,
    for all and for each status filter. (The menu then prints 'No expenses found.')"""
    conn.execute("INSERT INTO users (id, username, password, role, full_name) "
                 "VALUES (7, 'emp_new', 'new123', 'employee', 'New Person')")
    new_employee = User(id=7, username="emp_new", role="employee", full_name="New Person")

    assert services.list_my_expenses(conn, new_employee, status) == []


# ---------------------------------------------------------------- FR-E3 edit

def test_edit_pending_expense_changes_only_given_fields(conn, employee, seed_today):
    """Covers: FR-E3, FR-S2 (TE-03) - the changed field is saved, the kept fields stay
    the same, and the expense is still pending."""
    current = services.get_pending_expense(conn, employee, 13)

    # Change the amount; pass the current values for the rest, as the menu does on Enter
    services.update_expense(conn, employee, 13, "30.00", current["category_id"],
                            current["description"], current["date"], today=seed_today)

    row = _expense(conn, 13)
    assert (row["amount"], row["category_id"], row["description"], row["expense_date"], row["status"]) \
        == (30.0, 4, "Printer paper and pens", "2026-10-12", "pending")


@pytest.mark.negative
def test_edit_non_pending_expense_is_refused(conn, employee, seed_today):
    """Covers: FR-E3, FR-S2 (TE-09) - editing approved expense 5 raises
    InvalidStateError 'Expense is no longer pending.' and nothing changes."""
    with pytest.raises(InvalidStateError, match="Expense is no longer pending."):
        services.update_expense(conn, employee, 5, "50.00", "1", "Changed", "2026-10-28", today=seed_today)

    row = _expense(conn, 5)
    assert (row["amount"], row["status"]) == (245.6, "approved")


# ---------------------------------------------------------------- FR-E4 delete

def test_delete_pending_expense_removes_both_rows(conn, employee):
    """Covers: FR-E4, FR-S2 (TE-04) - the expense and its approval row are removed together."""
    services.delete_expense(conn, employee, 13)

    assert _expense(conn, 13) is None
    assert conn.execute("SELECT COUNT(*) FROM approvals WHERE expense_id = 13").fetchone()[0] == 0


@pytest.mark.negative
def test_missing_expense_is_not_found(conn, employee):
    """Covers: FR-E3, FR-E4 - an expense ID that doesn't exist raises NotFoundError."""
    with pytest.raises(NotFoundError):
        services.delete_expense(conn, employee, 999)


# ---------------------------------------------------------------- FR-E5 review history

def test_review_history_excludes_pending_newest_review_first(conn, employee):
    """Covers: FR-E5 (TE-11) - only approved/denied, newest review first,
    reviewer name shown, and an empty or NULL comment shows '(no comment)'."""
    history = services.list_review_history(conn, employee)

    # jordan's pending expenses 1, 13, 17, 25 are not shown
    assert [h["id"] for h in history] == [9, 21, 5]
    assert [h["review_date"] for h in history] == ["2026-09-24", "2026-08-21", "2026-08-03"]
    assert {h["status"] for h in history} == {"approved"}
    assert {h["reviewer"] for h in history} == {"Daniel Kim"}
    assert history[0]["comment"] == "(no comment)"    # expense 9: empty-string comment
    assert history[1]["comment"] == "Approved per training plan"
    assert history[2]["comment"] == "(no comment)"    # expense 5: NULL comment