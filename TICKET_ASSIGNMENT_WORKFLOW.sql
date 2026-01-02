-- ========================================
-- TICKET ASSIGNMENT WORKFLOW SETUP
-- ========================================
-- This file ensures the database is properly configured for 
-- the ticket assignment workflow between Council and Engineers

-- ========================================
-- 1. ENSURE REQUIRED COLUMNS EXIST
-- ========================================

-- Check and add assigned_engineer_id column (should already exist from COUNCIL_ENGINEER_ASSIGNMENT_SETUP.sql)
ALTER TABLE tickets 
ADD COLUMN IF NOT EXISTS assigned_engineer_id UUID REFERENCES profiles(id);

-- Check and add assigned_engineer_name column
ALTER TABLE tickets 
ADD COLUMN IF NOT EXISTS assigned_engineer_name TEXT;

-- Add assigned_at timestamp
ALTER TABLE tickets 
ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMPTZ;

-- Add council_notes column for instructions
ALTER TABLE tickets 
ADD COLUMN IF NOT EXISTS council_notes TEXT;

-- ========================================
-- 2. UPDATE STATUS COLUMN TO SUPPORT UNDER_REVIEW
-- ========================================
-- The status column should support these values:
-- 'Pending' - New ticket, not assigned yet
-- 'UNDER_REVIEW' - Assigned to engineer, awaiting engineer action
-- 'Accepted' - Engineer has completed the work
-- 'Rejected' - Engineer rejected the ticket
-- 'SPAM' - Marked as spam

-- If you have a CHECK constraint on status, update it:
-- ALTER TABLE tickets DROP CONSTRAINT IF EXISTS tickets_status_check;
-- ALTER TABLE tickets ADD CONSTRAINT tickets_status_check 
-- CHECK (status IN ('Pending', 'UNDER_REVIEW', 'Accepted', 'Rejected', 'SPAM'));

-- ========================================
-- 3. CREATE INDEXES FOR PERFORMANCE
-- ========================================

-- Index for assigned engineer queries
CREATE INDEX IF NOT EXISTS idx_tickets_assigned_engineer_id 
ON tickets(assigned_engineer_id) 
WHERE assigned_engineer_id IS NOT NULL;

-- Composite index for engineer + status
CREATE INDEX IF NOT EXISTS idx_tickets_engineer_status 
ON tickets(assigned_engineer_id, status) 
WHERE assigned_engineer_id IS NOT NULL;

-- Index for status filtering
CREATE INDEX IF NOT EXISTS idx_tickets_status 
ON tickets(status);

-- ========================================
-- 4. CREATE FUNCTION TO UPDATE ENGINEER STATS
-- ========================================
-- This function recalculates engineer statistics after assignment

