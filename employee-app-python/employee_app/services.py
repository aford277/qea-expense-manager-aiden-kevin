import logging
from dataclasses import dataclass
from datetime import date, datetime

from employee_app.exceptions import (
    AuthenticationError,
    AuthorizationError
)

logger = logging.getLogger(__name__)

@dataclass
class User:
    """The logged in user"""
    id: int
    username: str
    role: str
    full_name: str

def login(conn, username, password):
    """Check credentials and role. Return a User on success.
 
    Raises:
        AuthenticationError: "Invalid username or password."  (FR-A1)
            Same message for an unknown username and a wrong password.
        AuthorizationError: "This account does not have access to this app."
            when the role is not 'employee'.  (FR-A2)
 
    Note: empty-input re-prompting and the 3-attempt counter (FR-A3)
    belong in menu.py, not here.
    """

    # Get user info
    row = conn.execute(
        "SELECT id, username, password, role, full_name FROM users WHERE username = ?",
        (username,),
    ).fetchone()

    # Check if username is not found or incorrect password
    if row is None:
        logger.warning("User entered %s as username but they do not exist!", username)
        raise AuthenticationError("Username not found!")
    elif row["password"] != password:
        logger.warning("%s user entered wrong password.", username)
        raise AuthenticationError("Incorrect password.")

    # Check is user is actually a manager
    if row["role"] != "employee":
        logger.warning("%s is on the wrong app! They are not an employee.", username)
        raise AuthorizationError("You are using the wrong app. This app is for employees only.")

    # Log in succeeded
    logger.info("Login succeeded for %s", username)
    return User(
        id=row["id"],
        username=row["username"],
        role=row["role"],
        full_name=row["full_name"]
    )
