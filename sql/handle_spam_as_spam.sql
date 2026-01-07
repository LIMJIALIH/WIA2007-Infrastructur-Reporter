-- Ensure column exists
ALTER TABLE public.tickets
ADD COLUMN IF NOT EXISTS is_spam BOOLEAN DEFAULT false;

-- Create ticket_actions table if missing
CREATE TABLE IF NOT EXISTS public.ticket_actions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ticket_id UUID NOT NULL REFERENCES public.tickets(id) ON DELETE CASCADE,
  created_by UUID,
  action_type TEXT,
  reason TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Trigger function: when engineer marks SPAM, keep status = 'SPAM', set is_spam, set engineer_notes default
CREATE OR REPLACE FUNCTION public.handle_engineer_spam()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.status IS NOT NULL AND lower(NEW.status) = 'spam' THEN
    -- mark spam flag
    NEW.is_spam := true;

    -- keep public status as 'SPAM' so UI shows it in Spam tab
    NEW.status := 'SPAM';

    -- ensure engineer_notes contains standard text if empty
    IF NEW.engineer_notes IS NULL OR trim(NEW.engineer_notes) = '' THEN
      NEW.engineer_notes := 'Marked As Spam';
    END IF;

    -- record action in ticket_actions (best-effort)
    BEGIN
      INSERT INTO public.ticket_actions (ticket_id, created_by, action_type, reason)
      VALUES (COALESCE(NEW.id, OLD.id), NEW.assigned_engineer_id, 'SPAM', NEW.engineer_notes);
    EXCEPTION WHEN others THEN
      RAISE NOTICE 'Could not insert ticket_action: %', SQLERRM;
    END;
  END IF;

  RETURN NEW;
END;
$$;

-- Install trigger
DROP TRIGGER IF EXISTS trg_handle_engineer_spam ON public.tickets;
CREATE TRIGGER trg_handle_engineer_spam
BEFORE UPDATE ON public.tickets
FOR EACH ROW
WHEN (OLD.status IS DISTINCT FROM NEW.status)
EXECUTE FUNCTION public.handle_engineer_spam();

-- Backfill existing spam rows: set is_spam=true and status='SPAM' and engineer_notes default if empty
UPDATE public.tickets
SET is_spam = true,
    status = 'SPAM',
    engineer_notes = COALESCE(NULLIF(trim(engineer_notes), ''), 'Marked As Spam')
WHERE lower(COALESCE(status,'')) = 'spam' OR (engineer_notes IS NOT NULL AND trim(engineer_notes) = 'Marked As Spam');

-- NOTE:
-- Run this script in Supabase SQL editor. After running, your app should see tickets with status='SPAM' appear in the Spam tab.
-- If your client filters by is_spam, the above also sets the boolean flag.
