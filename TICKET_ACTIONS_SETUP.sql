-- ========================================
-- TICKET ACTIONS TABLE SETUP
-- ========================================
-- Creates the ticket_actions table used to log engineer responses
-- and enable accurate Avg Response calculation (assignment -> first action)

-- 1) Create table if not exists
CREATE TABLE IF NOT EXISTS ticket_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES profiles(id),
    action_type TEXT NOT NULL CHECK (action_type IN ('ACCEPTED','REJECTED','SPAM')),
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2) Indexes for fast lookups
CREATE INDEX IF NOT EXISTS idx_ticket_actions_ticket_id ON ticket_actions(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_actions_action_type ON ticket_actions(action_type);
CREATE INDEX IF NOT EXISTS idx_ticket_actions_created_at ON ticket_actions(created_at);
CREATE INDEX IF NOT EXISTS idx_ticket_actions_ticket_action_created ON ticket_actions(ticket_id, action_type, created_at);

-- 3) Row Level Security and policies (Supabase)
ALTER TABLE ticket_actions ENABLE ROW LEVEL SECURITY;

-- Allow authenticated users to insert actions for tickets assigned to them
CREATE POLICY IF NOT EXISTS insert_ticket_actions_authenticated ON ticket_actions
FOR INSERT TO authenticated
USING (EXISTS (
    SELECT 1 FROM tickets t 
    WHERE t.id = ticket_actions.ticket_id 
      AND (t.assigned_engineer_id = auth.uid())
))
WITH CHECK (EXISTS (
    SELECT 1 FROM tickets t 
    WHERE t.id = ticket_actions.ticket_id 
      AND (t.assigned_engineer_id = auth.uid())
));

-- Allow authenticated users to read actions for their assigned tickets
CREATE POLICY IF NOT EXISTS select_ticket_actions_authenticated ON ticket_actions
FOR SELECT TO authenticated
USING (EXISTS (
    SELECT 1 FROM tickets t 
    WHERE t.id = ticket_actions.ticket_id 
      AND (t.assigned_engineer_id = auth.uid())
));

-- 4) Helper view (optional): earliest_action_per_ticket
CREATE OR REPLACE VIEW earliest_engineer_action AS
SELECT 
    ticket_id,
    MIN(created_at) AS first_action_at
FROM ticket_actions
WHERE action_type IN ('ACCEPTED','REJECTED','SPAM')
GROUP BY ticket_id;

GRANT SELECT, INSERT ON ticket_actions TO authenticated;
GRANT SELECT ON earliest_engineer_action TO authenticated;
