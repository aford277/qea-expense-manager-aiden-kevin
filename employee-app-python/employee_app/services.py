import logging
import sqlite3
from dataclasses import dataclass
from datetime import date, datetime

from employee_app.exceptions import (
    AuthenticationError,
    AuthorizationError,
    ValidationError,
    DatabaseError,
    NotFoundError,
    InvalidStateError,
)
from employee_app.validators import (
    validate_amount,
    validate_category,
    validate_description,
    validate_expense_date,
    validate_expense_id,
)

logger = logging.getLogger(__name__)

VALID_STATUSES = ("pending", "approved", "denied")

@dataclass
class User:
    """The logged in user"""
    id: int
    username: str
    role: str
    full_name: str

def _require_employee(user):
    """FR-A4: services check who is acting, not just the menus."""
    if user is None or user.role != "employee":
        logger.warning("Authorization failed: non-employee called an employee service")
        raise AuthorizationError("This action is only available to employees.")

def login(conn, username, password):
    """Check credentials and role. Return a User on success.
 
    Raises:
        AuthenticationError: "Invalid username or password."  (FR-A1)
            Same message for an unknown username and a wrong password.
        AuthorizationError: "This account does not have access to this app."
            when the role is not 'employee'.  (FR-A2)
 
    Note: empty-input re-prompting and the 3-attempt counter (FR-A3)
    belong in menu.py, not here.
    """

    # Get user info
    row = conn.execute(
        "SELECT id, username, password, role, full_name FROM users WHERE username = ?",
        (username,),
    ).fetchone()

    # Check if username is not found or incorrect password
    if row is None:
        logger.warning("User entered %s as username but they do not exist!", username)
        raise AuthenticationError("Username not found!")
    elif row["password"] != password:
        logger.warning("%s user entered wrong password.", username)
        raise AuthenticationError("Incorrect password.")

    # Check is user is actually a manager
    if row["role"] != "employee":
        logger.warning("%s is on the wrong app! They are not an employee.", username)
        raise AuthorizationError("You are using the wrong app. This app is for employees only.")

    # Log in succeeded
    logger.info("Login succeeded for %s", username)
    return User(
        id=row["id"],
        username=row["username"],
        role=row["role"],
        full_name=row["full_name"]
    )

def list_my_expenses(conn, user, status=None):
    """Return the logged-in user's expenses, newest date first.
    status=None returns all; otherwise one of VALID_STATUSES. (FR-E2)"""
    # Make sure the user is an employee (not manager)
    _require_employee(user)
    # TEMP (waiting for validators)
    if status is not None and status not in VALID_STATUSES:
        raise ValidationError(f"Invalid status filter: {status}")

    sql = """
        SELECT e.id, e.expense_date, c.name AS category, e.amount, a.status
        FROM expenses e
        JOIN categories c ON c.id = e.category_id
        JOIN approvals  a ON a.expense_id = e.id
        WHERE e.user_id = ?
    """
    params = [user.id]
    if status is not None:
        sql += " AND a.status = ?"
        params.append(status)
    sql += " ORDER BY e.expense_date DESC, e.id DESC"

    try:
        rows = conn.execute(sql, params).fetchall()
    except sqlite3.Error as exc:
        logger.error("Failed to list expenses for user %s: %s", user.id, exc)
        raise DatabaseError("Could not load your expenses.") from exc

    return [
        {
            "id": r["id"],
            "date": r["expense_date"],
            "category": r["category"],
            "amount": round(r["amount"], 2),
            "status": r["status"],
        }
        for r in rows
    ]

def list_review_history(conn, user):
    """Return the user's approved and denied expenses, newest review first.
    (FR-E5, FR-A4)"""
    _require_employee(user)

    sql = """
        SELECT e.id, e.expense_date, c.name AS category, e.amount,
               a.status, r.full_name AS reviewer, a.review_date, a.comment
        FROM expenses e
        JOIN categories c ON c.id = e.category_id
        JOIN approvals  a ON a.expense_id = e.id
        LEFT JOIN users r ON r.id = a.reviewer_id
        WHERE e.user_id = ?
          AND a.status IN ('approved', 'denied')
        ORDER BY a.review_date DESC, e.id DESC
    """
    try:
        rows = conn.execute(sql, (user.id,)).fetchall()
    except sqlite3.Error as exc:
        logger.error("Failed to load review history for user %s: %s", user.id, exc)
        raise DatabaseError("Could not load your review history.") from exc


    return [
        {
            "id": r["id"],
            "date": r["expense_date"],
            "category": r["category"],
            "amount": round(r["amount"], 2),
            "status": r["status"],
            "reviewer": r["reviewer"] or "(unknown)",
            "review_date": r["review_date"],
            "comment": (r["comment"] or "").strip() or "(no comment)",
        }
        for r in rows
    ]

# FR-E1: submit, FR-E3: edit

