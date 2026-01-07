-- FIX: Engineer/Council delete HTTP 403 + Council refresh showing deleted tickets
-- FIX: Reject ticket status flow for all roles

-- ============================================================================
-- PART 1: FIX SOFT DELETE RLS POLICIES
-- ============================================================================

-- Step 1: Disable RLS temporarily
ALTER TABLE public.tickets DISABLE ROW LEVEL SECURITY;

-- Step 2: Drop ALL existing policies on tickets table
DROP POLICY IF EXISTS "Users can view own tickets" ON public.tickets;
DROP POLICY IF EXISTS "Users can insert own tickets" ON public.tickets;
DROP POLICY IF EXISTS "Users can update own tickets" ON public.tickets;
DROP POLICY IF EXISTS "Users can delete own tickets" ON public.tickets;
DROP POLICY IF EXISTS "Citizens can view their tickets" ON public.tickets;
DROP POLICY IF EXISTS "Citizens can view their non-deleted tickets" ON public.tickets;
DROP POLICY IF EXISTS "Citizens can insert tickets" ON public.tickets;
DROP POLICY IF EXISTS "Citizens can insert new tickets" ON public.tickets;
DROP POLICY IF EXISTS "Citizens can update their tickets" ON public.tickets;
DROP POLICY IF EXISTS "Citizens can soft-delete their tickets" ON public.tickets;
DROP POLICY IF EXISTS "Council can view all tickets" ON public.tickets;
DROP POLICY IF EXISTS "Council can view non-deleted tickets" ON public.tickets;
DROP POLICY IF EXISTS "Council can update tickets" ON public.tickets;
DROP POLICY IF EXISTS "Council can update and soft-delete tickets" ON public.tickets;
DROP POLICY IF EXISTS "Engineers can view assigned tickets" ON public.tickets;
DROP POLICY IF EXISTS "Engineers can view their assigned non-deleted tickets" ON public.tickets;
DROP POLICY IF EXISTS "Engineers can update assigned tickets" ON public.tickets;
DROP POLICY IF EXISTS "Engineers can update their assigned tickets" ON public.tickets;
DROP POLICY IF EXISTS "Enable read access for all users" ON public.tickets;
DROP POLICY IF EXISTS "Enable insert for authenticated users only" ON public.tickets;
DROP POLICY IF EXISTS "Enable update for users based on id" ON public.tickets;

-- Step 3: Ensure soft-delete columns exist with default false
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='tickets' AND column_name='deleted_by_citizen') THEN
        ALTER TABLE public.tickets ADD COLUMN deleted_by_citizen BOOLEAN DEFAULT false;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='tickets' AND column_name='deleted_by_council') THEN
        ALTER TABLE public.tickets ADD COLUMN deleted_by_council BOOLEAN DEFAULT false;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='tickets' AND column_name='deleted_by_engineer') THEN
        ALTER TABLE public.tickets ADD COLUMN deleted_by_engineer BOOLEAN DEFAULT false;
    END IF;
END $$;

-- Set existing NULL values to false
UPDATE public.tickets SET deleted_by_citizen = false WHERE deleted_by_citizen IS NULL;
UPDATE public.tickets SET deleted_by_council = false WHERE deleted_by_council IS NULL;
UPDATE public.tickets SET deleted_by_engineer = false WHERE deleted_by_engineer IS NULL;

-- Step 4: Re-enable RLS
ALTER TABLE public.tickets ENABLE ROW LEVEL SECURITY;

-- Step 5: Create comprehensive RLS policies

-- ============================================================================
-- CITIZEN POLICIES
-- ============================================================================

-- Citizens can view their own tickets that they haven't deleted
CREATE POLICY "Citizens can view their non-deleted tickets"
  ON public.tickets FOR SELECT
  TO authenticated
  USING (
    reporter_id = auth.uid()
    AND COALESCE(deleted_by_citizen, false) = false
  );

-- Citizens can create new tickets
CREATE POLICY "Citizens can insert new tickets"
  ON public.tickets FOR INSERT
  TO authenticated
  WITH CHECK (reporter_id = auth.uid());

-- Citizens can update their own tickets (including soft-delete flag)
CREATE POLICY "Citizens can update their tickets"
  ON public.tickets FOR UPDATE
  TO authenticated
  USING (reporter_id = auth.uid())
  WITH CHECK (reporter_id = auth.uid());

-- ============================================================================
-- COUNCIL POLICIES (using is_user_in_role to avoid recursion)
-- ============================================================================

