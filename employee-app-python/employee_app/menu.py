"""All user input and output for the Employee App.
 
Only this file and main.py may use input() and print().
No business logic here: read input, call a service, print the result.
"""

import getpass
import logging
import sys
import textwrap
 
from employee_app import services
from employee_app.exceptions import AuthenticationError, AuthorizationError, ExpenseManagerError, ValidationError
from employee_app.validators import (
    validate_amount,
    validate_description,
    validate_category,
    validate_expense_date,
    validate_expense_id,
    validate_menu_choice,
    validate_login_input
)
 
logger = logging.getLogger(__name__)
 
MAX_FAILED_LOGINS = 3
STATUS_FILTERS = {"1": None, "2": "pending", "3": "approved", "4": "denied"}
 
 
def login_prompt(conn):
    """Ask for username/password until login succeeds. Return the User."""
    failures = 0
 
    while failures < MAX_FAILED_LOGINS:
        username = _ask("Username: ", validate_login_input).strip()
        password = _ask("Password: ", validate_login_input, read=getpass.getpass)
        
        try:
            validate_login_input(username)
            validate_login_input(password)
        except ValidationError as exc:
            print(exc)
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

def _ask(prompt, validate, keep=None, read=None):
    """Ask until validate() accepts the input; print the rule on each failure (FR-E1).
    If keep is given, pressing Enter returns it unchanged (FR-E3)."""
    if read is None:
        read = input
    while True:
        raw = read(prompt)
        if keep is not None and raw.strip() == "":
            return keep
        try:
            return validate(raw)
        except ValidationError as exc:
            print(exc)

def _print_table(headers, rows, right=(), max_widths=None):
    """Print a bordered table with a line between every row (same look as the Manager App).
 
    headers:    list of column titles
    rows:       list of rows; each row is a list of values (None prints as blank)
    right:      column numbers (0-based) to right-align, e.g. IDs and amounts
    max_widths: {column number: max width}; longer text wraps onto extra lines
    """
    max_widths = max_widths or {}
 
    def wrap(col, value):
        text = "" if value is None else str(value)
        width = max_widths.get(col)
        if width:
            return textwrap.wrap(text, width) or [""]
        return [text]
 
    # Each cell becomes a list of lines (more than one only if it wrapped)
    header_cells = [[h] for h in headers]
    body = [[wrap(col, value) for col, value in enumerate(row)] for row in rows]
 
    # Column width = longest line in that column, header included
    widths = []
    for col in range(len(headers)):
        lines = list(header_cells[col])
        for row in body:
            lines.extend(row[col])
        widths.append(max(len(line) for line in lines))
 
    def border(left, middle, end):
        return left + middle.join("─" * (w + 2) for w in widths) + end
 
    def print_row(cells, is_header=False):
        height = max(len(c) for c in cells)
        for i in range(height):
            parts = []
            for col, cell in enumerate(cells):
                line = cell[i] if i < len(cell) else ""
                if is_header:
                    line = line.center(widths[col])
                elif col in right:
                    line = line.rjust(widths[col])
                else:
                    line = line.ljust(widths[col])
                parts.append(f" {line} ")
            print("│" + "│".join(parts) + "│")
 
    print(border("┌", "┬", "┐"))
    print_row(header_cells, is_header=True)
    for row in body:
        print(border("├", "┼", "┤"))
        print_row(row)
    print(border("└", "┴", "┘"))

def _print_categories(categories):
    """Show the numbered category list."""
    print("Categories:")
    for c in categories:
        print(f"  {c['id']}) {c['name']}")

def submit_expense(conn, user):
    """Menu option 1 – FR-E1."""
    try:
        categories = services.list_categories(conn)
    except ExpenseManagerError as exc:
        print(exc)
        return

    amount = _ask("Amount: ", validate_amount)
    _print_categories(categories)
    category_id = _ask("Category number: ", validate_category)
    description = _ask("Description: ", validate_description)
    expense_date = _ask("Date (YYYY-MM-DD, Enter = today): ", validate_expense_date)

    try:
        expense_id = services.submit_expense(
            conn, user, amount, category_id, description, expense_date
        )
    except ExpenseManagerError as exc:
        print(exc)
        return
    print(f"Expense submitted. ID: {expense_id}")

