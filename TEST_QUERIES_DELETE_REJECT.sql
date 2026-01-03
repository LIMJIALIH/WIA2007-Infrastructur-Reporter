-- QUICK TEST QUERIES FOR DELETE AND REJECT FIXES
-- Run these in Supabase SQL Editor AFTER running FIX_DELETE_AND_REJECT_ISSUES.sql

-- ============================================================================
-- 1. VERIFY RLS POLICIES EXIST
-- ============================================================================

-- Should show 6 policies: 3 citizen (SELECT, INSERT, UPDATE), 2 council (SELECT, UPDATE), 2 engineer (SELECT, UPDATE)
SELECT 
    policyname,
    cmd AS operation,
    CASE 
        WHEN policyname LIKE '%Citizens%' THEN 'Citizen'
        WHEN policyname LIKE '%Council%' THEN 'Council'
        WHEN policyname LIKE '%Engineers%' THEN 'Engineer'
        ELSE 'Unknown'
    END AS role_type
FROM pg_policies
WHERE tablename = 'tickets'
ORDER BY role_type, cmd;

-- ============================================================================
-- 2. CHECK SOFT-DELETE COLUMNS EXIST
-- ============================================================================

-- Should show 3 columns with type boolean and default false
SELECT 
    column_name,
    data_type,
    column_default,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'tickets'
  AND column_name IN ('deleted_by_citizen', 'deleted_by_council', 'deleted_by_engineer')
ORDER BY column_name;

-- ============================================================================
-- 3. VIEW CURRENT TICKETS WITH DELETE FLAGS
-- ============================================================================

-- Run as Supabase admin/service role to see all tickets
SELECT 
    ticket_id,
    status,
    reporter_id,
    assigned_engineer_id,
    COALESCE(deleted_by_citizen, false) AS deleted_by_citizen,
    COALESCE(deleted_by_council, false) AS deleted_by_council,
    COALESCE(deleted_by_engineer, false) AS deleted_by_engineer,
    created_at
FROM public.tickets
ORDER BY created_at DESC
LIMIT 20;

-- ============================================================================
-- 4. FIND REJECTED TICKETS
-- ============================================================================

-- Find all tickets with status = 'Rejected'
SELECT 
    ticket_id,
    status,
    engineer_notes AS rejection_reason,
    deleted_by_citizen,
    deleted_by_council,
    deleted_by_engineer
FROM public.tickets
WHERE status = 'Rejected'
ORDER BY created_at DESC;

-- ============================================================================
-- 5. SIMULATE CITIZEN VIEW (replace <CITIZEN_UUID> with actual reporter_id)
-- ============================================================================

-- This mimics what the citizen sees - only tickets they haven't deleted
-- SELECT 
--     ticket_id,
--     status,
--     deleted_by_citizen
-- FROM public.tickets
-- WHERE reporter_id = '<CITIZEN_UUID>'::uuid
--   AND COALESCE(deleted_by_citizen, false) = false
-- ORDER BY created_at DESC;

-- ============================================================================
-- 6. SIMULATE COUNCIL VIEW
-- ============================================================================

-- This mimics what council sees - all non-deleted tickets
SELECT 
    ticket_id,
    status,
    reporter_id,
    assigned_engineer_id,
    deleted_by_council
FROM public.tickets
WHERE COALESCE(deleted_by_council, false) = false
ORDER BY created_at DESC
LIMIT 20;

-- ============================================================================
-- 7. SIMULATE ENGINEER VIEW (replace <ENGINEER_UUID> with actual engineer ID)
-- ============================================================================

-- This mimics what engineer sees - only assigned tickets they haven't deleted
-- SELECT 
--     ticket_id,
--     status,
--     deleted_by_engineer
-- FROM public.tickets
-- WHERE assigned_engineer_id = '<ENGINEER_UUID>'::uuid
--   AND COALESCE(deleted_by_engineer, false) = false
-- ORDER BY created_at DESC;

-- ============================================================================
-- 8. TEST SOFT-DELETE UPDATE (Citizen)
-- ============================================================================

-- Simulate citizen deleting a ticket (replace <TICKET_UUID> and <CITIZEN_UUID>)
-- UPDATE public.tickets
-- SET deleted_by_citizen = true
-- WHERE id = '<TICKET_UUID>'::uuid
--   AND reporter_id = '<CITIZEN_UUID>'::uuid;

-- Verify it worked:
-- SELECT ticket_id, deleted_by_citizen, deleted_by_council, deleted_by_engineer
-- FROM public.tickets
-- WHERE id = '<TICKET_UUID>'::uuid;

-- ============================================================================
-- 9. TEST SOFT-DELETE UPDATE (Council)
-- ============================================================================

-- Simulate council deleting a ticket (must be run by council user or service role)
-- UPDATE public.tickets
-- SET deleted_by_council = true
-- WHERE id = '<TICKET_UUID>'::uuid;

-- ============================================================================
-- 10. TEST SOFT-DELETE UPDATE (Engineer)
-- ============================================================================