def list_categories(conn):
    """Return all categories as [{'id', 'name'}], ordered by id (FR-E1 numbered list)."""
    try:
        rows = conn.execute("SELECT id, name FROM categories ORDER BY id").fetchall()
    except sqlite3.Error as exc:
        logger.error("Failed to load categories: %s", exc)
        raise DatabaseError("Could not load categories.") from exc
    return [{"id": r["id"], "name": r["name"]} for r in rows]

def _validate_expense_fields(amount, category_id, description, expense_date, today=None):
    """Run VR-01 to VR-04. Services re-check values even if the menu already did."""
    return (
        validate_amount(amount),
        validate_category(category_id),
        validate_description(description),
        validate_expense_date(expense_date, today),
    )

def submit_expense(conn, user, amount, category_id, description, expense_date, today=None):
    """Save a new expense and 'pending' approval row in ONE transaction.
    Returns the new expense ID. (FR-E1, FR-A4, VR-01 to VR-04)

    An empty expense_date means today (Enter = today).
    """
    _require_employee(user)
    amount, category_id, description, expense_date = _validate_expense_fields(
        amount, category_id, description, expense_date, today
    )
    created_at = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    try:
        with conn:  # commits both inserts together, or rolls both back
            cur = conn.execute(
                """INSERT INTO expenses
                       (user_id, category_id, amount, description, expense_date, created_at) VALUES (?, ?, ?, ?, ?, ?)""",
                (user.id, category_id, amount, description, expense_date, created_at),
            )
            expense_id = cur.lastrowid
            conn.execute(
                "INSERT INTO approvals (expense_id, status) VALUES (?, 'pending')",
                (expense_id,),
            )
    except sqlite3.Error as exc:
        logger.error("Failed to submit expense for user %s: %s", user.username, exc)
        raise DatabaseError("Could not save your expense.") from exc

    logger.info("User %s submitted expense %s (amount %.2f)", user.username, expense_id, amount)
    return expense_id

def _check_own_pending(conn, user, expense_id):
    """Return the expense row if it exists, belongs to the user, and is pending.

    Raises:
        NotFoundError: the expense does not exist.
        AuthorizationError: the expense belongs to someone else.  (FR-A4)
        InvalidStateError: "Expense is no longer pending."  (FR-E3, FR-S2)
    """
    try:
        row = conn.execute(
            """SELECT e.id, e.user_id, e.amount, e.category_id, c.name AS category, e.description, e.expense_date, a.status
               FROM expenses e
               JOIN categories c ON c.id = e.category_id
               LEFT JOIN approvals a ON a.expense_id = e.id
               WHERE e.id = ?""",
            (expense_id,),
        ).fetchone()
    except sqlite3.Error as exc:
        logger.error("Failed to load expense %s: %s", expense_id, exc)
        raise DatabaseError("Could not load the expense.") from exc

    if row is None:
        raise NotFoundError(f"Expense #{expense_id} not found.")
    if row["user_id"] != user.id:
        logger.warning("User %s tried to change expense %s owned by another user", user.username, expense_id)
        raise AuthorizationError("You can only change your own expenses.")
    if row["status"] != "pending":
        raise InvalidStateError("Expense is no longer pending.")
    return row

def get_pending_expense(conn, user, expense_id):
    """Return the current values of the user's own pending expense,
    so the menu can show them before an edit or delete. (FR-E3, FR-E4)"""
    _require_employee(user)
    expense_id = validate_expense_id(expense_id)
    row = _check_own_pending(conn, user, expense_id)
    return {
        "id": row["id"],
        "amount": round(row["amount"], 2),
        "category_id": row["category_id"],
        "category": row["category"],
        "description": row["description"],
        "date": row["expense_date"],
    }

def update_expense(conn, user, expense_id, amount, category_id, description, expense_date, today=None):
    """Update the user's own pending expense. Same validation as submit. (FR-E3)

    The UPDATE only matches if the expense is still pending at the moment of saving,
    so a manager decision made while the employee was typing is caught (XW-03).
    """
    _require_employee(user)
    expense_id = validate_expense_id(expense_id)
    amount, category_id, description, expense_date = _validate_expense_fields(amount, category_id, description, expense_date, today)

    try:
        with conn:
            cur = conn.execute(
                """UPDATE expenses
                   SET amount = ?, category_id = ?, description = ?, expense_date = ?
                   WHERE id = ? AND user_id = ?
                     AND EXISTS (SELECT 1 FROM approvals
                                 WHERE expense_id = ? AND status = 'pending')""",
                (amount, category_id, description, expense_date,
                 expense_id, user.id, expense_id),
            )
    except sqlite3.Error as exc:
        logger.error("Failed to update expense %s: %s", expense_id, exc)
        raise DatabaseError("Could not update the expense.") from exc

    if cur.rowcount == 0:
        _check_own_pending(conn, user, expense_id)  # raises the specific reason
        raise InvalidStateError("Expense is no longer pending.")

    logger.info("User %s edited expense %s", user.username, expense_id)