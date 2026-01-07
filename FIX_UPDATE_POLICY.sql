-- ========================================
-- FIX: Replace Old UPDATE Policy
-- ========================================
-- Problem: "Users can update own tickets" policy exists but doesn't work for soft delete
-- Solution: Drop old policy and create new ones with correct permissions
-- ========================================

-- Step 1: Check what the current policy allows
SELECT policyname, cmd, qual, with_check
FROM pg_policies
WHERE tablename = 'tickets' 
AND policyname = 'Users can update own tickets';

-- Step 2: Drop the old restrictive policy
DROP POLICY IF EXISTS "Users can update own tickets" ON tickets;

-- Step 3: Create new UPDATE policies with correct permissions

-- Policy A: Citizens can update deleted_by_citizen on their own tickets
CREATE POLICY "Citizens can soft delete own tickets"
ON tickets
FOR UPDATE
TO authenticated
USING (
  auth.uid() = reporter_id::uuid
  AND EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role = 'citizen'
  )
)
WITH CHECK (
  auth.uid() = reporter_id::uuid
  AND EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role = 'citizen'
  )
);

-- Policy B: Council can update any ticket fields
CREATE POLICY "Council can update tickets"
ON tickets
FOR UPDATE
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role IN ('council', 'admin')
  )
)
WITH CHECK (
  EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role IN ('council', 'admin')
  )
);

-- Policy C: Engineers can update assigned tickets
CREATE POLICY "Engineers can update assigned tickets"
ON tickets
FOR UPDATE
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role = 'engineer'
  )
)
WITH CHECK (
  EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role = 'engineer'
  )
);

-- Step 4: Verify the new policies were created
SELECT policyname, cmd
FROM pg_policies
WHERE tablename = 'tickets'
AND cmd = 'UPDATE'
ORDER BY policyname;

-- Expected: Should see 3 UPDATE policies now

-- ========================================
-- Test the fix (replace UUIDs with real values)
-- ========================================
/*
-- Get your current user ID
SELECT auth.uid() as my_user_id;

-- Find one of your tickets
SELECT id, ticket_id, reporter_id, deleted_by_citizen
FROM tickets
WHERE reporter_id = auth.uid()
LIMIT 1;

-- Try to update it (replace UUID below)
UPDATE tickets 
SET deleted_by_citizen = TRUE 
WHERE id = 'YOUR-TICKET-UUID-HERE';

-- Should succeed now! If it fails, run the diagnostic:
*/

-- ========================================
-- If STILL failing, check these:
-- ========================================

-- 1. Is your user in profiles table?
SELECT id, email, full_name, role 
FROM profiles 
WHERE id = auth.uid();

-- If role is NULL or not 'citizen', fix it:
-- UPDATE profiles SET role = 'citizen' WHERE id = auth.uid();

-- 2. Check if reporter_id and id are both UUID type
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'tickets'
AND column_name IN ('id', 'reporter_id');

-- Both should be 'uuid'

-- 3. Check a specific ticket ownership
/*
SELECT 
    id,
    ticket_id,
    reporter_id,
    (reporter_id::uuid = auth.uid()) as i_own_this
FROM tickets
WHERE id = 'YOUR-TICKET-UUID';
*/
-- 'i_own_this' should be TRUE