-- Council can view all tickets they haven't deleted
CREATE POLICY "Council can view non-deleted tickets"
  ON public.tickets FOR SELECT
  TO authenticated
  USING (
    public.is_user_in_role(auth.uid()::uuid, 'council')
    AND COALESCE(deleted_by_council, false) = false
  );

-- Council can update ALL tickets (assign engineers, soft-delete, etc.)
CREATE POLICY "Council can update all tickets"
  ON public.tickets FOR UPDATE
  TO authenticated
  USING (public.is_user_in_role(auth.uid()::uuid, 'council'))
  WITH CHECK (public.is_user_in_role(auth.uid()::uuid, 'council'));

-- ============================================================================
-- ENGINEER POLICIES
-- ============================================================================

-- Engineers can view tickets assigned to them that they haven't deleted
CREATE POLICY "Engineers can view assigned non-deleted tickets"
  ON public.tickets FOR SELECT
  TO authenticated
  USING (
    assigned_engineer_id = auth.uid()
    AND COALESCE(deleted_by_engineer, false) = false
  );

-- Engineers can update their assigned tickets (accept/reject/spam, soft-delete)
CREATE POLICY "Engineers can update assigned tickets"
  ON public.tickets FOR UPDATE
  TO authenticated
  USING (assigned_engineer_id = auth.uid())
  WITH CHECK (assigned_engineer_id = auth.uid());

-- ============================================================================
-- PART 2: VERIFY POLICIES
-- ============================================================================

SELECT 
    schemaname, 
    tablename, 
    policyname, 
    permissive, 
    roles, 
    cmd, 
    qual AS using_expression,
    with_check AS with_check_expression
FROM pg_policies
WHERE tablename = 'tickets'
ORDER BY cmd, policyname;

-- ============================================================================
-- PART 3: TEST QUERIES
-- ============================================================================

-- Test 1: Check soft-delete columns exist and have defaults
SELECT 
    column_name, 
    data_type, 
    column_default,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'tickets' 
  AND column_name IN ('deleted_by_citizen', 'deleted_by_council', 'deleted_by_engineer')
ORDER BY column_name;

-- Test 2: View current ticket statuses (run as Supabase admin or service role)
SELECT 
    id,
    ticket_id,
    status,
    reporter_id,
    assigned_engineer_id,
    deleted_by_citizen,
    deleted_by_council,
    deleted_by_engineer,
    created_at
FROM public.tickets
ORDER BY created_at DESC
LIMIT 10;

-- Test 3: Simulate citizen soft-delete (replace <TICKET_UUID> with actual ticket ID)
-- UPDATE public.tickets 
-- SET deleted_by_citizen = true 
-- WHERE id = '<TICKET_UUID>'::uuid;

-- Test 4: Simulate council soft-delete
-- UPDATE public.tickets 
-- SET deleted_by_council = true 
-- WHERE id = '<TICKET_UUID>'::uuid;

-- Test 5: Simulate engineer soft-delete
-- UPDATE public.tickets 
-- SET deleted_by_engineer = true 
-- WHERE id = '<TICKET_UUID>'::uuid;

-- ============================================================================
-- PART 4: EXPLANATION OF SOFT-DELETE BEHAVIOR
-- ============================================================================

/*
HOW SOFT-DELETE WORKS:

1. CITIZEN deletes a ticket:
   - Sets deleted_by_citizen = true
   - Ticket disappears from citizen dashboard
   - Ticket STILL visible to council and engineer

2. COUNCIL deletes a ticket:
   - Sets deleted_by_council = true
   - Ticket disappears from council dashboard  
   - Ticket STILL visible to citizen and engineer

3. ENGINEER deletes a ticket:
   - Sets deleted_by_engineer = true
   - Ticket disappears from engineer dashboard
   - Ticket STILL visible to citizen and council

4. REFRESH BEHAVIOR:
   - Each role's SELECT policy filters by their deleted_by_* flag
   - When council refreshes: WHERE deleted_by_council = false
   - When engineer refreshes: WHERE deleted_by_engineer = false
   - When citizen refreshes: WHERE deleted_by_citizen = false

5. STATUS FLOW AFTER REJECT:
   - Engineer clicks "Reject" → status becomes "Rejected"
   - ALL roles see status = "Rejected" (not "Under_Review")
   - Engineer can delete rejected tickets
   - Council sees ticket as completed with "REJECTED" status
   - Citizen sees ticket as completed with "REJECTED" status
*/
