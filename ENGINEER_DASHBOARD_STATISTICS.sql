-- ========================================
-- ENGINEER DASHBOARD STATISTICS SETUP
-- ========================================
-- This file contains SQL for optimizing engineer dashboard statistics
-- Run these in your Supabase SQL Editor to create views and functions
-- for efficient statistics calculation

-- ========================================
-- 1. CREATE VIEW FOR ENGINEER STATISTICS
-- ========================================
-- This view provides a quick summary of ticket statistics per engineer
CREATE OR REPLACE VIEW engineer_ticket_stats AS
SELECT 
    p.id as engineer_id,
    p.full_name as engineer_name,
    p.email as engineer_email,
    COUNT(t.id) as total_tickets,
    COUNT(CASE WHEN t.status = 'Pending' THEN 1 END) as pending_tickets,
    COUNT(CASE WHEN t.status = 'Accepted' THEN 1 END) as accepted_tickets,
    COUNT(CASE WHEN t.status = 'Rejected' THEN 1 END) as rejected_tickets,
    COUNT(CASE WHEN t.status = 'SPAM' THEN 1 END) as spam_tickets,
    COUNT(CASE WHEN t.severity = 'High' AND t.status = 'Pending' THEN 1 END) as high_priority_pending,
    COUNT(CASE WHEN DATE(t.created_at) = CURRENT_DATE THEN 1 END) as new_today,
    COUNT(CASE WHEN t.created_at >= CURRENT_DATE - INTERVAL '7 days' THEN 1 END) as this_week
FROM profiles p
LEFT JOIN tickets t ON t.assigned_engineer_id = p.id
WHERE p.role = 'engineer'
GROUP BY p.id, p.full_name, p.email;

-- Grant access to authenticated users
GRANT SELECT ON engineer_ticket_stats TO authenticated;

-- ========================================
-- 2. CREATE FUNCTION FOR AVERAGE RESPONSE TIME
-- ========================================
-- This function calculates the average response time for an engineer
-- Response time = time from ticket creation to first ACCEPTED action
CREATE OR REPLACE FUNCTION get_engineer_avg_response_time(engineer_user_id UUID)
RETURNS TEXT AS $$
DECLARE
    avg_hours NUMERIC;
    avg_minutes NUMERIC;
    response_count INT;
BEGIN
    -- Calculate average response time in hours
    SELECT 
        COUNT(*),
        AVG(EXTRACT(EPOCH FROM (ta.created_at - t.created_at)) / 3600)
    INTO response_count, avg_hours
    FROM tickets t
    INNER JOIN ticket_actions ta ON ta.ticket_id = t.id
    WHERE t.assigned_engineer_id = engineer_user_id
      AND ta.action_type = 'ACCEPTED'
      AND ta.created_at = (
          SELECT MIN(created_at) 
          FROM ticket_actions 
          WHERE ticket_id = t.id AND action_type = 'ACCEPTED'
      );
    
    -- If no accepted tickets, return default
    IF response_count = 0 OR avg_hours IS NULL THEN
        RETURN '< 2 hours';
    END IF;
    
    -- Format the response
    IF avg_hours < 1 THEN
        RETURN '< 1 hour';
    ELSE
        RETURN '< ' || CEIL(avg_hours)::TEXT || ' hours';
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION get_engineer_avg_response_time TO authenticated;

