import logging
from employee_app.logging_config import setup_logging

logger = logging.getLogger(__name__)


def main():
    setup_logging()
    logger.info("Employee App started")
    # Do something

if __name__ == "__main__":
    main()