def view_expenses(conn, user):
    """Menu option 2 – FR-E2."""
    while True:  # VR-09: re-prompt until a valid choice
        print("1) All")
        print("2) Pending")
        print("3) Approved")
        print("4) Denied")
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

    print()
    _print_table(
        ["ID", "Date", "Category", "Amount", "Status"],
        [[e["id"], e["date"], e["category"], f"{e['amount']:.2f}", e["status"]]
         for e in expenses],
        right=(0, 3),   # ID and Amount line up on the right
    )
    
def edit_expense(conn, user):
    """Menu option 3 – FR-E3. Enter keeps current value."""
    expense_id = _ask("Expense ID to edit: ", validate_expense_id)
    try:
        current = services.get_pending_expense(conn, user, expense_id)
        categories = services.list_categories(conn)
    except ExpenseManagerError as exc:
        print(exc)  # not found, someone else's, or "Expense is no longer pending."
        return

    print(f"\nEditing expense {expense_id}. Press Enter to keep the current value.")
    amount = _ask(f"Amount [{current['amount']:.2f}]: ", validate_amount,
                  keep=current["amount"])
    _print_categories(categories)
    category_id = _ask(f"Category number [{current['category_id']} - {current['category']}]: ",
                       validate_category, keep=current["category_id"])
    description = _ask(f"Description [{current['description']}]: ", validate_description,
                       keep=current["description"])
    expense_date = _ask(f"Date [{current['date']}]: ", validate_expense_date,
                        keep=current["date"])

    try:
        services.update_expense(
            conn, user, expense_id, amount, category_id, description, expense_date
        )
    except ExpenseManagerError as exc:
        print(exc)  # e.g. a manager decided it while we were editing (XW-03)
        return
    print(f"Expense {expense_id} updated.")

def delete_expense(conn, user):
    """Menu option 4 – FR-E4. Only 'y' or 'Y' confirms."""
    expense_id = _ask("Expense ID to delete: ", validate_expense_id)
    try:
        e = services.get_pending_expense(conn, user, expense_id)
    except ExpenseManagerError as exc:
        print(exc)  # not found, someone else's, or "Expense is no longer pending."
        return

    print()
    _print_table(
        ["ID", "Date", "Category", "Amount", "Description"],
        [[e["id"], e["date"], e["category"], f"{e['amount']:.2f}", e["description"]]],
        right=(0, 3),
        max_widths={4: 40},   # a 200-character description (VR-02) wraps instead of stretching
    )
    if input("Delete this expense? (y/n): ").strip() not in ("y", "Y"):
        print("Delete cancelled.")
        return

    try:
        services.delete_expense(conn, user, expense_id)
    except ExpenseManagerError as exc:
        print(exc)  # manager decided it before we confirmed (XW-04)
        return
    print(f"Expense {expense_id} deleted.")

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

    print()
    _print_table(
        ["ID", "Date", "Category", "Amount", "Status", "Reviewer", "Reviewed", "Comment"],
        [[h["id"], h["date"], h["category"], f"{h['amount']:.2f}", h["status"],
          h["reviewer"], h["review_date"], h["comment"]]
         for h in history],
        right=(0, 3),
        max_widths={7: 30},   # comments can be up to 200 characters (VR-05/VR-06); wrap them
    )
 
 
def main_menu(conn, user):
    """Main menu loop (spec 6.2).
 
    Later: 1 Submit, 2 View, 3 Edit, 4 Delete, 5 Review history.
    """
    while True:
        print("\nPlease choose a service to perform or 0 to exit.")
        print("1) Submit expense")
        print("2) View expense reports")
        print("3) Edit pending expense")
        print("4) Delete pending expense")
        print("5) Review expense history")
        print("0) Exit")
        try:
            choice = validate_menu_choice(input("Choose: "), max_choice=5)
        except ValidationError as exc:
            print(exc)
            continue
        if choice == 0:
            print("Goodbye.")
            return
        elif choice == 1:
            submit_expense(conn, user)
        elif choice == 2:
            view_expenses(conn, user)
        elif choice == 3:
            edit_expense(conn, user)
        elif choice == 4:
            delete_expense(conn, user)
        elif choice == 5:
            view_review_history(conn, user)
        else:
            print("Choose a number from the menu.")


def run(conn):
    """Log the user in, then show the main menu."""
    user = login_prompt(conn)
    print(f"\nWelcome, {user.full_name}")
    main_menu(conn, user)