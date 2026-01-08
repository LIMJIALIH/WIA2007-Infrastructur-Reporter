-- Statistics and Dashboard Views
-- Provides quick stats for engineer and council dashboards

-- Engineer statistics view
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

-- Council dashboard statistics view
CREATE OR REPLACE VIEW council_dashboard_stats AS
SELECT 
    COUNT(*) as total_reports,
    COUNT(CASE WHEN status = 'Pending' THEN 1 END) as total_pending,
    COUNT(CASE WHEN status = 'UNDER_REVIEW' THEN 1 END) as under_review,
    COUNT(CASE WHEN status = 'Accepted' THEN 1 END) as completed,
    COUNT(CASE WHEN status IN ('Rejected', 'SPAM') THEN 1 END) as spam,
    COUNT(CASE WHEN status = 'Pending' AND severity = 'High' THEN 1 END) as high_priority_pending
FROM tickets
WHERE COALESCE(deleted_by_council, false) = false;

-- Calculate average response time for council
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

-- Calculate average response time for specific engineer
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
      AND ta.id = (
          SELECT ta2.id FROM ticket_actions ta2
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

-- Grant permissions
GRANT SELECT ON engineer_ticket_stats TO authenticated;
GRANT SELECT ON council_dashboard_stats TO authenticated;
GRANT EXECUTE ON FUNCTION get_council_avg_response_time TO authenticated;
GRANT EXECUTE ON FUNCTION get_engineer_avg_response_time TO authenticated;
