"""Entry point for the Employee App.

Run from route
Run from employee-app-python/:   python -m employee_app.main
 
Job of this file:
  * set up logging
  * open the database connection and hand it to the menu
  * catch anything that escapes and turn it into a friendly message + exit code
"""

import logging
import sqlite3
import sys

from employee_app import menu
from employee_app.db import get_connection
from employee_app.exceptions import DatabaseError
from employee_app.logging_config import setup_logging

logger = logging.getLogger(__name__)


def main():
    setup_logging()
    logger.info("Employee App started")

    try:
        with get_connection() as conn:
            menu.run(conn)
    except RuntimeError as e:
        # db_path() raises this when EM_DB_PATH is not set
        logger.error("EM_DM_PATH is not set! See README.")
        print("EM_DM_PATH is not set! See README.")
        sys.exit(1)
    except (DatabaseError, sqlite3.Error):
        logger.exception("There was a problem starting accessing the database!")
        print("Could not access the database.")
        sys.exit(1)
    except Exception:
        logger.exception("Unexpected error!")
        print("Unexpected error. See log.")
        sys.exit(1)
    

if __name__ == "__main__":
    main()