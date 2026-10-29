import re
from datetime import datetime, date, timedelta
from employee_app.exceptions import ValidationError

def validate_amount(amount_input) -> float:
    """
    VR-01: Amount must be greater than 0, at most 10,000.00, at most 2 decimals,
    digits and one decimal point only.
    """
    if amount_input is None:
        raise ValidationError("Amount can't be empty")

    amt_str = str(amount_input).strip()

    # Regex makes sure digits, optional single decimal point, and 1-2 decimal digits
    if not re.match(r"^\d+(\.\d{1,2})?$", amt_str):
        raise ValidationError("Invalid amount format. Must be a positive number with at most 2 decimals.")
    
    try:
        val = float(amt_str)
    except ValueError:
        raise ValidationError("Invalid amount format.")

    if val <= 0:
        raise ValidationError("Amount must be greater than 0.")
    if val > 10000.00:
        raise ValidationError("Amount can't exceed $10,000.00.")

    return round(val,2)

def is_valid_amount(amount_input) -> bool:
    """Boolean helper for VR-01 testing."""
    try:
        validate_amount(amount_input)
        return True
    except ValidationError:
        return False

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

def is_valid_description(desc_input) -> bool:
    '''Boolean helper for VR-02 testing.'''
    try:
        validate_description(desc_input)
        return True
    except ValidationError:
        return False
