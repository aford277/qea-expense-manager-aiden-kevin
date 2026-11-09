"""Shared validation cases (Section 9.5).

Reads shared-test-data/validation_cases.csv - the same file the Java suite reads.
Each CSV row is one test case.
"""

import csv
from pathlib import Path

import pytest

from employee_app.exceptions import ValidationError
from employee_app.validators import (
    validate_expense_id,
    validate_login_input,
    validate_menu_choice,
)

# tests/unit/this_file.py -> up 3 folders is the repo root (expense-manager/)
CSV_PATH = Path(__file__).resolve().parents[3] / "shared-test-data" / "validation_cases.csv"

# Each CSV field name -> how the Employee App calls that validator.
# Menu choice uses the Employee App main menu (0-5). The CSV only holds
# inputs that mean the same thing in both apps' main menus.
VALIDATORS = {
    "expense_id": validate_expense_id,
    "menu_choice": lambda value: validate_menu_choice(value, max_choice=5),
    "login": validate_login_input,
}

# What a valid input should come back as.
EXPECTED_RESULT = {
    "expense_id": lambda value: int(value),
    "menu_choice": lambda value: int(value),
    "login": lambda value: value,
}


def _load_cases():
    """Turn every CSV row into a pytest case. Invalid rows get the 'negative' marker."""
    cases = []
    with open(CSV_PATH, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            expected = row["expected"] == "true"
            cases.append(
                pytest.param(
                    row["field"],
                    row["input"],
                    expected,
                    id=f"{row['field']}-{row['input']!r}",
                    marks=() if expected else pytest.mark.negative,
                )
            )
    return cases


@pytest.mark.boundary
@pytest.mark.parametrize("field, value, expected", _load_cases())
def test_shared_validation_cases(field, value, expected):
    """Covers: VR-08, VR-09, FR-A1 (TV-01, TV-02) - shared CSV cases, Section 9.5."""
    validate = VALIDATORS[field]

    if expected:
        assert validate(value) == EXPECTED_RESULT[field](value)
    else:
        with pytest.raises(ValidationError):
            validate(value)