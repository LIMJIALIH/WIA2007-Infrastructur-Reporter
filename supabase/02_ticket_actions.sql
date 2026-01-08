-- Ticket Actions Tracking
-- Logs engineer actions (accept/reject/spam) for audit trail and statistics

-- Create ticket actions table
CREATE TABLE IF NOT EXISTS ticket_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES profiles(id),
    action_type TEXT NOT NULL CHECK (action_type IN ('ACCEPTED','REJECTED','SPAM')),
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add indexes for quick lookups
CREATE INDEX IF NOT EXISTS idx_ticket_actions_ticket_id ON ticket_actions(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_actions_action_type ON ticket_actions(action_type);
CREATE INDEX IF NOT EXISTS idx_ticket_actions_created_at ON ticket_actions(created_at);

-- Enable row-level security
ALTER TABLE ticket_actions ENABLE ROW LEVEL SECURITY;

-- Engineers can insert actions for tickets assigned to them
CREATE POLICY "ticket_actions_insert"
ON ticket_actions FOR INSERT TO authenticated
WITH CHECK (
    created_by = auth.uid() AND
    EXISTS (
        SELECT 1 FROM profiles
        WHERE profiles.id = auth.uid() 
        AND profiles.role = 'engineer'
    )
);

-- Engineers and council can view all ticket actions
CREATE POLICY "ticket_actions_select"
ON ticket_actions FOR SELECT TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM profiles
        WHERE profiles.id = auth.uid() 
        AND profiles.role IN ('engineer', 'council', 'admin')
    )
);

-- View for finding first action timestamp per ticket (used for avg response time)
CREATE OR REPLACE VIEW earliest_engineer_action AS
SELECT ticket_id, MIN(created_at) AS first_action_at
FROM ticket_actions
GROUP BY ticket_id;

GRANT SELECT, INSERT ON ticket_actions TO authenticated;
GRANT SELECT ON earliest_engineer_action TO authenticated;
