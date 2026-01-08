-- Spam Handling
-- Manages spam tickets with proper status and triggers

-- Trigger function: when engineer marks as SPAM
CREATE OR REPLACE FUNCTION public.handle_engineer_spam()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.status IS NOT NULL AND upper(NEW.status) = 'SPAM' THEN
    -- Set spam flag
    NEW.is_spam := true;
    NEW.status := 'SPAM';
    
    -- Add default note if empty
    IF NEW.engineer_notes IS NULL OR trim(NEW.engineer_notes) = '' THEN
      NEW.engineer_notes := 'Marked As Spam';
    END IF;
    
    -- Log action (ignore errors)
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

-- Trigger to ensure SPAM tickets have NULL reason
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

-- Function to mark ticket as spam (sets reason to NULL)
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

GRANT EXECUTE ON FUNCTION mark_ticket_as_spam(UUID) TO authenticated;

-- Backfill existing spam tickets
UPDATE public.tickets
SET is_spam = true,
    status = 'SPAM',
    engineer_notes = COALESCE(NULLIF(trim(engineer_notes), ''), 'Marked As Spam')
WHERE upper(COALESCE(status,'')) = 'SPAM' 
  OR (engineer_notes IS NOT NULL AND trim(engineer_notes) = 'Marked As Spam');
