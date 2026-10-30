"""All user input and output for the Employee App.
 
Only this file and main.py may use input() and print().
No business logic here: read input, call a service, print the result.
"""

import getpass
import logging
import sys
 
from employee_app import services
from employee_app.exceptions import AuthenticationError, AuthorizationError, ExpenseManagerError
 
logger = logging.getLogger(__name__)
 
MAX_FAILED_LOGINS = 3
STATUS_FILTERS = {"1": None, "2": "pending", "3": "approved", "4": "denied"}
 
 
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

def view_expenses(conn, user):
    """Menu option 2 – FR-E2."""
    while True:  # VR-09: re-prompt until a valid choice
        print("\nShow: 1) All  2) Pending  3) Approved  4) Denied")
        choice = input("Choice: ").strip()
        if choice in STATUS_FILTERS:
            break
        print("Please enter a number from the menu.")

    try:
        expenses = services.list_my_expenses(conn, user, STATUS_FILTERS[choice])
    except ExpenseManagerError as exc:
        print(exc)  # FR-S4: short message, no traceback
        return

    if not expenses:
        print("No expenses found.")
        return

    print(f"\n{'ID':>4}  {'Date':<10}  {'Category':<16}  {'Amount':>10}  Status")
    print("-" * 56)
    for e in expenses:
        print(f"{e['id']:>4}  {e['date']:<10}  {e['category']:<16}  "
              f"{e['amount']:>10.2f}  {e['status']}")

def view_review_history(conn, user):
    """Menu option 5 – FR-E5."""
    try:
        history = services.list_review_history(conn, user)
    except ExpenseManagerError as exc:
        print(exc)
        return

    if not history:
        print("No reviewed expenses found.")
        return

    for h in history:
        print(f"\nExpense #{h['id']}  {h['date']}  {h['category']}  ${h['amount']:.2f}")
        print(f"  Status:   {h['status']}")
        print(f"  Reviewer: {h['reviewer']}")
        print(f"  Reviewed: {h['review_date']}")
        print(f"  Comment:  {h['comment']}")
 
 
def main_menu(conn, user):
    """Main menu loop (spec 6.2).
 
    Later: 1 Submit, 2 View, 3 Edit, 4 Delete, 5 Review history.
    """
    while True:
        print("\nPlease choose a service to perform or 0 to exit.")
        print("2. View expense reports")
        print("5. Review expense history")
        print("0. Exit")
        choice = input("Choose: ").strip()
        if choice == "0":
            return
        elif choice == "2":
            view_expenses(conn, user)
        elif choice == "5":
            view_review_history(conn, user)
        else:
            print ("Choose a number from the menu.")


def run(conn):
    """Log the user in, then show the main menu."""
    user = login_prompt(conn)
    print(f"Welcome {user.full_name}")
    main_menu(conn, user)