"""Integration tests: login, authorization and the database itself.

Each test gets a fresh temporary database built from schema.sql + seed.sql
(see the db_path / conn fixtures in tests/conftest.py).
"""

import logging
import re
import sqlite3

import pytest

from employee_app import services
from employee_app.exceptions import AuthenticationError, AuthorizationError
from employee_app.services import User
from employee_app.logging_config import setup_logging


# ---------------------------------------------------------------- FR-A1 login

@pytest.mark.smoke
def test_login_valid_employee(conn):
    """Covers: FR-A1 (TA-01) - valid employee login returns that user."""
    user = services.login(conn, "emp_jordan", "jordan123")

    assert user == User(id=3, username="emp_jordan", role="employee", full_name="Jordan Lee")


@pytest.mark.negative
def test_login_wrong_password(conn):
    """Covers: FR-A1 (TA-09) - wrong password raises AuthenticationError."""
    with pytest.raises(AuthenticationError):
        services.login(conn, "emp_jordan", "wrongpass")


# ---------------------------------------------------------------- FR-A4 another employee's expense

@pytest.mark.negative
@pytest.mark.parametrize("action", ["edit", "delete"])
def test_employee_cannot_change_another_employees_expense(conn, employee, seed_today, action):
    """Covers: FR-A4, FR-E3, FR-E4 (TA-08) - emp_jordan can't edit or delete
    emp_maria's pending expense 6, and nothing changes."""
    with pytest.raises(AuthorizationError):
        if action == "edit":
            services.update_expense(conn, employee, 6, "99.00", "1", "Hijacked", "", today=seed_today)
        else:
            services.delete_expense(conn, employee, 6)

    row = conn.execute(
        """SELECT e.user_id, e.amount, e.description, a.status
           FROM expenses e JOIN approvals a ON a.expense_id = e.id
           WHERE e.id = 6"""
    ).fetchone()
    assert tuple(row) == (4, 12.5, "Coffee with new hire", "pending")


# ---------------------------------------------------------------- FR-S1 database setup

@pytest.mark.smoke
def test_seed_data_loads(conn):
    """Covers: FR-S1 (TS-01) - seed.sql loads the minimum data from Section 8.4."""
    def count(sql):
        return conn.execute(sql).fetchone()[0]

    assert count("SELECT COUNT(*) FROM users WHERE role = 'manager'") == 2
    assert count("SELECT COUNT(*) FROM users WHERE role = 'employee'") == 4
    assert count("SELECT COUNT(*) FROM categories") == 6
    assert count("SELECT COUNT(*) FROM expenses") == 25
    # every expense has exactly one approval row
    assert count("""SELECT COUNT(*) FROM expenses e
                    LEFT JOIN approvals a ON a.expense_id = e.id
                    WHERE a.id IS NULL""") == 0
    # boundary rows from Section 8.4
    assert count("SELECT COUNT(*) FROM expenses WHERE amount = 0.01") == 1
    assert count("SELECT COUNT(*) FROM expenses WHERE amount = 10000") == 1
    assert count("SELECT COUNT(*) FROM expenses WHERE length(description) = 200") == 1


BAD_ROWS = {
    "amount_0": """INSERT INTO expenses (user_id, category_id, amount, description, expense_date, created_at)
                   VALUES (3, 1, 0, 'Zero', '2026-10-01', '2026-10-01 09:00:00')""",
    "amount_10000.01": """INSERT INTO expenses (user_id, category_id, amount, description, expense_date, created_at)
                          VALUES (3, 1, 10000.01, 'Too much', '2026-10-01', '2026-10-01 09:00:00')""",
    "role_admin": """INSERT INTO users (username, password, role, full_name)
                     VALUES ('admin_x', 'pw', 'admin', 'Admin X')""",
    "status_uppercase": "UPDATE approvals SET status = 'Approved' WHERE expense_id = 1",
    "second_approval_row": "INSERT INTO approvals (expense_id) VALUES (1)",
    "unknown_user_id": """INSERT INTO expenses (user_id, category_id, amount, description, expense_date, created_at)
                          VALUES (999, 1, 10, 'Nobody', '2026-10-01', '2026-10-01 09:00:00')""",
}


@pytest.mark.negative
@pytest.mark.boundary
@pytest.mark.parametrize("sql", BAD_ROWS.values(), ids=BAD_ROWS.keys())
def test_database_rejects_bad_rows(conn, sql):
    """Covers: FR-S1 (TS-02) - constraints are enforced by the database itself,
    not only by the app (CHECK, UNIQUE and FOREIGN KEY)."""
    with pytest.raises(sqlite3.IntegrityError):
        conn.execute(sql)


# ---------------------------------------------------------------- FR-S3 logging
 
@pytest.fixture
def app_log_file(tmp_path, monkeypatch):
    """Runs the app's real setup_logging() inside a temp folder and returns the log file path.
 
    pytest attaches its own handlers to the root logger, and logging.basicConfig()
    does nothing while any handler is attached - so they are set aside during the
    test and put back afterwards.
    """
    monkeypatch.chdir(tmp_path)                 # logs/employee_app.log is created under tmp_path
    root = logging.getLogger()
    saved_handlers, saved_level = root.handlers[:], root.level
    root.handlers = []
 
    setup_logging()
    yield tmp_path / "logs" / "employee_app.log"
 
    for handler in root.handlers:               # close the app's log file (Windows can't delete open files)
        handler.close()
    root.handlers = saved_handlers
    root.setLevel(saved_level)
 
 
def test_logins_are_logged_without_passwords(conn, app_log_file):
    """Covers: FR-S3 (TS-03) - a successful and a failed login are both written to
    logs/employee_app.log with time, level and message, and neither password appears."""
    services.login(conn, "emp_jordan", "jordan123")
    with pytest.raises(AuthenticationError):
        services.login(conn, "emp_jordan", "wrongpass")
 
    for handler in logging.getLogger().handlers:
        handler.flush()
    lines = app_log_file.read_text(encoding="utf-8").splitlines()
 
    # every entry: "YYYY-MM-DD HH:MM:SS LEVEL logger.name: message"
    assert lines
    for line in lines:
        assert re.fullmatch(r"\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} [A-Z]+ \S+: .+", line)
    assert any(" INFO " in line and "emp_jordan" in line for line in lines)      # successful login
    assert any(" WARNING " in line and "emp_jordan" in line for line in lines)   # failed login
 
    log_text = "\n".join(lines)
    assert "jordan123" not in log_text
    assert "wrongpass" not in log_text
 