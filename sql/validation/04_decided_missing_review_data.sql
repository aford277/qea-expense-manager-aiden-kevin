-- 9.3 #4: Approved/denied approvals missing a reviewer or review date,
-- or reviewed by a non-manager (or a reviewer id that matches no user).
-- Expected: zero rows.
-- Covers: FR-M2, FR-S2, FR-S5, FR-A4
SELECT a.expense_id, a.status, a.reviewer_id, a.review_date, u.role AS reviewer_role
FROM approvals a
LEFT JOIN users u ON u.id = a.reviewer_id
WHERE a.status IN ('approved', 'denied')
  AND (a.reviewer_id IS NULL
       OR a.review_date IS NULL
       OR u.role IS NOT 'manager');
