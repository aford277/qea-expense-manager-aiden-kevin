-- 9.3 #3: Pending approvals that have a reviewer or review date. Expected: zero rows.
-- Covers: FR-S2, FR-S5
SELECT a.expense_id, a.reviewer_id, a.review_date
FROM approvals a
WHERE a.status = 'pending'
  AND (a.reviewer_id IS NOT NULL OR a.review_date IS NOT NULL);
