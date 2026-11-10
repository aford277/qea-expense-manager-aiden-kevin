"""SQL validation query tests (Section 9.3).

Each query must return zero rows on clean seed data, and must catch one
planted bad row (otherwise a broken query that never returns rows would pass).
"""

import sqlite3
from pathlib import Path

import pytest

REPO_ROOT = Path(__file__).resolve().parents[3]
SQL_DIR = REPO_ROOT / "sql"
VALIDATION_DIR = SQL_DIR / "validation"

Q1_NO_USER = "01_expenses_no_user.sql"
Q2_NO_APPROVAL = "02_expenses_no_approval.sql"
Q3_PENDING_REVIEWED = "03_pending_with_review_data.sql"
Q4_DECIDED_INCOMPLETE = "04_decided_missing_review_data.sql"
Q5_CONTRACT = "05_invalid_status_contract.sql"
ALL_QUERIES = [Q1_NO_USER, Q2_NO_APPROVAL, Q3_PENDING_REVIEWED,
               Q4_DECIDED_INCOMPLETE, Q5_CONTRACT]
QUERIES = [VALIDATION_DIR / name for name in ALL_QUERIES]


@pytest.fixture
def db(tmp_path):
    """Temporary database built from schema.sql + seed.sql, removed after the test."""
    conn = sqlite3.connect(tmp_path / "test.db")
    conn.execute("PRAGMA foreign_keys = ON")
    conn.executescript((SQL_DIR / "schema.sql").read_text())
    conn.executescript((SQL_DIR / "seed.sql").read_text())
    yield conn
    conn.close()


def run(conn, query_file):
    return conn.execute(query_file.read_text()).fetchall()


def new_expense(conn, user_id=None, approval=True):
    """Insert a valid pending expense and return its id."""
    user_id = user_id or conn.execute(
        "SELECT id FROM users WHERE role = 'employee' LIMIT 1").fetchone()[0]
    expense_id = conn.execute(
        "INSERT INTO expenses (user_id, category_id, amount, description, expense_date, created_at) "
        "VALUES (?, 1, 25.00, 'Taxi', '2026-11-01', '2026-11-01 09:00:00')", (user_id,)).lastrowid
    if approval:
        conn.execute("INSERT INTO approvals (expense_id) VALUES (?)", (expense_id,))
    return expense_id


def manager(conn):
    return conn.execute("SELECT id FROM users WHERE role = 'manager' LIMIT 1").fetchone()[0]

def bad_no_user(conn):
    conn.execute("PRAGMA foreign_keys = OFF")       # mimic a connection that forgot the pragma
    return new_expense(conn, user_id=99999)


def bad_no_approval(conn):
    return new_expense(conn, approval=False)


def bad_pending_reviewed(conn):
    e = new_expense(conn)
    conn.execute("UPDATE approvals SET reviewer_id = ? WHERE expense_id = ?", (manager(conn), e))
    return e


def bad_decided_no_reviewer(conn):
    e = new_expense(conn)
    conn.execute("UPDATE approvals SET status = 'approved', review_date = '2026-11-09' "
                 "WHERE expense_id = ?", (e,))
    return e


def bad_review_date(conn):
    e = new_expense(conn)
    conn.execute("PRAGMA ignore_check_constraints = ON")   # schema.sql blocks bad dates; bypass it to plant one
    conn.execute("UPDATE approvals SET status = 'denied', reviewer_id = ?, review_date = '2026-02-30' "
                 "WHERE expense_id = ?", (manager(conn), e))
    return e


BAD_ROWS = [bad_no_user, bad_no_approval, bad_pending_reviewed,
            bad_decided_no_reviewer, bad_review_date]   # same order as ALL_QUERIES


@pytest.mark.smoke
@pytest.mark.integration
@pytest.mark.parametrize("query_file", QUERIES, ids=lambda p: p.stem)
def test_query_returns_zero_rows_on_seed_data(db, query_file):
    """Covers: FR-S1, FR-S5 – each validation query returns zero rows on seed data."""
    assert run(db, query_file) == []


@pytest.mark.negative
@pytest.mark.integration
@pytest.mark.parametrize("query_file, plant", list(zip(QUERIES, BAD_ROWS)),
                         ids=[f.__name__ for f in BAD_ROWS])
def test_query_catches_bad_row(db, query_file, plant):
    """Covers: FR-S2, FR-S5 – each validation query flags the bad row planted for it."""
    bad_id = plant(db)
    assert [row[0] for row in run(db, query_file)] == [bad_id]