-- ========================================
-- 3. CREATE FUNCTION FOR COMPLETE ENGINEER STATS
-- ========================================
-- This function returns all statistics for an engineer in one call
CREATE OR REPLACE FUNCTION get_engineer_dashboard_stats(engineer_user_id UUID)
RETURNS TABLE(
    total_tickets BIGINT,
    new_today BIGINT,
    this_week BIGINT,
    pending_tickets BIGINT,
    accepted_tickets BIGINT,
    rejected_tickets BIGINT,
    spam_tickets BIGINT,
    high_priority_pending BIGINT,
    avg_response_time TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(t.id) as total_tickets,
        COUNT(CASE WHEN DATE(t.created_at) = CURRENT_DATE THEN 1 END) as new_today,
        COUNT(CASE WHEN t.created_at >= CURRENT_DATE - INTERVAL '7 days' THEN 1 END) as this_week,
        COUNT(CASE WHEN t.status = 'Pending' THEN 1 END) as pending_tickets,
        COUNT(CASE WHEN t.status = 'Accepted' THEN 1 END) as accepted_tickets,
        COUNT(CASE WHEN t.status = 'Rejected' THEN 1 END) as rejected_tickets,
        COUNT(CASE WHEN t.status = 'SPAM' THEN 1 END) as spam_tickets,
        COUNT(CASE WHEN t.severity = 'High' AND t.status = 'Pending' THEN 1 END) as high_priority_pending,
        get_engineer_avg_response_time(engineer_user_id) as avg_response_time
    FROM tickets t
    WHERE t.assigned_engineer_id = engineer_user_id;
END;
$$ LANGUAGE plpgsql;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION get_engineer_dashboard_stats TO authenticated;

-- ========================================
-- 4. INDEXES FOR PERFORMANCE
-- ========================================
-- Create indexes to speed up common queries

-- Index for finding tickets by assigned engineer
CREATE INDEX IF NOT EXISTS idx_tickets_assigned_engineer 
ON tickets(assigned_engineer_id) 
WHERE assigned_engineer_id IS NOT NULL;

-- Index for filtering by status
CREATE INDEX IF NOT EXISTS idx_tickets_status 
ON tickets(status);

-- Index for filtering by severity
CREATE INDEX IF NOT EXISTS idx_tickets_severity 
ON tickets(severity);

-- Index for date-based queries
CREATE INDEX IF NOT EXISTS idx_tickets_created_at 
ON tickets(created_at DESC);

-- Composite index for engineer + status queries
CREATE INDEX IF NOT EXISTS idx_tickets_engineer_status 
ON tickets(assigned_engineer_id, status) 
WHERE assigned_engineer_id IS NOT NULL;

-- Index for ticket_actions lookup
CREATE INDEX IF NOT EXISTS idx_ticket_actions_ticket_id 
ON ticket_actions(ticket_id, action_type, created_at);

-- ========================================
-- 5. SAMPLE QUERIES FOR TESTING
-- ========================================

-- Get all statistics for a specific engineer
-- Replace 'your-engineer-user-id' with actual UUID
/*
SELECT * FROM get_engineer_dashboard_stats('your-engineer-user-id');
*/

-- Get list of all engineers with their stats
/*
SELECT * FROM engineer_ticket_stats ORDER BY total_tickets DESC;
*/

-- Get tickets assigned to an engineer today
/*
SELECT * FROM tickets 
WHERE assigned_engineer_id = 'your-engineer-user-id' 
  AND DATE(created_at) = CURRENT_DATE
ORDER BY created_at DESC;
*/

-- Get high priority pending tickets for an engineer
/*
SELECT * FROM tickets 
WHERE assigned_engineer_id = 'your-engineer-user-id' 
  AND status = 'Pending' 
  AND severity = 'High'
ORDER BY created_at ASC;
*/

-- Get tickets from this week
/*
SELECT * FROM tickets 
WHERE assigned_engineer_id = 'your-engineer-user-id' 
  AND created_at >= CURRENT_DATE - INTERVAL '7 days'
ORDER BY created_at DESC;
*/

-- ========================================
-- 6. OPTIONAL: CREATE MATERIALIZED VIEW FOR BETTER PERFORMANCE
-- ========================================
-- If you have many engineers and tickets, consider using a materialized view
-- that refreshes periodically instead of calculating on each request

/*
CREATE MATERIALIZED VIEW engineer_stats_cached AS
SELECT * FROM engineer_ticket_stats;

-- Create a unique index for the materialized view
CREATE UNIQUE INDEX idx_engineer_stats_cached_id 
ON engineer_stats_cached(engineer_id);

-- Grant access
GRANT SELECT ON engineer_stats_cached TO authenticated;

-- Set up automatic refresh (optional, runs every hour)
-- You can also refresh manually: REFRESH MATERIALIZED VIEW CONCURRENTLY engineer_stats_cached;
*/

-- ========================================
-- NOTES:
-- ========================================
-- 1. The Android app currently calculates statistics in Java
--    but you can optimize by calling these functions directly
-- 2. The view 'engineer_ticket_stats' provides a quick overview
--    of all engineers and their statistics
-- 3. The function 'get_engineer_dashboard_stats' returns all
--    stats for one engineer in a single database call
-- 4. Indexes are created to speed up common queries
-- 5. Consider using the materialized view for large datasets
-- 
-- To use these optimizations in your Android app, you can:
-- - Call the function: /rest/v1/rpc/get_engineer_dashboard_stats
--   with POST body: {"engineer_user_id": "uuid-here"}
-- - Query the view: /rest/v1/engineer_ticket_stats?engineer_id=eq.uuid-here
-- ========================================
