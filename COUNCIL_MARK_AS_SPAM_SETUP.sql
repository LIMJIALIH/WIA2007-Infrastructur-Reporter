-- ========================================
-- COUNCIL MARK AS SPAM SETUP
-- ========================================
-- Run this in Supabase SQL editor to enable council spam marking
-- When council marks ticket as spam:
--   - Citizens see: status = 'Rejected' with council_notes = 'Marked as Spam by the Council'
--   - Council sees: ticket in Spam tab (is_spam = true)
--   - Engineers: still see the ticket (unless they delete it)

-- Step 1: Add is_spam column if it doesn't exist
ALTER TABLE public.tickets
ADD COLUMN IF NOT EXISTS is_spam BOOLEAN DEFAULT false;

-- Step 2: Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_tickets_is_spam ON public.tickets(is_spam);

-- Step 3: Create ticket_actions table if missing (for audit trail)
CREATE TABLE IF NOT EXISTS public.ticket_actions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ticket_id UUID NOT NULL REFERENCES public.tickets(id) ON DELETE CASCADE,
  created_by UUID,
  action_type TEXT,
  reason TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Step 4: Verify or update status constraint to allow 'Rejected'
-- First check what constraint exists
DO $$
BEGIN
  -- Drop old constraint if exists
  IF EXISTS (
    SELECT 1 FROM pg_constraint 
    WHERE conname = 'tickets_status_check' 
    AND conrelid = 'public.tickets'::regclass
  ) THEN
    ALTER TABLE public.tickets DROP CONSTRAINT tickets_status_check;
  END IF;
  
  -- Add updated constraint with all allowed values
  ALTER TABLE public.tickets 
  ADD CONSTRAINT tickets_status_check 
  CHECK (status IN ('Pending', 'UNDER_REVIEW', 'Accepted', 'Rejected', 'SPAM'));
END $$;

-- Step 5: Backfill any existing spam-marked tickets
-- (only run if you have tickets that need updating)
UPDATE public.tickets
SET is_spam = true
WHERE lower(COALESCE(council_notes, '')) LIKE '%marked as spam by%'
  AND status = 'Rejected';

-- ========================================
-- VERIFICATION QUERIES
-- ========================================
-- Run these to verify setup worked:

-- Check if is_spam column exists
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'tickets' AND column_name = 'is_spam';

-- Check status constraint
SELECT constraint_name, check_clause
FROM information_schema.check_constraints
WHERE constraint_name = 'tickets_status_check';

-- Check ticket_actions table
SELECT table_name FROM information_schema.tables 
WHERE table_name = 'ticket_actions';

-- ========================================
-- TEST QUERY
-- ========================================
-- Test by updating a ticket (replace <ticket-id> with real UUID):
/*
UPDATE public.tickets
SET status = 'Rejected',
    council_notes = 'Marked as Spam by the Council',
    is_spam = true
WHERE id = '<ticket-id>';

-- Then verify:
SELECT id, status, council_notes, is_spam, assigned_engineer_id, assigned_engineer_name
FROM public.tickets
WHERE id = '<ticket-id>';
*/

-- ========================================
-- NOTES
-- ========================================
-- After running this SQL:
-- 1. Rebuild your Android app
-- 2. Test council "Mark as Spam" button
-- 3. Verify in Supabase that ticket shows:
--    - status = 'Rejected'
--    - council_notes = 'Marked as Spam by the Council'
--    - is_spam = true
--    - assigned_engineer_id remains unchanged (so engineer still sees it)
-- 4. In app, verify:
--    - Citizen sees: Status = Rejected, Reason = "Marked as Spam by the Council"
--    - Council All Tickets: ticket appears in Spam(0) tab
--    - Engineers: still see the ticket in their dashboard (can delete if needed)
