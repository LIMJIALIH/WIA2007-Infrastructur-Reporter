-- ========================================
-- COUNCIL DASHBOARD SUPABASE SETUP
-- ========================================
-- This script sets up Row Level Security (RLS) policies
-- to allow council/management users to view all tickets
-- ========================================

-- Step 1: Update role column constraints in profiles table
-- Your profiles table already has 'role' column, so we just add constraints

-- Add constraint to ensure valid roles (if not exists)
DO $$ 
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'check_role'
  ) THEN
    ALTER TABLE profiles
    ADD CONSTRAINT check_role 
    CHECK (role IN ('citizen', 'engineer', 'council', 'admin'));
  END IF;
END $$;

-- Add index for faster role queries
CREATE INDEX IF NOT EXISTS idx_profiles_role ON profiles(role);

-- ========================================
-- Step 2: Update RLS Policies for tickets table
-- ========================================

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Users can view their own tickets" ON tickets;
DROP POLICY IF EXISTS "Council can view all tickets" ON tickets;
DROP POLICY IF EXISTS "Users can insert their own tickets" ON tickets;
DROP POLICY IF EXISTS "Council can update all tickets" ON tickets;

-- Enable RLS on tickets table
ALTER TABLE tickets ENABLE ROW LEVEL SECURITY;

-- Policy 1: Citizens can view their own tickets
CREATE POLICY "Users can view their own tickets"
ON tickets
FOR SELECT
TO authenticated
USING (
  auth.uid() = reporter_id::uuid
);

-- Policy 2: Council members can view ALL tickets
CREATE POLICY "Council can view all tickets"
ON tickets
FOR SELECT
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role IN ('council', 'admin')
  )
);

-- Policy 3: Citizens can insert their own tickets
CREATE POLICY "Users can insert their own tickets"
ON tickets
FOR INSERT
TO authenticated
WITH CHECK (
  auth.uid() = reporter_id::uuid
);

-- Policy 4: Council members can update all tickets (for status changes)
CREATE POLICY "Council can update all tickets"
ON tickets
FOR UPDATE
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role IN ('council', 'admin', 'engineer')
  )
);

-- ========================================
-- Step 3: Update RLS Policies for ticket_images table
-- ========================================

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Users can view their ticket images" ON ticket_images;
DROP POLICY IF EXISTS "Council can view all ticket images" ON ticket_images;
DROP POLICY IF EXISTS "Users can insert their ticket images" ON ticket_images;

-- Enable RLS on ticket_images table
ALTER TABLE ticket_images ENABLE ROW LEVEL SECURITY;

-- Policy 1: Users can view images for their tickets
CREATE POLICY "Users can view their ticket images"
ON ticket_images
FOR SELECT
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM tickets
    WHERE tickets.id = ticket_images.ticket_id
    AND tickets.reporter_id = auth.uid()::text
  )
);

-- Policy 2: Council members can view all ticket images
CREATE POLICY "Council can view all ticket images"
ON ticket_images
FOR SELECT
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role IN ('council', 'admin', 'engineer')
  )
);

-- Policy 3: Users can insert images for their tickets
CREATE POLICY "Users can insert their ticket images"
ON ticket_images
FOR INSERT
TO authenticated
WITH CHECK (
  uploaded_by = auth.uid()::text
);

-- ========================================
-- Step 4: Create helper function to assign council role
-- ========================================

-- Function to update user role (run as admin in SQL editor)
CREATE OR REPLACE FUNCTION update_user_role(user_email TEXT, new_role TEXT)
RETURNS VOID AS $$
BEGIN
  UPDATE profiles
  SET role = new_role
  WHERE email = user_email;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ========================================
-- Step 5: Assign council role to specific users
-- ========================================
-- EXAMPLE: Update specific users to council role
-- Replace 'jiemm@example.com' with actual council member emails

-- To make a user a council member, run:
-- SELECT update_user_role('jiemm@example.com', 'council');

-- Or update directly:
-- UPDATE profiles
-- SET role = 'council'
-- WHERE email = 'jiemm@example.com';

-- To check current roles:
-- SELECT id, full_name, email, role FROM profiles;

-- ========================================
-- Step 6: Test the policies
-- ========================================

-- Test 1: Check if council user can see all tickets
-- (Run this while logged in as council user in your app)
-- SELECT * FROM tickets;

-- Test 2: Check if citizen can only see their tickets
-- (Run this while logged in as citizen in your app)
-- SELECT * FROM tickets;

-- Test 3: Verify user roles
-- SELECT email, role FROM profiles ORDER BY role;

-- ========================================
-- IMPORTANT NOTES:
-- ========================================
-- 1. After running this script, you need to assign council role to management users
-- 2. In your Android app, the council dashboard will automatically fetch all tickets
-- 3. Citizens will still only see their own tickets in CitizenDashboardActivity
-- 4. Storage bucket 'ticket-images' should already be public (as per previous setup)
-- ========================================
