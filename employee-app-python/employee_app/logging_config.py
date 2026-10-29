import logging
import os

LOG_DIR = "logs"
LOG_FILE = os.path.join(LOG_DIR, "employee_app.log")


def setup_logging():
    """Configure the app-wide logger. Call once from main.py."""
    os.makedirs(LOG_DIR, exist_ok=True)   # the file handler fails if logs/ is missing
    logging.basicConfig(
        filename=LOG_FILE,
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s: %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )