"""Menu-flow test for the Employee App (Section 9.1).

Drives menu.login_prompt with scripted keyboard input (mocked input and getpass)
against a real temporary database.
"""

from unittest.mock import patch

import pytest

from employee_app import menu


@pytest.mark.negative
@pytest.mark.boundary
def test_three_failed_logins_lock_out(conn, capsys):
    """Covers: FR-A3 (TA-06) - the third wrong password in a row prints
    'Too many failed attempts.' and exits with code 1."""
    with patch("builtins.input", side_effect=["emp_jordan", "emp_jordan", "emp_jordan"]), \
         patch("employee_app.menu.getpass.getpass", side_effect=["wrong1", "wrong2", "wrong3"]) as fake_getpass:

        with pytest.raises(SystemExit) as exit_info:
            menu.login_prompt(conn)

    assert exit_info.value.code == 1
    assert "Too many failed attempts." in capsys.readouterr().out
    assert fake_getpass.call_count == 3   # exactly 3 attempts: not locked out early, not allowed a 4th