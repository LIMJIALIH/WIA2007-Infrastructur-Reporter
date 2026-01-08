-- Row Level Security Policies
-- Controls who can see and modify tickets

-- Clean slate - drop all existing policies
ALTER TABLE public.tickets DISABLE ROW LEVEL SECURITY;

DO $$ 
DECLARE
    r RECORD;
BEGIN
    FOR r IN (SELECT policyname FROM pg_policies WHERE tablename = 'tickets' AND schemaname = 'public') LOOP
        EXECUTE 'DROP POLICY IF EXISTS "' || r.policyname || '" ON public.tickets';
    END LOOP;
END $$;

-- Set default values for existing rows
UPDATE public.tickets SET deleted_by_citizen = false WHERE deleted_by_citizen IS NULL;
UPDATE public.tickets SET deleted_by_council = false WHERE deleted_by_council IS NULL;
UPDATE public.tickets SET deleted_by_engineer = false WHERE deleted_by_engineer IS NULL;

ALTER TABLE public.tickets ENABLE ROW LEVEL SECURITY;

-- Helper function to check user role (prevents recursion in RLS policies)
CREATE OR REPLACE FUNCTION public.is_user_in_role(user_id_param UUID, role_name_param TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
STABLE
AS $$
DECLARE
    user_role TEXT;
BEGIN
    SELECT role INTO user_role FROM profiles WHERE id = user_id_param;
    RETURN user_role = role_name_param;
END;
$$;

GRANT EXECUTE ON FUNCTION public.is_user_in_role TO authenticated;

-- === CITIZEN POLICIES ===
-- Citizens can view their own tickets (not deleted by them)
CREATE POLICY "citizens_view_own_tickets"
ON public.tickets FOR SELECT TO authenticated
USING (
    reporter_id = auth.uid()
    AND COALESCE(deleted_by_citizen, false) = false
);

-- Citizens can create new tickets
CREATE POLICY "citizens_insert_tickets"
ON public.tickets FOR INSERT TO authenticated
WITH CHECK (reporter_id = auth.uid());

-- Citizens can update their own tickets (including soft-delete flag)
CREATE POLICY "citizens_update_own_tickets"
ON public.tickets FOR UPDATE TO authenticated
USING (reporter_id = auth.uid())
WITH CHECK (reporter_id = auth.uid());

-- === COUNCIL POLICIES ===
-- Council can view all tickets (not deleted by them)
CREATE POLICY "council_view_tickets"
ON public.tickets FOR SELECT TO authenticated
USING (
    public.is_user_in_role(auth.uid()::uuid, 'council')
    AND COALESCE(deleted_by_council, false) = false
);

-- Council can update any ticket
CREATE POLICY "council_update_tickets"
ON public.tickets FOR UPDATE TO authenticated
USING (public.is_user_in_role(auth.uid()::uuid, 'council'));

-- === ENGINEER POLICIES ===
-- Engineers can view assigned tickets (not deleted, not spam)
CREATE POLICY "engineers_view_assigned_tickets"
ON public.tickets FOR SELECT TO authenticated
USING (
    assigned_engineer_id = auth.uid()
    AND COALESCE(deleted_by_engineer, false) = false
    AND status != 'SPAM'
);

-- Engineers can update assigned tickets
CREATE POLICY "engineers_update_assigned_tickets"
ON public.tickets FOR UPDATE TO authenticated
USING (assigned_engineer_id = auth.uid());
