import re
from datetime import datetime, date, timedelta
from employee_app.exceptions import ValidationError


def _is_ascii_digits(text: str) -> bool:
    """True only for 0-9 characters (rejects '²', '١', etc.)."""
    return text.isascii() and text.isdigit()


def validate_amount(amount_input) -> float:
    """
    VR-01: Amount must be greater than 0, at most 10,000.00, at most 2 decimals,
    digits and one decimal point only.
    """
    amt_str = "" if amount_input is None else str(amount_input).strip()
    if not amt_str:
        raise ValidationError("Amount can't be empty.")

    # Regex makes sure digits (0-9 only), optional single decimal point, and 1-2 decimal digits
    if not re.fullmatch(r"[0-9]+(\.[0-9]{1,2})?", amt_str):
        raise ValidationError("Invalid amount format. Must be a positive number with at most 2 decimals.")

    val = float(amt_str)

    if val <= 0:
        raise ValidationError("Amount must be greater than 0.")
    if val > 10000.00:
        raise ValidationError("Amount can't exceed $10,000.00.")

    return round(val, 2)


def validate_description(desc_input) -> str:
    """VR-02: Description must be 1-200 characters after trimming"""
    if desc_input is None:
        raise ValidationError("Description cannot be empty")

    desc = str(desc_input).strip()
    if not desc:
        raise ValidationError("Description cannot be empty or spaces only.")
    if len(desc) > 200:
        raise ValidationError("Description can't exceed 200 characters.")
    return desc


def validate_category(cat_input) -> int:
    """
    VR-03: Category must be a whole number from 1 to 6.
    """
    if cat_input is None:
        raise ValidationError("Category cannot be empty.")

    cat_str = str(cat_input).strip()
    if not _is_ascii_digits(cat_str):
        raise ValidationError("Category must be a whole number between 1 and 6.")

    cat_id = int(cat_str)
    if not (1 <= cat_id <= 6):
        raise ValidationError("Category must be between 1 and 6.")

    return cat_id


def validate_expense_date(date_input, today: date = None) -> str:
    """
    VR-04: Expense date must be YYYY-MM-DD, a real date, not in the future,
    and not more than 90 days ago.
    If date_input is empty/blank, returns today's date string (Enter = today).
    """
    if today is None:
        today = date.today()

    if date_input is None or str(date_input).strip() == "":
        return today.strftime("%Y-%m-%d")

    date_str = str(date_input).strip()

    # Strict YYYY-MM-DD format first (strptime alone accepts 2026-1-5)
    if not re.fullmatch(r"[0-9]{4}-[0-9]{2}-[0-9]{2}", date_str):
        raise ValidationError("Invalid date format. Must be YYYY-MM-DD.")

    # Then check it is a real calendar date (rejects 2026-02-30)
    try:
        parsed_date = datetime.strptime(date_str, "%Y-%m-%d").date()
    except ValueError:
        raise ValidationError(f"{date_str} is not a real date.") from None

    if parsed_date > today:
        raise ValidationError("Expense date cannot be in the future.")

    ninety_days_ago = today - timedelta(days=90)
    if parsed_date < ninety_days_ago:
        raise ValidationError("Expense date cannot be more than 90 days ago.")

    return parsed_date.strftime("%Y-%m-%d")


def validate_expense_id(id_input) -> int:
    """
    VR-08: Expense ID must be a positive whole number.
    """
    if id_input is None:
        raise ValidationError("Expense ID cannot be empty.")

    id_str = str(id_input).strip()
    if not _is_ascii_digits(id_str):
        raise ValidationError("Expense ID must be a positive whole number.")

    val = int(id_str)
    if val <= 0:
        raise ValidationError("Expense ID must be greater than 0.")

    return val


def validate_menu_choice(choice_input, max_choice=5) -> int:
    """
    VR-09: Menu choice must be a whole number shown in the menu.
    """
    if choice_input is None:
        raise ValidationError("Menu choice cannot be empty.")

    choice_str = str(choice_input).strip()
    if not _is_ascii_digits(choice_str):
        raise ValidationError("Menu choice must be a whole number.")

    val = int(choice_str)
    if not (0 <= val <= max_choice):
        raise ValidationError(f"Menu choice must be between 0 and {max_choice}.")

    return val