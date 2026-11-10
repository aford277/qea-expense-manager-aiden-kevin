# Revature Expense Manager

A console-based expense tracker with two apps that share one SQLite database:

- **Employee App (Python)**: submit and manage personal expenses.
- **Manager App (Java)**: review, approve, or deny expenses and view reports.

---

## Repository layout

```
expense-manager/
├── sql/                    # schema.sql, seed.sql, validation/ (data-check queries)
├── shared-test-data/       # validation_cases.csv, used by both test suites
├── employee-app-python/    # Employee App - run and test it from this folder
├── manager-app-java/       # Manager App - run and test it from this folder
├── docs/                   # test plan, test cases, RTM, defects, evidence/
└── reports/                # CSV exports from the Manager App
```

Each app is run and tested **from its own folder**. Both apps find the database through `EM_DB_PATH` (below), so they share the same data no matter which folder you start them in.

---

## Prerequisites

- [SQLite command-line tool](https://www.sqlite.org/download.html) (`sqlite3`)
- Python 3.x (the exact version we tested with is noted at the top of `employee-app-python/requirements.txt`)
- Java 17+
- Maven

---

## 1. Database setup

The database is a single SQLite file built from the scripts in `sql/`. It is not committed to Git (`*.db` is in `.gitignore`), so each person builds their own copy.

### Create the database

Run these from the **repository root**. They create `expense_manager.db` there.

```
sqlite3 expense_manager.db ".read sql/schema.sql"
sqlite3 expense_manager.db ".read sql/seed.sql"
```

`schema.sql` creates the tables and constraints. `seed.sql` fills them with sample data (2 managers, 4 employees, 6 categories, 25 expenses).

### Reset the database

Rerun both commands above. `schema.sql` drops and recreates the tables, so this wipes all data and restores the seed.

### Set `EM_DB_PATH`

Both apps find the database through the `EM_DB_PATH` environment variable. Use the **full path** to the file so both apps point to the same database, even though each app runs from its own folder.

**Windows (PowerShell)**

```powershell
$env:EM_DB_PATH = "C:\path\to\expense-manager\expense_manager.db"
```

**Windows (Command Prompt)**

```
set EM_DB_PATH=C:\path\to\expense-manager\expense_manager.db
```

**macOS / Linux**

```bash
export EM_DB_PATH=/path/to/expense-manager/expense_manager.db
```

This only lasts for the current terminal. Set it again in each new terminal before running an app. If it is missing, both apps show a clear message and exit with code 1.

The automated tests do **not** need `EM_DB_PATH`. They build their own temporary database for each test.

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

Employees can only log in to the Employee App and managers only to the Manager App.

---

## 2. Employee App (Python)

Run every command in this section from the `employee-app-python` folder:

```
cd employee-app-python
```

### Install dependencies

Create a virtual environment (first time only):

```
py -m venv venv
```

Activate it (every new terminal):

| Shell | Command |
|-------|---------|
| PowerShell | `.\venv\Scripts\Activate.ps1` |
| Command Prompt | `venv\Scripts\activate` |
| macOS / Linux | `source venv/bin/activate` |

On macOS / Linux, use `python3 -m venv venv` to create it. If PowerShell blocks the activate script, run `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` once.

Your prompt starts with `(venv)` when it is active. Then install the packages:

```
pip install -r requirements.txt
```

The app itself uses only the Python standard library. `requirements.txt` holds the test tools (PyTest and pytest-html).

### Run

Set `EM_DB_PATH` first (Section 1), then:

```
py -m employee_app.main
```

On macOS / Linux, use `python3 -m employee_app.main`.

Logs are written to `logs/employee_app.log`.

### Run tests

Run all tests:

```
pytest
```

Run a subset by marker (`smoke`, `negative`, `boundary`, `integration`):

```
pytest -m smoke
pytest -m "not integration"
```

Produce the HTML test report (saved for the final run in `docs/evidence/`):

```
pytest --html=../docs/evidence/pytest_report.html --self-contained-html
```

---

## 3. Manager App (Java)

Run every command in this section from the `manager-app-java` folder:

```
cd manager-app-java
```

Maven downloads the dependencies listed in `pom.xml` (sqlite-jdbc, JUnit 5, Mockito) the first time you build.

### Build

```
mvn compile
```

### Run

Set `EM_DB_PATH` first (Section 1), then:

```
mvn compile exec:java
```

Logs are written to `logs/manager_app.log`.

### Run tests

Run all tests:

```
mvn test
```

Run a subset by tag (`smoke`, `negative`, `boundary`, `integration`):

```
mvn test -Dgroups=smoke
```

Produce the Surefire HTML test report:

```
mvn surefire-report:report
```

Maven prints where it saved the report under `target/`. Copy it to `docs/evidence/` for the final run.

---

## 4. Reports (CSV export)

In the Manager App, choose `3` Reports, pick a report, then answer `y` to export it. The file is saved as:

```
manager-app-java/reports/<type>_<YYYYMMDD_HHMMSS>.csv
```

where `<type>` is `employee`, `category`, or `employee_range`. Each file has a header row followed by the rows shown on screen.

---

## 5. Shared test data

`shared-test-data/validation_cases.csv` holds the validation cases both apps must agree on: expense IDs (VR-08), menu choices (VR-09), and empty login input (FR-A1). It has the columns `field,input,expected`. Both test suites read this one file directly, so edit it in one place only.