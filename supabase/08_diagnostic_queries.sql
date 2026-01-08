-- Diagnostic Queries
-- Use these to troubleshoot and verify your setup

-- Check if all required columns exist
SELECT column_name, data_type, column_default, is_nullable
FROM information_schema.columns
WHERE table_name = 'tickets'
AND column_name IN (
    'deleted_by_citizen', 'deleted_by_council', 'deleted_by_engineer',
    'engineer_notes', 'assigned_engineer_id', 'assigned_engineer_name',
    'council_notes', 'is_spam', 'assigned_at'
)
ORDER BY column_name;

-- Check RLS is enabled
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE tablename IN ('tickets', 'profiles', 'ticket_actions');

-- View all policies on tickets table
SELECT 
    policyname,
    cmd AS operation,
    CASE 
        WHEN policyname LIKE '%citizen%' THEN 'Citizen'
        WHEN policyname LIKE '%council%' THEN 'Council'
        WHEN policyname LIKE '%engineer%' THEN 'Engineer'
        ELSE 'Other'
    END AS role_type
FROM pg_policies 
WHERE tablename = 'tickets'
ORDER BY role_type, cmd, policyname;

-- Check status constraint
SELECT constraint_name, check_clause
FROM information_schema.check_constraints
WHERE constraint_name = 'tickets_status_check';

-- View current user info (run while authenticated)
-- SELECT auth.uid() as my_user_id;
-- SELECT id, full_name, role, email FROM profiles WHERE id = auth.uid();

-- View sample tickets with all flags
SELECT 
    ticket_id,
    status,
    reporter_id,
    assigned_engineer_id,
    deleted_by_citizen,
    deleted_by_council,
    deleted_by_engineer,
    is_spam,
    created_at
FROM public.tickets
ORDER BY created_at DESC
LIMIT 10;

-- Check functions exist
SELECT routine_name, routine_type
FROM information_schema.routines
WHERE routine_schema = 'public'
AND routine_name IN (
    'mark_ticket_as_spam',
    'soft_delete_ticket_for_citizen',
    'soft_delete_ticket_for_council',
    'soft_delete_ticket_for_engineer',
    'get_council_avg_response_time',
    'get_engineer_avg_response_time',
    'is_user_in_role'
)
ORDER BY routine_name;

-- Check triggers exist
SELECT trigger_name, event_manipulation, event_object_table
FROM information_schema.triggers
WHERE trigger_name IN ('trg_handle_engineer_spam', 'ensure_spam_null_reason')
ORDER BY trigger_name;

-- Test queries for specific scenarios
-- Find tickets owned by current user (run as authenticated citizen)
-- SELECT id, ticket_id, status, deleted_by_citizen
-- FROM tickets WHERE reporter_id = auth.uid();

-- Find all spam tickets
-- SELECT ticket_id, status, is_spam, engineer_notes
-- FROM tickets WHERE is_spam = true;

-- Find tickets soft-deleted by any role
-- SELECT ticket_id, status, deleted_by_citizen, deleted_by_council, deleted_by_engineer
-- FROM tickets 
-- WHERE deleted_by_citizen = true 
--    OR deleted_by_council = true 
--    OR deleted_by_engineer = true;
