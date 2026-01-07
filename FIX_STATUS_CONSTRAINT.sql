-- ========================================
-- FIX: Allow UNDER_REVIEW Status
-- ========================================
-- This fixes the HTTP 400 error (code 23514) by updating the CHECK constraint

-- Step 1: Drop the existing CHECK constraint on status column
ALTER TABLE tickets DROP CONSTRAINT IF EXISTS tickets_status_check;

-- Step 2: Add new CHECK constraint with UNDER_REVIEW included
ALTER TABLE tickets ADD CONSTRAINT tickets_status_check 
CHECK (status IN ('Pending', 'UNDER_REVIEW', 'Accepted', 'Rejected', 'SPAM'));

-- Step 3: Verify the constraint is working
-- Try to insert a test value (this should work now):
-- UPDATE tickets SET status = 'UNDER_REVIEW' WHERE id = '<some-id>';

-- ========================================
-- ALTERNATIVE: Check what constraint exists
-- ========================================
-- To see your current constraint:
/*
SELECT constraint_name, check_clause
FROM information_schema.check_constraints
WHERE constraint_name LIKE '%status%';
*/

-- ========================================
-- NOTES:
-- ========================================
-- If your status column uses a different format (lowercase, different naming),
-- you may need to adjust the constraint accordingly.
-- Common formats:
--   - 'Pending', 'Accepted', 'Rejected'  (Title Case)
--   - 'pending', 'accepted', 'rejected'  (lowercase)
--   - 'PENDING', 'ACCEPTED', 'REJECTED'  (UPPERCASE)
