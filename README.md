# Revature Expense Manager

A console-based expense tracker with two apps that share one SQLite database:

- **Employee App (Python)**: submit and manage personal expenses.
- **Manager App (Java)**: review, approve, or deny expenses and view reports.

---

## Prerequisites

- [SQLite command-line tool](https://www.sqlite.org/download.html) (`sqlite3`)
- Python 3.x
- Java 17+
- Maven

---

## 1. Database setup

The database is a single SQLite file built from the scripts in `sql/`. It is not committed to Git (`*.db` is in `.gitignore`), so each person builds their own copy.

### Create the database

Run these from the repository root. They create `expense_manager.db` there.

```
sqlite3 expense_manager.db ".read sql/schema.sql"
sqlite3 expense_manager.db ".read sql/seed.sql"
```

`schema.sql` creates the tables and constraints. `seed.sql` fills them with sample data (2 managers, 4 employees, 6 categories, 25 expenses).

### Reset the database

Rerun both commands above. `schema.sql` drops and recreates the tables, so this wipes all data and restores the seed.

### Set `EM_DB_PATH`

Both apps find the database through the `EM_DB_PATH` environment variable. Use the **full path** to the file so both apps point to the same database.

**Windows (PowerShell)**

```powershell
$env:EM_DB_PATH = "C:\path\to\expense-manager\expense_manager.db"
```

**macOS / Linux**

```bash
export EM_DB_PATH=/path/to/expense-manager/expense_manager.db
```

This only lasts for the current terminal. Set it again in each new terminal before running an app.

### Check it worked

```
sqlite3 expense_manager.db "SELECT COUNT(*) FROM expenses;"
```

This should print `25`.

> **Note:** foreign keys are off by default in the `sqlite3` command-line tool. When testing constraints by hand, run `PRAGMA foreign_keys = ON;` first.

### Sample logins

| Username | Password | Role |
|----------|----------|------|
| `mgr_asha` | `asha123` | manager |
| `mgr_daniel` | `daniel123` | manager |
| `emp_jordan` | `jordan123` | employee |
| `emp_maria` | `maria123` | employee |
| `emp_wei` | `wei123` | employee |
| `emp_omar` | `omar123` | employee |

---

## 2. Employee App (Python)

### Install dependencies

_TBD_

### Run

_TBD_

### Run tests

_TBD_

---

## 3. Manager App (Java)

### Build

_TBD_

### Run

_TBD_

### Run tests

_TBD_

---