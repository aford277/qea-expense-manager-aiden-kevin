-- 9.3 #5: Approvals whose status or review_date breaks the shared data contract (8.3).
--   status must be exactly 'pending', 'approved' or 'denied' (lowercase).
--   review_date, when present, must be a real date in exact YYYY-MM-DD form.
--   date(x) = x rejects wrong formats (NULL), impossible dates such as 2026-02-30
--   (SQLite normalises them to another day) and timestamps with a time part.
-- Expected: zero rows.
-- Covers: FR-S5
SELECT a.expense_id, a.status, a.review_date
FROM approvals a
WHERE a.status NOT IN ('pending', 'approved', 'denied')
   OR (a.review_date IS NOT NULL
       AND (length(a.review_date) <> 10
            OR date(a.review_date) IS NULL
            OR date(a.review_date) <> a.review_date));
