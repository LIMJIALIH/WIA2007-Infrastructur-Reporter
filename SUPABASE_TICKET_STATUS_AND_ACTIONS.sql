-- ========================================
-- TICKET STATUS CONSTRAINT + ACTION LOGGING
-- ========================================
-- Ensures status supports UNDER_REVIEW and logs engineer actions
-- Run in Supabase SQL editor as a privileged user
-- ========================================

-- 1) Normalize and widen status constraint to include UNDER_REVIEW
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM pg_constraint 
    WHERE conrelid = 'public.tickets'::regclass 
      AND conname = 'tickets_status_check'
  ) THEN
    ALTER TABLE tickets DROP CONSTRAINT tickets_status_check;
  END IF;
END $$;

ALTER TABLE tickets
ADD CONSTRAINT tickets_status_check
CHECK (lower(status) IN ('pending','under_review','accepted','rejected','spam'));

-- Optional default (keeps new tickets pending)
ALTER TABLE tickets ALTER COLUMN status SET DEFAULT 'pending';

-- 2) Action logging table (engineer responses)
CREATE TABLE IF NOT EXISTS ticket_actions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
  action_type TEXT NOT NULL CHECK (lower(action_type) IN ('accepted','rejected','spam')),
  reason TEXT,
  created_by UUID NOT NULL REFERENCES auth.users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ticket_actions_ticket ON ticket_actions(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_actions_created_at ON ticket_actions(created_at);

-- 3) Minimal RLS policies (adjust to your needs)
ALTER TABLE ticket_actions ENABLE ROW LEVEL SECURITY;

-- Engineers and council can insert/select actions on tickets they can update/select
CREATE POLICY IF NOT EXISTS "ticket_actions_select"
ON ticket_actions FOR SELECT TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM tickets t
    WHERE t.id = ticket_actions.ticket_id
  )
);

CREATE POLICY IF NOT EXISTS "ticket_actions_insert"
ON ticket_actions FOR INSERT TO authenticated
WITH CHECK (
  EXISTS (
    SELECT 1 FROM profiles p
    WHERE p.id = auth.uid() AND p.role IN ('engineer','council','admin')
  )
);

-- 4) Helper view: first engineer action timestamp per ticket (for Avg Response)
CREATE OR REPLACE VIEW earliest_engineer_action AS
SELECT ticket_id, MIN(created_at) AS first_action_at
FROM ticket_actions
GROUP BY ticket_id;

-- ========================================
-- NOTES
-- - App assigns status to UNDER_REVIEW when council assigns to engineer
-- - Engineer actions (Accept/Reject/Spam) insert into ticket_actions and update tickets.status
-- - Avg Response is computed from tickets.assigned_at -> earliest_engineer_action.first_action_at
-- ========================================
