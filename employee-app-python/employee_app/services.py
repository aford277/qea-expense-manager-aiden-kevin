import logging
import sqlite3
from dataclasses import dataclass
from datetime import date, datetime

from employee_app.exceptions import (
    AuthenticationError,
    AuthorizationError,
    ValidationError,
    DatabaseError
)
from employee_app.validators import (
    validate_amount,
    validate_category,
    validate_description,
    validate_expense_date,
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

# FR-E1: submit

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
                       (user_id, category_id, amount, description, expense_date, created_at)
                   VALUES (?, ?, ?, ?, ?, ?)""",
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