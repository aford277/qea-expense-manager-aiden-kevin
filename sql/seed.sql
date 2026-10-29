-- seed.sql: sample data for the Revature Expense Manager
-- Run right after schema.sql to insert data:  sqlite3 expense_manager.db ".read sql/seed.sql"
-- Dates were picked relative to 2026-10-28.

PRAGMA foreign_keys = ON;

BEGIN TRANSACTION;

-- ---------------------------------------------------------------
-- Users: 2 managers, 4 employees (plain-text passwords, exercise only)
-- ---------------------------------------------------------------
INSERT INTO users (id, username, password, role, full_name) VALUES
    (1, 'mgr_asha',   'asha123',   'manager',  'Asha Patel'),
    (2, 'mgr_daniel', 'daniel123', 'manager',  'Daniel Kim'),
    (3, 'emp_jordan', 'jordan123', 'employee', 'Jordan Lee'),
    (4, 'emp_maria',  'maria123',  'employee', 'Maria Santos'),
    (5, 'emp_wei',    'wei123',    'employee', 'Wei Chen'),
    (6, 'emp_omar',   'omar123',   'employee', 'Omar Haddad');

-- ---------------------------------------------------------------
-- Categories: the 6 from Section 8.2
-- ---------------------------------------------------------------
INSERT INTO categories (id, name) VALUES
    (1, 'Travel'),
    (2, 'Meals'),
    (3, 'Lodging'),
    (4, 'Office Supplies'),
    (5, 'Training'),
    (6, 'Other');

-- ---------------------------------------------------------------
-- Expenses: 25 rows (10 pending, 8 approved, 7 denied)
-- Edge cases are marked with a comment.
-- ---------------------------------------------------------------
INSERT INTO expenses (id, user_id, category_id, amount, description, expense_date, created_at) VALUES
    -- EDGE: minimum amount (0.01)
    (1,  3, 1, 0.01,     'Boundary: minimum amount', '2026-10-01', '2026-10-01 09:15:00'),
    -- EDGE: maximum amount (10000.00)
    (2,  4, 5, 10000.00, 'Boundary: maximum amount - certification bootcamp', '2026-09-15', '2026-09-16 10:00:00'),
    -- EDGE: description exactly 200 characters
    (3,  5, 6, 75.00,    '01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789', '2026-10-10', '2026-10-10 14:22:00'),
    -- EDGE: description exactly 1 character
    (4,  6, 2, 18.75,    'X', '2026-10-20', '2026-10-20 12:05:00'),
    -- EDGE: expense date exactly 90 days before 2026-10-28
    (5,  3, 1, 245.60,   'Boundary: dated exactly 90 days before 2026-10-28', '2026-07-30', '2026-07-30 17:40:00'),
    -- EDGE: expense date = 2026-10-28 (today when written); amount with 1 decimal
    (6,  4, 2, 12.5,     'Coffee with new hire', '2026-10-28', '2026-10-28 08:30:00'),
    (7,  5, 3, 389.00,   'Hotel, 2 nights - Chicago client visit', '2026-09-02', '2026-09-02 19:10:00'),
    (8,  6, 4, 54.99,    'Keyboard and mouse', '2026-08-14', '2026-08-14 11:00:00'),
    -- EDGE: apostrophe in description (escaped as '')
    (9,  3, 2, 86.40,    'Client''s lunch at Bistro 22', '2026-09-21', '2026-09-21 15:45:00'),
    (10, 4, 1, 1200.00,  'Flight to Toronto for team offsite', '2026-08-25', '2026-08-25 09:00:00'),
    (11, 5, 5, 450.00,   'Online Python course', '2026-09-30', '2026-09-30 16:20:00'),
    (12, 6, 1, 62.30,    'Taxi from airport', '2026-10-05', '2026-10-05 22:10:00'),
    (13, 3, 4, 23.15,    'Printer paper and pens', '2026-10-12', '2026-10-12 10:30:00'),
    (14, 4, 3, 975.50,   'Hotel, 5 nights - testing conference', '2026-08-08', '2026-08-09 08:15:00'),
    (15, 5, 2, 31.20,    'Team breakfast', '2026-10-15', '2026-10-15 09:40:00'),
    (16, 6, 5, 1499.99,  'Agile certification exam fee', '2026-09-10', '2026-09-10 13:00:00'),
    (17, 3, 6, 15.00,    'Parking downtown', '2026-10-22', '2026-10-22 18:05:00'),
    (18, 4, 4, 210.00,   'Monitor stand and cables', '2026-09-03', '2026-09-03 14:50:00'),
    (19, 5, 1, 78.45,    'Train ticket to Montreal', '2026-10-25', '2026-10-25 07:55:00'),
    (20, 6, 2, 44.80,    'Working dinner, release night', '2026-09-27', '2026-09-28 09:00:00'),
    (21, 3, 3, 520.00,   'Hotel, 3 nights - training course', '2026-08-18', '2026-08-18 20:30:00'),
    (22, 4, 6, 9.99,     'Software subscription, 1 month', '2026-10-18', '2026-10-18 11:11:00'),
    (23, 5, 4, 129.00,   'Desk lamp', '2026-08-29', '2026-08-29 16:00:00'),
    -- EDGE: just under the maximum
    (24, 6, 3, 9999.99,  'Relocation lodging, 1 month', '2026-10-03', '2026-10-03 12:00:00'),
    (25, 3, 5, 300.00,   'Workshop on test design', '2026-10-26', '2026-10-26 15:30:00');

