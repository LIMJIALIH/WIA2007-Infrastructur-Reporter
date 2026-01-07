-- Add soft delete columns and engineer reason support to tickets table
-- Run this in your Supabase SQL Editor
-- NOTE: The app now works WITHOUT these columns (backward compatible)
-- But running this SQL will enable proper soft delete and performance improvements

-- 1. Add soft delete columns for citizen and council views
ALTER TABLE tickets
ADD COLUMN IF NOT EXISTS deleted_by_citizen BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS deleted_by_council BOOLEAN DEFAULT FALSE;

-- 2. Ensure engineer_notes column exists (for engineer's reason when accepting/rejecting)
-- This should already exist from TICKET_ACTIONS_SETUP.sql, but adding it just in case
ALTER TABLE tickets
ADD COLUMN IF NOT EXISTS engineer_notes TEXT;

-- 3. Create index for faster filtering on soft-deleted tickets
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_citizen ON tickets(deleted_by_citizen) WHERE deleted_by_citizen = FALSE;
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_council ON tickets(deleted_by_council) WHERE deleted_by_council = FALSE;

-- 4. Add comment for documentation
COMMENT ON COLUMN tickets.deleted_by_citizen IS 'Soft delete flag - when TRUE, ticket is hidden from citizen view only';
COMMENT ON COLUMN tickets.deleted_by_council IS 'Soft delete flag - when TRUE, ticket is hidden from council view only';
COMMENT ON COLUMN tickets.engineer_notes IS 'Engineer provided reason when accepting/rejecting ticket';

-- 5. Create or replace function to calculate council average response time
-- This calculates time from ticket creation to first assignment
CREATE OR REPLACE FUNCTION get_council_avg_response_time()
RETURNS TEXT AS $$
DECLARE
    avg_hours NUMERIC;
BEGIN
    SELECT AVG(EXTRACT(EPOCH FROM (assigned_at - created_at)) / 3600)
    INTO avg_hours
    FROM tickets
    WHERE assigned_at IS NOT NULL
      AND created_at IS NOT NULL
      AND assigned_at > created_at;
    
    IF avg_hours IS NULL THEN
        RETURN 'N/A';
    ELSIF avg_hours < 1 THEN
        RETURN '< 1 hr';
    ELSE
        RETURN ROUND(avg_hours, 1) || ' hrs';
    END IF;
END;
$$ LANGUAGE plpgsql;

-- 6. Create or replace function to calculate engineer average response time
-- This calculates time from assignment to first action (accepted/rejected/spam)
CREATE OR REPLACE FUNCTION get_engineer_avg_response_time(engineer_id_param UUID)
RETURNS TEXT AS $$
DECLARE
    avg_hours NUMERIC;
BEGIN
    SELECT AVG(EXTRACT(EPOCH FROM (ta.created_at - t.assigned_at)) / 3600)
    INTO avg_hours
    FROM tickets t
    INNER JOIN ticket_actions ta ON t.id = ta.ticket_id
    WHERE t.assigned_engineer_id = engineer_id_param
      AND t.assigned_at IS NOT NULL
      AND ta.action_type IN ('ACCEPTED', 'REJECTED', 'SPAM')
      AND ta.created_at > t.assigned_at
      -- Only count the first action per ticket
      AND ta.id = (
          SELECT ta2.id
          FROM ticket_actions ta2
          WHERE ta2.ticket_id = t.id
            AND ta2.action_type IN ('ACCEPTED', 'REJECTED', 'SPAM')
          ORDER BY ta2.created_at ASC
          LIMIT 1
      );
    
    IF avg_hours IS NULL THEN
        RETURN 'N/A';
    ELSIF avg_hours < 1 THEN
        RETURN '< 1 hr';
    ELSE
        RETURN ROUND(avg_hours, 1) || ' hrs';
    END IF;
END;
$$ LANGUAGE plpgsql;

-- 7. Usage examples:
-- Get council avg response time:
-- SELECT get_council_avg_response_time();

-- Get engineer avg response time for a specific engineer:
-- SELECT get_engineer_avg_response_time('engineer-uuid-here');

-- Query tickets visible to citizen (not deleted by citizen):
-- SELECT * FROM tickets 
-- WHERE reporter_id = 'user-id' 
--   AND deleted_by_citizen = FALSE
-- ORDER BY created_at DESC;

-- Query tickets visible to council (not deleted by council):
-- SELECT * FROM tickets 
-- WHERE deleted_by_council = FALSE
-- ORDER BY created_at DESC;

COMMENT ON FUNCTION get_council_avg_response_time IS 'Calculates average time from ticket creation to assignment by council';
COMMENT ON FUNCTION get_engineer_avg_response_time IS 'Calculates average time from assignment to first action by engineer';
