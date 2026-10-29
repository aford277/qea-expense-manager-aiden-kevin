"""All user input and output for the Employee App.
 
Only this file and main.py may use input() and print().
No business logic here: read input, call a service, print the result.
"""

import getpass
import logging
import sys
 
from employee_app import services
from employee_app.exceptions import AuthenticationError, AuthorizationError
 
logger = logging.getLogger(__name__)
 
MAX_FAILED_LOGINS = 3
 
 
def run(conn):
    """Log the user in, then show the main menu."""
    user = login_prompt(conn)
    print(f"Welcome {user.full_name}")
    main_menu(conn, user)
 
 
def login_prompt(conn):
    """Ask for username/password until login succeeds. Return the User."""
    failures = 0
 
    while failures < MAX_FAILED_LOGINS:
        username = input("Username: ").strip()
        password = getpass.getpass("Password: ")
        
        if not username or not password:
            # re-prompt, failures unchanged
            print("Please enter a username and password.")
            continue
 
        try:
            test = services.login(conn, username, password)
            return test
 
        except AuthenticationError as e:
            # Invalid username or password
            print(e)
            failures += 1
 
        except AuthorizationError as e:
            # Manager tries to log into wrong app
            print(e)
            sys.exit(1)


    # 3 failed attempts
    logger.warning("Login locked out after 3 failed attempts.")
    print("Too many failed attempts.")
    sys.exit(1)
 
 
def main_menu(conn, user):
    """Main menu loop (spec 6.2). Stub: only 0 = Exit for now.
 
    Later: 1 Submit, 2 View, 3 Edit, 4 Delete, 5 Review history.
    """
    while True:
        print("\n Please choose a service to perform or 0 to exit.")
        print("0. Exit")
        choice = input("Choose: ").strip()
        if choice == "0":
            return
        else:
            print ("Choose a number from the menu.")