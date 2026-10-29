# employee_app/db.py
import os
import sqlite3
from contextlib import contextmanager


def db_path():
    path = os.environ.get("EM_DB_PATH")
    if not path:
        raise RuntimeError("EM_DB_PATH is not set.")
    return path


@contextmanager
def get_connection(path=None):
    conn = sqlite3.connect(path or db_path(), timeout=5)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

if __name__ == "__main__":
    # Quick manual check that EM_DB_PATH points at a working database.
    # Run with: python -m employee_app.db   (from employee-app-python/)
    path = db_path()
    print(f"EM_DB_PATH = {path}")

    # sqlite3.connect() silently CREATES an empty file if the path is wrong,
    # so check the file exists first.
    if not os.path.isfile(path):
        raise SystemExit("No database file at that path. Run schema.sql and seed.sql first.")

    with get_connection() as conn:
        fk = conn.execute("PRAGMA foreign_keys").fetchone()[0]
        print(f"Foreign keys on: {bool(fk)}")

        for table in ("users", "categories", "expenses", "approvals"):
            count = conn.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
            print(f"{table:<11} {count} rows")