-- ========================================
-- SPAM STATUS AND SOFT DELETE FIXES
-- ========================================
-- This script adds:
-- 1. Proper SPAM status handling (reason = null for SPAM)
-- 2. Soft delete columns for citizens, council, and engineers
-- 3. Updated RLS policies to hide soft-deleted tickets
-- ========================================

-- Step 1: Add soft delete columns to tickets table
ALTER TABLE tickets 
ADD COLUMN IF NOT EXISTS deleted_by_citizen BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS deleted_by_council BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS deleted_by_engineer BOOLEAN DEFAULT FALSE;

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_citizen ON tickets(deleted_by_citizen);
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_council ON tickets(deleted_by_council);
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_engineer ON tickets(deleted_by_engineer);

-- Step 2: Add function to handle SPAM marking with null reason
CREATE OR REPLACE FUNCTION mark_ticket_as_spam(ticket_id_param UUID)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  UPDATE tickets
  SET 
    status = 'SPAM',
    reason = NULL,
    updated_at = NOW()
  WHERE id = ticket_id_param;
END;
$$;

-- Step 3: Update RLS policies to respect soft deletes

-- Drop existing SELECT policies
DROP POLICY IF EXISTS "Users can view their own tickets" ON tickets;
DROP POLICY IF EXISTS "Council can view all tickets" ON tickets;
DROP POLICY IF EXISTS "Engineers can view assigned tickets" ON tickets;

-- Policy 1: Citizens can view their own non-deleted tickets
CREATE POLICY "Citizens can view their own tickets"
ON tickets
FOR SELECT
TO authenticated
USING (
  auth.uid() = reporter_id::uuid
  AND deleted_by_citizen = FALSE
  AND EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role = 'citizen'
  )
);

-- Policy 2: Council can view all non-deleted tickets (from their perspective)
CREATE POLICY "Council can view non-deleted tickets"
ON tickets
FOR SELECT
TO authenticated
USING (
  deleted_by_council = FALSE
  AND EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role IN ('council', 'admin')
  )
);

-- Policy 3: Engineers can view assigned non-deleted tickets
CREATE POLICY "Engineers can view assigned tickets"
ON tickets
FOR SELECT
TO authenticated
USING (
  deleted_by_engineer = FALSE
  AND EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role = 'engineer'
    AND (
      tickets.assigned_engineer_id::uuid = auth.uid()
      OR tickets.status = 'UNDER_REVIEW'
    )
  )
);

-- Step 4: ADD UPDATE POLICIES (CRITICAL FIX FOR HTTP 400 ERROR)
-- These policies allow direct UPDATE of deleted_by_* columns

-- Policy 4: Citizens can update deleted_by_citizen on their own tickets
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

-- Policy 5: Council can update deleted_by_council on any ticket
CREATE POLICY "Council can soft delete tickets"
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

-- Policy 6: Engineers can update deleted_by_engineer on assigned tickets
CREATE POLICY "Engineers can soft delete assigned tickets"
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

-- Step 5: Create function to soft delete for each role (OPTIONAL - for future use)
CREATE OR REPLACE FUNCTION soft_delete_ticket_for_citizen(ticket_id_param UUID)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  UPDATE tickets
  SET 
    deleted_by_citizen = TRUE,
    updated_at = NOW()
  WHERE id = ticket_id_param
  AND reporter_id::uuid = auth.uid();
END;
$$;

CREATE OR REPLACE FUNCTION soft_delete_ticket_for_council(ticket_id_param UUID)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  UPDATE tickets
  SET 
    deleted_by_council = TRUE,
    updated_at = NOW()
  WHERE id = ticket_id_param
  AND EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role IN ('council', 'admin')
  );
END;
$$;

-- Step 6: Create function to soft delete for each role (OPTIONAL - for future use)
CREATE OR REPLACE FUNCTION soft_delete_ticket_for_engineer(ticket_id_param UUID)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  UPDATE tickets
  SET 
    deleted_by_engineer = TRUE,
    updated_at = NOW()
  WHERE id = ticket_id_param
  AND EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role = 'engineer'
  );
END;
$$;

-- Step 7: Create trigger to auto-set reason to NULL when status is SPAM
CREATE OR REPLACE FUNCTION enforce_spam_null_reason()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.status = 'SPAM' THEN
    NEW.reason = NULL;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS ensure_spam_null_reason ON tickets;
CREATE TRIGGER ensure_spam_null_reason
BEFORE INSERT OR UPDATE OF status ON tickets
FOR EACH ROW
EXECUTE FUNCTION enforce_spam_null_reason();

-- Step 8: Grant necessary permissions
GRANT EXECUTE ON FUNCTION mark_ticket_as_spam(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION soft_delete_ticket_for_citizen(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION soft_delete_ticket_for_council(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION soft_delete_ticket_for_engineer(UUID) TO authenticated;

-- ========================================
-- VERIFICATION QUERIES
-- ========================================
-- Run these to verify the setup worked correctly

-- Check if columns were added
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'tickets'
AND column_name IN ('deleted_by_citizen', 'deleted_by_council', 'deleted_by_engineer');

-- Check if functions exist
SELECT routine_name
FROM information_schema.routines
WHERE routine_name IN (
  'mark_ticket_as_spam',
  'soft_delete_ticket_for_citizen',
  'soft_delete_ticket_for_council',
  'soft_delete_ticket_for_engineer'
);

-- Check if trigger exists
SELECT trigger_name
FROM information_schema.triggers
WHERE trigger_name = 'ensure_spam_null_reason';

-- Check UPDATE policies (CRITICAL - These must exist!)
SELECT policyname, cmd
FROM pg_policies
WHERE tablename = 'tickets'
AND cmd = 'UPDATE';

-- ========================================
-- TROUBLESHOOTING HTTP 400 ERRORS
-- ========================================
-- If you still get HTTP 400 when deleting tickets:

-- 1. Verify RLS is enabled on tickets table
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE tablename = 'tickets';
-- Expected: rowsecurity = true

-- 2. Test if you can update as a citizen (replace UUIDs with real values)
-- Make sure you're authenticated as a citizen user first
/*
UPDATE tickets 
SET deleted_by_citizen = TRUE 
WHERE id = 'your-ticket-uuid' 
AND reporter_id = 'your-user-uuid';
*/

-- 3. Check what error Supabase returns:
-- In Android Studio, check Logcat for detailed error message from SupabaseManager

-- 4. Common causes of HTTP 400:
--    - Column doesn't exist (run Step 1 of this script)
--    - No UPDATE policy (run Step 4 of this script)
--    - User not authenticated (check access token)
--    - User role is not 'citizen' in profiles table
--    - ticket_id format is wrong (should be UUID not ticket_id string)

