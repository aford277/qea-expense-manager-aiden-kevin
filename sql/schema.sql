-- schema.sql: schema creation for the Revature Expense Manager
-- Run to create the database for the first time or as a part of resetting the data:  sqlite3 expense_manager.db ".read sql/schema.sql"

DROP TABLE IF EXISTS users;

DROP TABLE IF EXISTS categories;

DROP TABLE IF EXISTS expenses;

DROP TABLE IF EXISTS approvals;

CREATE TABLE users (
    id        INTEGER PRIMARY KEY,
    username  TEXT NOT NULL UNIQUE,
    password  TEXT NOT NULL,
    role      TEXT NOT NULL CHECK (role IN ('employee', 'manager')),
    full_name TEXT NOT NULL
);

CREATE TABLE categories (
    id   INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE CHECK (name IN ('Travel', 'Meals', 'Lodging', 'Office Supplies', 'Training', 'Other'))
);

CREATE TABLE expenses (
    id           INTEGER PRIMARY KEY,
    user_id      INTEGER NOT NULL,
    category_id  INTEGER NOT NULL,
    amount       REAL NOT NULL CHECK (amount > 0 AND amount <= 10000),
    description  TEXT NOT NULL,
    expense_date TEXT NOT NULL CHECK (expense_date IS date(expense_date)),
    created_at   TEXT NOT NULL CHECK (created_at IS datetime(created_at)),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE approvals (
    id INTEGER PRIMARY KEY,
    expense_id INTEGER NOT NULL UNIQUE,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'denied')),
    reviewer_id INTEGER NULL,
    comment TEXT NULL,
    review_date TEXT NULL CHECK (review_date IS date(review_date)),
    FOREIGN KEY (expense_id) REFERENCES expenses(id),
    FOREIGN KEY (reviewer_id) REFERENCES users(id)
);