-- ---------------------------------------------------------------
-- Approvals: exactly one per expense
-- Pending  -> reviewer_id, comment, review_date are all NULL
-- Decided  -> reviewer is a manager (1 or 2) and review_date is set
-- ---------------------------------------------------------------
INSERT INTO approvals (id, expense_id, status, reviewer_id, comment, review_date) VALUES
    (1,  1,  'pending',  NULL, NULL, NULL),
    (2,  2,  'approved', 1,    'Approved - pre-booked with HR', '2026-09-18'),
    (3,  3,  'pending',  NULL, NULL, NULL),
    (4,  4,  'pending',  NULL, NULL, NULL),
    -- EDGE: approved with no comment (NULL), should show "(no comment)"
    (5,  5,  'approved', 2,    NULL, '2026-08-03'),
    (6,  6,  'pending',  NULL, NULL, NULL),
    -- EDGE: short approval comment from VR-06
    (7,  7,  'approved', 1,    'OK', '2026-09-05'),
    -- EDGE: denial comment exactly 5 characters (VR-05 minimum)
    (8,  8,  'denied',   2,    'No PO', '2026-08-20'),
    -- EDGE: approved with an empty-string comment, should also show "(no comment)"
    (9,  9,  'approved', 2,    '', '2026-09-24'),
    (10, 10, 'approved', 1,    'Within travel policy', '2026-08-27'),
    (11, 11, 'denied',   1,    'Not on the approved training list', '2026-10-02'),
    (12, 12, 'pending',  NULL, NULL, NULL),
    (13, 13, 'pending',  NULL, NULL, NULL),
    (14, 14, 'denied',   2,    'Missing receipt', '2026-08-12'),
    (15, 15, 'approved', 2,    'Thanks for organizing', '2026-10-17'),
    -- EDGE: approval comment exactly 200 characters (VR-06 maximum)
    (16, 16, 'approved', 1,    '01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789', '2026-09-14'),
    (17, 17, 'pending',  NULL, NULL, NULL),
    (18, 18, 'denied',   1,    'Personal items are not reimbursable under the current office supplies policy.', '2026-09-06'),
    (19, 19, 'pending',  NULL, NULL, NULL),
    (20, 20, 'denied',   2,    'Alcohol is not reimbursable', '2026-09-30'),
    (21, 21, 'approved', 2,    'Approved per training plan', '2026-08-21'),
    (22, 22, 'denied',   1,    'Use the company license', '2026-10-21'),
    (23, 23, 'denied',   2,    'Already provided by facilities', '2026-09-01'),
    (24, 24, 'pending',  NULL, NULL, NULL),
    (25, 25, 'pending',  NULL, NULL, NULL);

COMMIT;