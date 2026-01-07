-- ========================================
-- COMPREHENSIVE FIXES FOR ALL ISSUES
-- ========================================
-- This script fixes:
-- 1. Council view: REJECTED tickets shown as COMPLETED (not SPAM)
-- 2. Deleted tickets completely hidden from role views
-- 3. SPAM tickets hidden from engineer's pending view
-- 4. Centralized soft delete approach
-- ========================================

-- Step 1: Ensure soft delete columns exist
ALTER TABLE tickets 
ADD COLUMN IF NOT EXISTS deleted_by_citizen BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS deleted_by_council BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS deleted_by_engineer BOOLEAN DEFAULT FALSE;

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_citizen ON tickets(deleted_by_citizen);
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_council ON tickets(deleted_by_council);
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_engineer ON tickets(deleted_by_engineer);

-- Step 2: Update RLS policies to properly filter tickets

-- Drop all existing SELECT policies
DROP POLICY IF EXISTS "Users can view their own tickets" ON tickets;
DROP POLICY IF EXISTS "Council can view all tickets" ON tickets;
DROP POLICY IF EXISTS "Council can view non-deleted tickets" ON tickets;
DROP POLICY IF EXISTS "Engineers can view assigned tickets" ON tickets;
DROP POLICY IF EXISTS "Citizens can view their own tickets" ON tickets;

-- CITIZEN POLICY: View own tickets that are NOT deleted by citizen
CREATE POLICY "citizens_view_own_tickets"
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

-- COUNCIL POLICY: View all tickets that are NOT deleted by council
-- Includes PENDING, ASSIGNED, IN_PROGRESS, COMPLETED, REJECTED, SPAM
-- (But REJECTED and SPAM are separated in the app logic)
CREATE POLICY "council_view_non_deleted_tickets"
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

-- ENGINEER POLICY: View assigned tickets that are NOT deleted by engineer AND NOT SPAM
-- This prevents SPAM tickets from appearing in engineer's view
CREATE POLICY "engineers_view_assigned_non_spam_tickets"
ON tickets
FOR SELECT
TO authenticated
USING (
  deleted_by_engineer = FALSE
  AND status != 'SPAM'  -- CRITICAL: Hide SPAM tickets from engineers
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

-- Step 3: Soft delete functions (same as before)
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

-- Step 4: SPAM marking function (sets reason to NULL)
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

-- Step 5: Trigger to enforce NULL reason for SPAM status
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

-- Step 6: Grant permissions
GRANT EXECUTE ON FUNCTION mark_ticket_as_spam(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION soft_delete_ticket_for_citizen(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION soft_delete_ticket_for_council(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION soft_delete_ticket_for_engineer(UUID) TO authenticated;

-- ========================================
-- VERIFICATION QUERIES
-- ========================================

-- 1. Check if columns exist
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'tickets'
AND column_name IN ('deleted_by_citizen', 'deleted_by_council', 'deleted_by_engineer');

-- 2. Check policies
SELECT schemaname, tablename, policyname, roles
FROM pg_policies
WHERE tablename = 'tickets';

-- 3. Check functions
SELECT routine_name, routine_type
FROM information_schema.routines
WHERE routine_name IN (
  'mark_ticket_as_spam',
  'soft_delete_ticket_for_citizen',
  'soft_delete_ticket_for_council',
  'soft_delete_ticket_for_engineer',
  'enforce_spam_null_reason'
);

-- 4. Check trigger
SELECT trigger_name, event_manipulation, event_object_table
FROM information_schema.triggers
WHERE trigger_name = 'ensure_spam_null_reason';

-- ========================================
-- TEST SCENARIOS
-- ========================================

-- Test 1: Mark a ticket as SPAM (should set reason to NULL)
-- SELECT mark_ticket_as_spam('your-ticket-id-here');

-- Test 2: Verify SPAM ticket has NULL reason
-- SELECT id, status, reason FROM tickets WHERE status = 'SPAM' LIMIT 5;

-- Test 3: Soft delete as citizen
-- SELECT soft_delete_ticket_for_citizen('your-ticket-id-here');

-- Test 4: Check deleted tickets
-- SELECT id, status, deleted_by_citizen, deleted_by_council, deleted_by_engineer 
-- FROM tickets 
-- WHERE deleted_by_citizen = TRUE OR deleted_by_council = TRUE OR deleted_by_engineer = TRUE;

-- ========================================
-- NOTES
-- ========================================
-- 1. RLS policies now properly filter:
--    - Citizens see only their non-deleted tickets
--    - Council sees all non-deleted tickets (including REJECTED)
--    - Engineers see only assigned, non-deleted, non-SPAM tickets
--
-- 2. Status display in Java:
--    - Council will see REJECTED as "COMPLETED" (handled in Java)
--    - Council will see SPAM as "SPAM" in separate tab
--    - Engineers won't see SPAM tickets at all (filtered by RLS)
--
-- 3. Soft deletes are per-role:
--    - Citizen deletes only affect citizen view
--    - Council deletes only affect council view
--    - Engineer deletes only affect engineer view
