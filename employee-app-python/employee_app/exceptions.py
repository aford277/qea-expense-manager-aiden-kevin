class ExpenseManagerError(Exception):
    """Covers every custom exception"""

class ValidationError(ExpenseManagerError):
    """Raised when input data breaks a validation rule"""

class AuthenticationError(ExpenseManagerError):
    """Raised when login credentials are wrong or the user does not exist"""

class AuthorizationError(ExpenseManagerError):
    """Raised when a logged in user tries an action they are not permitted to perform"""

class NotFoundError(ExpenseManagerError):
    """Raised when a requested record does not exist"""

class InvalidStateError(ExpenseManagerError):
    """Raised when an action is not allowed in the record's current state, such as changing an expense that is no longer pending"""

class DataFileError(ExpenseManagerError):
    """Raised when an import file is missing or malformed"""

class DatabaseError(ExpenseManagerError):
    """Raised when a database operation fails"""