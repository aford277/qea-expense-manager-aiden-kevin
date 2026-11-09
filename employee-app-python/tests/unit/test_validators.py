"""Unit tests for employee_app.validators (VR-01 to VR-04, VR-09)."""

from datetime import date, timedelta
from unittest.mock import patch

import pytest

from employee_app.exceptions import ValidationError
from employee_app.validators import (
    validate_amount,
    validate_category,
    validate_description,
    validate_expense_date,
    validate_menu_choice,
)

# A fixed "today" so date tests give the same result on any day (Section 4.3).
TODAY = date(2026, 11, 9)


def _iso(d):
    return d.strftime("%Y-%m-%d")


# ---------------------------------------------------------------- VR-01 amount (TE-05)

@pytest.mark.boundary
@pytest.mark.parametrize("raw, expected", [
    ("0.01", 0.01),       # lowest valid
    ("10000", 10000.0),   # highest valid
    ("10000.00", 10000.0),
    ("12.5", 12.5),
])
def test_amount_valid(raw, expected):
    """Covers: FR-E1, VR-01 (TE-05) - valid amounts, including both boundaries."""
    assert validate_amount(raw) == expected


@pytest.mark.negative
@pytest.mark.boundary
@pytest.mark.parametrize("raw", [
    "0",          # not greater than 0
    "-5",
    "10000.01",   # just over the max
    "0.001",      # 3 decimals
    "$50",
    "abc",
    "",
])
def test_amount_invalid(raw):
    """Covers: FR-E1, VR-01 (TE-05) - every invalid example from VR-01."""
    with pytest.raises(ValidationError):
        validate_amount(raw)


# ---------------------------------------------------------------- VR-03 category (TE-06)

@pytest.mark.boundary
@pytest.mark.parametrize("raw, expected", [("1", 1), ("6", 6)])
def test_category_valid(raw, expected):
    """Covers: FR-E1, FR-E3, VR-03 (TE-06) - first and last category."""
    assert validate_category(raw) == expected


@pytest.mark.negative
@pytest.mark.boundary
@pytest.mark.parametrize("raw", ["0", "7", "abc", ""])
def test_category_invalid(raw):
    """Covers: FR-E1, FR-E3, VR-03 (TE-06) - just outside the list, and not a number."""
    with pytest.raises(ValidationError):
        validate_category(raw)


# ---------------------------------------------------------------- VR-02 description (TE-07)

@pytest.mark.boundary
@pytest.mark.parametrize("raw, expected", [
    ("a", "a"),                    # 1 char
    ("a" * 200, "a" * 200),        # 200 chars
    ("  Lunch  ", "Lunch"),        # trimmed before checking
])
def test_description_valid(raw, expected):
    """Covers: FR-E1, VR-02 (TE-07) - 1 and 200 characters after trimming."""
    assert validate_description(raw) == expected


@pytest.mark.negative
@pytest.mark.boundary
@pytest.mark.parametrize("raw", ["", "   ", "a" * 201])
def test_description_invalid(raw):
    """Covers: FR-E1, VR-02 (TE-07) - empty, spaces only, 201 characters."""
    with pytest.raises(ValidationError):
        validate_description(raw)


# ---------------------------------------------------------------- VR-04 expense date (TE-08)

@pytest.mark.boundary
@pytest.mark.parametrize("raw, expected", [
    (_iso(TODAY), _iso(TODAY)),                                          # today
    (_iso(TODAY - timedelta(days=90)), _iso(TODAY - timedelta(days=90))),  # exactly 90 days ago
    ("", _iso(TODAY)),                                                   # Enter = today (FR-E1)
])
def test_expense_date_valid(raw, expected):
    """Covers: FR-E1, VR-04 (TE-08) - today, exactly 90 days ago, and Enter = today."""
    assert validate_expense_date(raw, today=TODAY) == expected


@pytest.mark.negative
@pytest.mark.boundary
@pytest.mark.parametrize("raw", [
    _iso(TODAY + timedelta(days=1)),    # tomorrow
    _iso(TODAY - timedelta(days=91)),   # 91 days ago
    "2026-02-30",                       # not a real date
    "12-10-2026",                       # wrong format
])
def test_expense_date_invalid(raw):
    """Covers: FR-E1, VR-04 (TE-08) - every invalid example from VR-04."""
    with pytest.raises(ValidationError):
        validate_expense_date(raw, today=TODAY)


def test_expense_date_enter_uses_real_today():
    """Covers: FR-E1, VR-04 (TE-08) - with no 'today' passed, Enter uses date.today() (mocked)."""
    with patch("employee_app.validators.date") as mock_date:
        mock_date.today.return_value = TODAY
        assert validate_expense_date("") == _iso(TODAY)
        mock_date.today.assert_called_once()


# ---------------------------------------------------------------- VR-09 menu choice (TV-02)

@pytest.mark.boundary
@pytest.mark.parametrize("raw, valid", [("5", True), ("6", False)])
def test_menu_choice_top_of_main_menu(raw, valid):
    """Covers: VR-09 (TV-02) - top of the Employee App main menu (0-5). Not in the shared
    CSV because Java's main menu stops at 3."""
    if valid:
        assert validate_menu_choice(raw, max_choice=5) == int(raw)
    else:
        with pytest.raises(ValidationError):
            validate_menu_choice(raw, max_choice=5)