-- =====================================================
-- COMPREHENSIVE SPAM HANDLING FOR ENGINEER & COUNCIL
-- =====================================================
-- This trigger ensures proper handling when tickets are marked as SPAM
-- by either engineers or council, with correct status display for each role:
--
-- ENGINEER marks as SPAM:
--   - Engineers see: status = 'SPAM', engineer_notes = 'Marked As Spam'
--   - Council sees: status = 'SPAM', engineer_notes = 'Marked As Spam'
--   - Citizens see: status = 'Rejected', reason = 'Marked As Spam'
--
-- COUNCIL marks as SPAM (via markTicketAsSpam):
--   - Engineers see: status = 'SPAM' (filtered out by RLS)
--   - Council sees: status = 'SPAM' in Spam tab
--   - Citizens see: status = 'Rejected', reason = 'Marked as Spam by the Council'
-- =====================================================

-- Step 1: Ensure required columns exist
ALTER TABLE public.tickets
ADD COLUMN IF NOT EXISTS is_spam BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS engineer_notes TEXT;

-- Step 2: Create ticket_actions table if missing
CREATE TABLE IF NOT EXISTS public.ticket_actions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ticket_id UUID NOT NULL REFERENCES public.tickets(id) ON DELETE CASCADE,
  created_by UUID,
  action_type TEXT,
  reason TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Step 3: Create comprehensive spam handling trigger
-- This trigger runs BEFORE UPDATE and handles:
-- 1. Engineer marking ticket as SPAM (status set to 'SPAM')
-- 2. Council marking ticket as SPAM (status set to 'Rejected', is_spam = true)
CREATE OR REPLACE FUNCTION public.handle_spam_tickets()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  -- Case 1: Engineer marks as SPAM (status = 'SPAM')
  IF NEW.status IS NOT NULL AND upper(NEW.status) = 'SPAM' THEN
    -- Set spam flag
    NEW.is_spam := true;
    
    -- Normalize status to uppercase
    NEW.status := 'SPAM';
    
    -- Ensure engineer_notes contains standard text
    IF NEW.engineer_notes IS NULL OR trim(NEW.engineer_notes) = '' THEN
      NEW.engineer_notes := 'Marked As Spam';
    END IF;
    
    -- Record action in ticket_actions (best-effort)
    BEGIN
      INSERT INTO public.ticket_actions (ticket_id, created_by, action_type, reason)
      VALUES (COALESCE(NEW.id, OLD.id), NEW.assigned_engineer_id, 'SPAM', NEW.engineer_notes);
    EXCEPTION WHEN others THEN
      RAISE NOTICE 'Could not insert ticket_action: %', SQLERRM;
    END;
  END IF;
  
  -- Case 2: Council marks as SPAM (status = 'Rejected', is_spam = true)
  -- This is handled by the markTicketAsSpam() Java method which directly sets:
  -- - status = 'Rejected'
  -- - council_notes = 'Marked as Spam by the Council'  
  -- - is_spam = true
  -- No additional trigger logic needed for this case.
  
  RETURN NEW;
END;
$$;

-- Step 4: Install trigger
DROP TRIGGER IF EXISTS trg_handle_spam_tickets ON public.tickets;
CREATE TRIGGER trg_handle_spam_tickets
BEFORE UPDATE ON public.tickets
FOR EACH ROW
WHEN (OLD.status IS DISTINCT FROM NEW.status OR OLD.is_spam IS DISTINCT FROM NEW.is_spam)
EXECUTE FUNCTION public.handle_spam_tickets();

-- Step 5: Backfill existing spam tickets
-- For tickets marked as SPAM by engineers
UPDATE public.tickets
SET is_spam = true,
    status = 'SPAM',
    engineer_notes = COALESCE(NULLIF(trim(engineer_notes), ''), 'Marked As Spam')
WHERE upper(COALESCE(status,'')) = 'SPAM' 
  OR (engineer_notes IS NOT NULL AND trim(engineer_notes) = 'Marked As Spam');

-- Step 6: Ensure status constraint allows SPAM
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM pg_constraint 
    WHERE conrelid = 'public.tickets'::regclass 
      AND conname = 'tickets_status_check'
  ) THEN
    ALTER TABLE public.tickets DROP CONSTRAINT tickets_status_check;
  END IF;
END $$;

ALTER TABLE public.tickets
ADD CONSTRAINT tickets_status_check
CHECK (status IN ('Pending', 'UNDER_REVIEW', 'Accepted', 'Rejected', 'SPAM'));

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Check SPAM tickets
-- SELECT id, ticket_id, status, is_spam, engineer_notes, council_notes
-- FROM public.tickets
-- WHERE is_spam = true OR upper(status) = 'SPAM';

-- Check recent ticket actions
-- SELECT ta.*, t.ticket_id, t.status
-- FROM public.ticket_actions ta
-- JOIN public.tickets t ON t.id = ta.ticket_id
-- WHERE ta.action_type = 'SPAM'
-- ORDER BY ta.created_at DESC
-- LIMIT 10;

-- =====================================================
-- USAGE NOTES
-- =====================================================
-- 1. Run this script in Supabase SQL Editor
-- 2. After running, when engineer marks ticket as SPAM:
--    - Engineer sees: status = 'SPAM', response = 'Marked As Spam'
--    - Citizen sees: status = 'Rejected', reason = 'Marked As Spam'
-- 3. When council marks ticket as SPAM:
--    - Council sees it in Spam tab
--    - Citizen sees: status = 'Rejected', reason = 'Marked as Spam by the Council'
-- =====================================================