-- Simulate engineer deleting a ticket (replace <TICKET_UUID> and <ENGINEER_UUID>)
-- UPDATE public.tickets
-- SET deleted_by_engineer = true
-- WHERE id = '<TICKET_UUID>'::uuid
--   AND assigned_engineer_id = '<ENGINEER_UUID>'::uuid;

-- ============================================================================
-- 11. TEST REJECT STATUS UPDATE
-- ============================================================================

-- Simulate engineer rejecting a ticket (replace <TICKET_UUID>)
-- UPDATE public.tickets
-- SET status = 'Rejected',
--     engineer_notes = 'Test rejection reason from SQL'
-- WHERE id = '<TICKET_UUID>'::uuid;

-- Verify status changed:
-- SELECT ticket_id, status, engineer_notes
-- FROM public.tickets
-- WHERE id = '<TICKET_UUID>'::uuid;

-- ============================================================================
-- 12. RESET A TICKET FOR TESTING
-- ============================================================================

-- Reset all delete flags and status to test again (replace <TICKET_UUID>)
-- UPDATE public.tickets
-- SET deleted_by_citizen = false,
--     deleted_by_council = false,
--     deleted_by_engineer = false,
--     status = 'Under_Review'
-- WHERE id = '<TICKET_UUID>'::uuid;

-- ============================================================================
-- 13. COUNT TICKETS BY STATUS AND DELETE FLAGS
-- ============================================================================

-- Summary of ticket states
SELECT 
    status,
    COUNT(*) AS total_tickets,
    SUM(CASE WHEN COALESCE(deleted_by_citizen, false) = true THEN 1 ELSE 0 END) AS deleted_by_citizen_count,
    SUM(CASE WHEN COALESCE(deleted_by_council, false) = true THEN 1 ELSE 0 END) AS deleted_by_council_count,
    SUM(CASE WHEN COALESCE(deleted_by_engineer, false) = true THEN 1 ELSE 0 END) AS deleted_by_engineer_count
FROM public.tickets
GROUP BY status
ORDER BY status;

-- ============================================================================
-- 14. FIND TICKETS DELETED BY ALL THREE ROLES
-- ============================================================================

-- These are tickets that are hidden from everyone (but still in database)
SELECT 
    ticket_id,
    status,
    deleted_by_citizen,
    deleted_by_council,
    deleted_by_engineer,
    created_at
FROM public.tickets
WHERE COALESCE(deleted_by_citizen, false) = true
  AND COALESCE(deleted_by_council, false) = true
  AND COALESCE(deleted_by_engineer, false) = true
ORDER BY created_at DESC;

-- ============================================================================
-- 15. CHECK IF is_user_in_role FUNCTION EXISTS
-- ============================================================================

-- Should return one row with function name
SELECT 
    routine_name,
    routine_type,
    security_type
FROM information_schema.routines
WHERE routine_schema = 'public'
  AND routine_name = 'is_user_in_role';

-- If function doesn't exist, you need to create it first:
-- See FIX_PROFILES_INFINITE_RECURSION.sql

-- ============================================================================
-- 16. VERIFY user_roles TABLE EXISTS
-- ============================================================================

-- Should return one row
SELECT table_name, table_type
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name = 'user_roles';

-- View contents:
SELECT * FROM public.user_roles LIMIT 10;

-- ============================================================================
-- EXAMPLE: Complete Test Flow
-- ============================================================================

/*
STEP 1: Find a test ticket
*/
-- SELECT id, ticket_id, status, reporter_id, assigned_engineer_id
-- FROM public.tickets
-- WHERE status = 'Under_Review'
-- LIMIT 1;

/*
STEP 2: Engineer rejects the ticket
*/
-- UPDATE public.tickets
-- SET status = 'Rejected',
--     engineer_notes = 'Test rejection from SQL'
-- WHERE ticket_id = 'T1767...';

/*
STEP 3: Verify all roles see it as Rejected
*/
-- SELECT ticket_id, status, engineer_notes,
--        deleted_by_citizen, deleted_by_council, deleted_by_engineer
-- FROM public.tickets
-- WHERE ticket_id = 'T1767...';

/*
STEP 4: Engineer deletes it
*/
-- UPDATE public.tickets
-- SET deleted_by_engineer = true
-- WHERE ticket_id = 'T1767...'
--   AND assigned_engineer_id = '<ENGINEER_UUID>'::uuid;

/*
STEP 5: Verify engineer can't see it, but citizen and council can
*/
-- Engineer view (should be empty):
-- SELECT ticket_id FROM public.tickets
-- WHERE ticket_id = 'T1767...'
--   AND assigned_engineer_id = '<ENGINEER_UUID>'::uuid
--   AND COALESCE(deleted_by_engineer, false) = false;

-- Citizen/Council view (should still show):
-- SELECT ticket_id, status FROM public.tickets
-- WHERE ticket_id = 'T1767...'
--   AND COALESCE(deleted_by_council, false) = false;
*/
