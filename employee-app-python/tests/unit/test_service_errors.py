"""Unit test: a database failure must become DatabaseError (FR-S4).

The connection is a MagicMock whose execute() always fails, so no real database is used.
"""

import sqlite3
from unittest.mock import MagicMock

import pytest

from employee_app import services
from employee_app.exceptions import DatabaseError


@pytest.mark.negative
def test_database_failure_becomes_database_error(employee):
    """Covers: FR-S4 - an sqlite3 error is turned into DatabaseError (a short message
    for the menu), keeping the original error as the cause for the log."""
    broken_conn = MagicMock()
    broken_conn.execute.side_effect = sqlite3.OperationalError("disk I/O error")

    with pytest.raises(DatabaseError) as error:
        services.list_my_expenses(broken_conn, employee)

    assert isinstance(error.value.__cause__, sqlite3.Error)