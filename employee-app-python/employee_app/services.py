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