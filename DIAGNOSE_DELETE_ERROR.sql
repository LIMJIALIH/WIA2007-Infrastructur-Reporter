-- ========================================
-- DIAGNOSE HTTP 400 / PGRST204 ERROR
-- ========================================
-- Run these queries to find the root cause
-- Error PGRST204 means "0 rows updated"

-- ========================================
-- STEP 1: Check if deleted_by_citizen column exists
-- ========================================
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'tickets'
AND column_name IN ('deleted_by_citizen', 'deleted_by_council', 'deleted_by_engineer', 'id', 'reporter_id');

-- Expected: Should show all these columns
-- If deleted_by_citizen is missing, run SPAM_AND_DELETE_FIXES.sql Step 1

-- ========================================
-- STEP 2: Check if RLS is enabled
-- ========================================
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE tablename = 'tickets';

-- Expected: rowsecurity = true
-- If false, run: ALTER TABLE tickets ENABLE ROW LEVEL SECURITY;

-- ========================================
-- STEP 3: Check ALL policies on tickets table
-- ========================================
SELECT 
    policyname,
    cmd,
    roles,
    qual,
    with_check
FROM pg_policies 
WHERE tablename = 'tickets'
ORDER BY cmd, policyname;

-- Expected: Should see at least 3 UPDATE policies:
--   1. "Citizens can soft delete own tickets" (UPDATE)
--   2. "Council can soft delete tickets" (UPDATE)
--   3. "Engineers can soft delete assigned tickets" (UPDATE)

-- If no UPDATE policies exist, this is your problem!
-- Run SPAM_AND_DELETE_FIXES.sql Step 4

-- ========================================
-- STEP 4: Check current user role
-- ========================================
-- Get your current user ID (run this while logged in as citizen)
SELECT auth.uid() as current_user_id;

-- Check if that user exists in profiles with role 'citizen'
SELECT id, full_name, role, email
FROM profiles
WHERE id = auth.uid();

-- Expected: role should be 'citizen'
-- If role is NULL or wrong, update it:
-- UPDATE profiles SET role = 'citizen' WHERE id = auth.uid();

-- ========================================
-- STEP 5: Test manual UPDATE (as citizen user)
-- ========================================
-- IMPORTANT: You must be authenticated as a citizen user when running this!
-- Replace the UUID below with an actual ticket ID from YOUR database

-- First, find a ticket owned by you:
SELECT id, ticket_id, reporter_id, deleted_by_citizen, status
FROM tickets
WHERE reporter_id = auth.uid()
LIMIT 1;

-- Try to update it manually (replace the UUID):
-- UPDATE tickets 
-- SET deleted_by_citizen = TRUE 
-- WHERE id = 'PASTE-YOUR-TICKET-UUID-HERE';

-- If this fails with permission error, the UPDATE policy is not working
-- If this succeeds, the problem is in the app code (wrong ID format)

-- ========================================
-- STEP 6: Check data types match
-- ========================================
-- Make sure id and reporter_id are both UUID type
SELECT 
    column_name,
    data_type,
    udt_name
FROM information_schema.columns
WHERE table_name = 'tickets'
AND column_name IN ('id', 'reporter_id');

-- Expected: Both should be 'uuid' type
-- If reporter_id is 'text', this could cause auth.uid() comparison to fail

-- ========================================
-- STEP 7: Debug a specific ticket
-- ========================================
-- Replace with the ticket ID from your app (the one that failed to delete)
-- Use the UUID from the error, not the ticket_id string

/*
SELECT 
    id,
    ticket_id,
    reporter_id,
    deleted_by_citizen,
    status,
    (reporter_id::uuid = auth.uid()) as is_owner
FROM tickets
WHERE id = 'PASTE-THE-UUID-HERE';
*/

-- The 'is_owner' column should be TRUE if you own this ticket

-- ========================================
-- COMMON ISSUES & FIXES
-- ========================================

-- Issue 1: No UPDATE policies exist
-- Fix: Run SPAM_AND_DELETE_FIXES.sql (Step 4)

-- Issue 2: User role is not 'citizen'
-- Fix: UPDATE profiles SET role = 'citizen' WHERE id = auth.uid();

-- Issue 3: reporter_id is text but auth.uid() returns UUID
-- Fix: UPDATE policies need reporter_id::uuid = auth.uid()
--      (Already fixed in SPAM_AND_DELETE_FIXES.sql)

-- Issue 4: Wrong ticket ID being passed from app
-- Fix: Make sure app passes the UUID 'id' field, not 'ticket_id' string
--      Check Logcat for "Ticket DB ID: XXX" to see what's being sent

-- Issue 5: Access token expired or invalid
-- Fix: Logout and login again in the app
