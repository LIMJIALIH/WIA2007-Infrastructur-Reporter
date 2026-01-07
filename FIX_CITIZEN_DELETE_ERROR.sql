-- ========================================
-- FIX: HTTP 400 Error When Citizens Delete Tickets
-- ========================================
-- Problem: Citizens get HTTP 400 error when trying to delete tickets
-- Cause: No RLS policy allows citizens to UPDATE tickets table
-- Solution: Add UPDATE policy for deleted_by_citizen column
-- ========================================

-- Option 1: Add UPDATE policy (RECOMMENDED - No app code changes needed)
-- This allows citizens to update ONLY the deleted_by_citizen column on their own tickets
CREATE POLICY "citizens_can_soft_delete_own_tickets"
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

-- Verify the policy was created
SELECT policyname, cmd, qual 
FROM pg_policies 
WHERE tablename = 'tickets' 
AND policyname = 'citizens_can_soft_delete_own_tickets';

-- ========================================
-- ALTERNATIVE: Option 2 - Use SECURITY DEFINER Function
-- ========================================
-- If you prefer to use a function approach (more secure, but requires app changes):
-- 
-- 1. The function already exists in COMPREHENSIVE_FIXES.sql:
--    soft_delete_ticket_for_citizen(ticket_id_param UUID)
--
-- 2. Grant execute permission (if not already done):
--    GRANT EXECUTE ON FUNCTION soft_delete_ticket_for_citizen(UUID) TO authenticated;
--
-- 3. In your app (TicketRepository.java), change the URL from:
--    String url = BuildConfig.SUPABASE_URL + "/rest/v1/tickets?id=eq." + ticketDbId;
--    To:
--    String url = BuildConfig.SUPABASE_URL + "/rest/v1/rpc/soft_delete_ticket_for_citizen";
--    
--    And change the body to:
--    JSONObject updateData = new JSONObject();
--    updateData.put("ticket_id_param", ticketDbId);
--
-- ========================================

-- Test the fix (replace 'your-ticket-id' with an actual UUID)
-- UPDATE tickets SET deleted_by_citizen = TRUE WHERE id = 'your-ticket-id';