CREATE OR REPLACE FUNCTION update_engineer_stats(engineer_user_id UUID)
RETURNS TABLE(
    total_reports BIGINT,
    high_priority BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_reports,
        COUNT(CASE WHEN severity = 'High' AND status = 'UNDER_REVIEW' THEN 1 END) as high_priority
    FROM tickets
    WHERE assigned_engineer_id = engineer_user_id;
END;
$$ LANGUAGE plpgsql;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION update_engineer_stats TO authenticated;

-- ========================================
-- 5. CREATE VIEW FOR COUNCIL DASHBOARD STATS
-- ========================================

CREATE OR REPLACE VIEW council_dashboard_stats AS
SELECT 
    COUNT(*) as total_reports,
    COUNT(CASE WHEN status = 'Pending' THEN 1 END) as total_pending,
    COUNT(CASE WHEN status = 'UNDER_REVIEW' THEN 1 END) as under_review,
    COUNT(CASE WHEN status = 'Accepted' THEN 1 END) as completed,
    COUNT(CASE WHEN status IN ('Rejected', 'SPAM') THEN 1 END) as spam,
    COUNT(CASE WHEN status = 'Pending' AND severity = 'High' THEN 1 END) as high_priority_pending
FROM tickets;

-- Grant access
GRANT SELECT ON council_dashboard_stats TO authenticated;

-- ========================================
-- 6. SAMPLE QUERIES FOR TESTING
-- ========================================

-- Assign a ticket to an engineer
/*
UPDATE tickets 
SET 
    assigned_engineer_id = '<engineer-user-id>',
    assigned_engineer_name = '<engineer-full-name>',
    status = 'UNDER_REVIEW',
    assigned_at = NOW(),
    council_notes = 'Please prioritize this issue'
WHERE id = '<ticket-database-id>';
*/

-- Get all pending tickets (not yet assigned)
/*
SELECT * FROM tickets 
WHERE status = 'Pending' 
ORDER BY created_at DESC;
*/

-- Get all tickets assigned to an engineer
/*
SELECT * FROM tickets 
WHERE assigned_engineer_id = '<engineer-user-id>' 
  AND status = 'UNDER_REVIEW'
ORDER BY created_at ASC;
*/

-- Get council dashboard statistics
/*
SELECT * FROM council_dashboard_stats;
*/

-- Get engineer statistics
/*
SELECT * FROM update_engineer_stats('<engineer-user-id>');
*/

-- Get all engineers with their current workload
/*
SELECT 
    p.id,
    p.full_name,
    p.email,
    COUNT(t.id) FILTER (WHERE t.status = 'UNDER_REVIEW') as pending_review,
    COUNT(t.id) FILTER (WHERE t.status = 'Accepted') as completed,
    COUNT(t.id) FILTER (WHERE t.severity = 'High' AND t.status = 'UNDER_REVIEW') as high_priority
FROM profiles p
LEFT JOIN tickets t ON t.assigned_engineer_id = p.id
WHERE p.role = 'engineer'
GROUP BY p.id, p.full_name, p.email
ORDER BY pending_review DESC;
*/

-- ========================================
-- 7. VERIFY SETUP
-- ========================================

-- Check if all required columns exist
/*
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'tickets' 
  AND column_name IN ('assigned_engineer_id', 'assigned_engineer_name', 'assigned_at', 'council_notes', 'status');
*/

-- Check if indexes exist
/*
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'tickets' 
  AND indexname LIKE '%assigned%';
*/

-- ========================================
-- WORKFLOW EXPLANATION:
-- ========================================
/*
1. COUNCIL DASHBOARD:
   - Shows all tickets
   - "Pending" tab shows tickets with status = 'Pending' (not assigned yet)
   - "Completed" tab shows tickets with status = 'UNDER_REVIEW', 'Accepted'
   - Statistics: Total Pending counts only 'Pending' status

2. ASSIGNMENT PROCESS:
   - Council selects a ticket and assigns to engineer
   - Status changes from 'Pending' to 'UNDER_REVIEW'
   - assigned_engineer_id, assigned_engineer_name, assigned_at are set
   - council_notes stores any instructions

3. ENGINEER DASHBOARD:
   - Fetches tickets WHERE assigned_engineer_id = current_user
   - "Pending Review" tab shows tickets with status = 'UNDER_REVIEW'
   - Engineer can Accept, Reject, or mark as Spam
   - Statistics:
     - New Today: tickets assigned today
     - This Week: tickets assigned this week
     - High Priority: UNDER_REVIEW tickets with severity = 'High'
     - Avg Response: time from assignment to acceptance

4. STATUS FLOW:
   Pending (new) 
     → UNDER_REVIEW (assigned to engineer)
       → Accepted (engineer completed)
       → Rejected (engineer rejected)
       → SPAM (marked as spam)
*/

-- ========================================
-- NOTES:
-- ========================================
-- 1. Make sure to run COUNCIL_ENGINEER_ASSIGNMENT_SETUP.sql first
-- 2. The Android app uses status = 'UNDER_REVIEW' for assigned tickets
-- 3. Statistics are calculated based on status:
--    - Council counts 'Pending' only
--    - Engineer counts 'UNDER_REVIEW' as their pending work
-- 4. Indexes improve query performance for large datasets
-- ========================================
