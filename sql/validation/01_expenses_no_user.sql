-- 9.3 #1: Expenses with no matching user. Expected: zero rows.
-- Covers: FR-S1, FR-S5
SELECT e.id 
FROM expenses e 
LEFT JOIN users u ON e.user_id = u.id 
WHERE u.id IS NULL;
