-- 9.3 #2: Expenses with no approval row. Expected: zero rows.
-- Covers: FR-E1, FR-E4, FR-S5
SELECT e.id 
FROM expenses e 
LEFT JOIN approvals a ON e.id = a.expense_id 
WHERE a.expense_id IS NULL;
