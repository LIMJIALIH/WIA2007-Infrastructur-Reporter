-- FIX: HTTP 403 error when deleting tickets
-- SOFT DELETE: Tickets remain in database but hidden from specific roles

-- This SQL creates RLS policies that allow:
-- 1. Citizens to mark tickets as deleted_by_citizen (hides from citizen view only)
-- 2. Council to mark tickets as deleted_by_council (hides from council view only)
-- 3. Engineers to mark tickets as deleted_by_engineer (hides from engineer view only)
-- 4. Tickets stay in database and visible to other roles unless they also delete

-- Step 1: Disable RLS temporarily to modify policies safely
ALTER TABLE public.tickets DISABLE ROW LEVEL SECURITY;

-- Step 2: Drop any existing conflicting policies on tickets
DROP POLICY IF EXISTS "Users can view own tickets" ON public.tickets;
DROP POLICY IF EXISTS "Users can insert own tickets" ON public.tickets;
DROP POLICY IF EXISTS "Users can update own tickets" ON public.tickets;
DROP POLICY IF EXISTS "Users can delete own tickets" ON public.tickets;
DROP POLICY IF EXISTS "Citizens can view their tickets" ON public.tickets;
DROP POLICY IF EXISTS "Citizens can insert tickets" ON public.tickets;
DROP POLICY IF EXISTS "Citizens can update their tickets" ON public.tickets;
DROP POLICY IF EXISTS "Council can view all tickets" ON public.tickets;
DROP POLICY IF EXISTS "Council can update tickets" ON public.tickets;
DROP POLICY IF EXISTS "Engineers can view assigned tickets" ON public.tickets;
DROP POLICY IF EXISTS "Engineers can update assigned tickets" ON public.tickets;
DROP POLICY IF EXISTS "Enable read access for all users" ON public.tickets;
DROP POLICY IF EXISTS "Enable insert for authenticated users only" ON public.tickets;
DROP POLICY IF EXISTS "Enable update for users based on id" ON public.tickets;

-- Step 3: Re-enable RLS
ALTER TABLE public.tickets ENABLE ROW LEVEL SECURITY;

-- Step 4: Create safe, non-conflicting policies for tickets

-- CITIZEN policies
CREATE POLICY "Citizens can view their non-deleted tickets"
  ON public.tickets FOR SELECT
  TO authenticated
  USING (
    reporter_id = auth.uid()
    AND COALESCE(deleted_by_citizen, false) = false
  );

CREATE POLICY "Citizens can insert new tickets"
  ON public.tickets FOR INSERT
  TO authenticated
  WITH CHECK (reporter_id = auth.uid());

CREATE POLICY "Citizens can soft-delete their tickets"
  ON public.tickets FOR UPDATE
  TO authenticated
  USING (reporter_id = auth.uid())
  WITH CHECK (reporter_id = auth.uid());

-- COUNCIL policies (using user_roles helper to avoid recursion)
CREATE POLICY "Council can view non-deleted tickets"
  ON public.tickets FOR SELECT
  TO authenticated
  USING (
    public.is_user_in_role(auth.uid()::uuid, 'council')
    AND COALESCE(deleted_by_council, false) = false
  );

CREATE POLICY "Council can update and soft-delete tickets"
  ON public.tickets FOR UPDATE
  TO authenticated
  USING (public.is_user_in_role(auth.uid()::uuid, 'council'))
  WITH CHECK (public.is_user_in_role(auth.uid()::uuid, 'council'));

-- ENGINEER policies
CREATE POLICY "Engineers can view their assigned non-deleted tickets"
  ON public.tickets FOR SELECT
  TO authenticated
  USING (
    assigned_engineer_id = auth.uid()
    AND COALESCE(deleted_by_engineer, false) = false
  );

CREATE POLICY "Engineers can update their assigned tickets"
  ON public.tickets FOR UPDATE
  TO authenticated
  USING (assigned_engineer_id = auth.uid())
  WITH CHECK (assigned_engineer_id = auth.uid());

-- Step 5: Verify policies
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual, with_check
FROM pg_policies
WHERE tablename = 'tickets'
ORDER BY policyname;

-- Step 6: Quick test (replace <YOUR_AUTH_UID> with actual UUID)
-- Test citizen view (should only see tickets not deleted by citizen):
-- SELECT id, ticket_id, status, deleted_by_citizen FROM public.tickets WHERE reporter_id = '<YOUR_AUTH_UID>'::uuid;

-- Test update (soft delete) - should succeed:
-- UPDATE public.tickets SET deleted_by_citizen = true WHERE id = '<TICKET_ID>'::uuid AND reporter_id = '<YOUR_AUTH_UID>'::uuid;
