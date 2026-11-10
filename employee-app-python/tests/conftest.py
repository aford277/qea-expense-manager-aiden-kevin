"""Shared PyTest fixtures for the Employee App tests (Section 9.2).

Every integration test gets its own temporary database, built from the real
sql/schema.sql and sql/seed.sql, and deleted again after the test.
Tests never use EM_DB_PATH or the real expense_manager.db.
"""

import sqlite3
from datetime import date
from pathlib import Path

import pytest

from employee_app.db import get_connection
from employee_app.services import User

# tests/conftest.py -> up 2 folders is the repo root (expense-manager/)
REPO_ROOT = Path(__file__).resolve().parents[2]
SCHEMA_SQL = REPO_ROOT / "sql" / "schema.sql"
SEED_SQL = REPO_ROOT / "sql" / "seed.sql"

# seed.sql dates were picked relative to this day. Pass it as `today` to services
# so date checks (VR-04) give the same result no matter when the tests run.
SEED_TODAY = date(2026, 10, 28)


def _build_database(path, with_seed=True):
    """Create a database file from schema.sql (and seed.sql)."""
    raw = sqlite3.connect(path)
    try:
        raw.executescript(SCHEMA_SQL.read_text(encoding="utf-8"))
        if with_seed:
            raw.executescript(SEED_SQL.read_text(encoding="utf-8"))
        raw.commit()
    finally:
        raw.close()


# ---------------------------------------------------------------- databases

@pytest.fixture
def db_path(tmp_path):
    """Path to a fresh temporary database with schema + seed data.
    Created before each test and removed after it."""
    path = tmp_path / "test_expense_manager.db"
    _build_database(path)
    yield path
    path.unlink(missing_ok=True)


@pytest.fixture
def conn(db_path):
    """An open connection to the temporary database, set up exactly like the app's
    (foreign keys on, rows by column name). Closed after the test."""
    with get_connection(str(db_path)) as connection:
        yield connection


# ---------------------------------------------------------------- users (from seed.sql)

@pytest.fixture
def employee():
    """emp_jordan (id 3). Owns seed expenses 1, 5, 9, 13, 17, 21, 25."""
    return User(id=3, username="emp_jordan", role="employee", full_name="Jordan Lee")


@pytest.fixture
def seed_today():
    """The 'today' the seed data was written for (2026-10-28)."""
    return SEED_TODAY


# ---------------------------------------------------------------- markers

def pytest_collection_modifyitems(items):
    """Every test in tests/integration/ or tests/sql/ gets the 'integration' marker
    automatically, so `pytest -m integration` / `-m "not integration"` work."""
    for item in items:
        parts = Path(str(item.fspath)).parts
        if "integration" in parts or "sql" in parts:
            item.add_marker(pytest.mark.integration)