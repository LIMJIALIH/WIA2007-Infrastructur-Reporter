-- FIX: Infinite recursion in profiles table RLS policy
-- Error: "infinite recursion detected in policy for relation \"profiles\""
-- 
-- CAUSE: The profiles table has RLS policies that reference themselves in a circular way.
-- This commonly happens when a policy tries to check the profiles table while accessing it.
--
-- SOLUTION: Drop and recreate policies with proper non-recursive logic

-- Step 1: Temporarily disable RLS to clean up
ALTER TABLE profiles DISABLE ROW LEVEL SECURITY;

-- Step 2: Drop all existing policies on profiles
DROP POLICY IF EXISTS "Users can view their own profile" ON profiles;
DROP POLICY IF EXISTS "Users can update their own profile" ON profiles;
DROP POLICY IF EXISTS "Users can insert their own profile" ON profiles;
DROP POLICY IF EXISTS "Public profiles are viewable by everyone" ON profiles;
DROP POLICY IF EXISTS "Enable read access for all users" ON profiles;
DROP POLICY IF EXISTS "Enable insert for authenticated users only" ON profiles;
DROP POLICY IF EXISTS "Enable update for users based on user_id" ON profiles;
DROP POLICY IF EXISTS "Enable select for authenticated users" ON profiles;
DROP POLICY IF EXISTS "Enable insert access for all users" ON profiles;
DROP POLICY IF EXISTS "Enable update access for users based on id" ON profiles;

-- Step 3: Re-enable RLS
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- Step 4: Create simple, non-recursive policies

-- Allow users to view their own profile (using auth.uid() directly, no subquery)
CREATE POLICY "Users can view own profile"
ON profiles FOR SELECT
TO authenticated
USING (auth.uid() = user_id);

-- Allow users to insert their own profile during signup
CREATE POLICY "Users can insert own profile"
ON profiles FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = user_id);

-- Allow users to update their own profile
CREATE POLICY "Users can update own profile"
ON profiles FOR UPDATE
TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- Optional: Allow council/admin users to view all profiles (if needed for engineer assignment)
-- Only uncomment if your app needs this feature
-- CREATE POLICY "Council can view all profiles"
-- ON profiles FOR SELECT
-- TO authenticated
-- USING (
--   auth.uid() IN (
--     SELECT user_id FROM profiles WHERE role = 'council'
--   )
-- );

-- Step 5: Verify the policies
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual, with_check
FROM pg_policies
WHERE tablename = 'profiles'
ORDER BY policyname;
