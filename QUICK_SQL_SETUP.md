# Quick SQL Setup - Run These in Supabase SQL Editor

## Step 1: Add Soft Delete Columns
```sql
ALTER TABLE tickets 
ADD COLUMN IF NOT EXISTS deleted_by_citizen BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS deleted_by_council BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS deleted_by_engineer BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_citizen ON tickets(deleted_by_citizen);
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_council ON tickets(deleted_by_council);
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_engineer ON tickets(deleted_by_engineer);
```

## Step 2: Create SPAM Mark Function
```sql
CREATE OR REPLACE FUNCTION mark_ticket_as_spam(ticket_id_param UUID)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  UPDATE tickets
  SET 
    status = 'SPAM',
    reason = NULL,
    updated_at = NOW()
  WHERE id = ticket_id_param;
END;
$$;

GRANT EXECUTE ON FUNCTION mark_ticket_as_spam(UUID) TO authenticated;
```

## Step 3: Update RLS Policies
```sql
-- Drop old policies
DROP POLICY IF EXISTS "Users can view their own tickets" ON tickets;
DROP POLICY IF EXISTS "Council can view all tickets" ON tickets;
DROP POLICY IF EXISTS "Engineers can view assigned tickets" ON tickets;

-- Create new policies with soft delete support
CREATE POLICY "Citizens can view their own tickets"
ON tickets FOR SELECT TO authenticated
USING (
  auth.uid() = reporter_id::uuid
  AND deleted_by_citizen = FALSE
  AND EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role = 'citizen'
  )
);

CREATE POLICY "Council can view non-deleted tickets"
ON tickets FOR SELECT TO authenticated
USING (
  deleted_by_council = FALSE
  AND EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role IN ('council', 'admin')
  )
);

CREATE POLICY "Engineers can view assigned tickets"
ON tickets FOR SELECT TO authenticated
USING (
  deleted_by_engineer = FALSE
  AND EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role = 'engineer'
    AND (
      tickets.assigned_engineer_id::uuid = auth.uid()
      OR tickets.status = 'UNDER_REVIEW'
    )
  )
);
```

## Step 4: Create SPAM Trigger
```sql
CREATE OR REPLACE FUNCTION enforce_spam_null_reason()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.status = 'SPAM' THEN
    NEW.reason = NULL;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS ensure_spam_null_reason ON tickets;
CREATE TRIGGER ensure_spam_null_reason
BEFORE INSERT OR UPDATE OF status ON tickets
FOR EACH ROW
EXECUTE FUNCTION enforce_spam_null_reason();
```

## Verification
```sql
-- Check columns
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'tickets'
AND column_name IN ('deleted_by_citizen', 'deleted_by_council', 'deleted_by_engineer');

-- Check function
SELECT routine_name FROM information_schema.routines
WHERE routine_name = 'mark_ticket_as_spam';

-- Check trigger
SELECT trigger_name FROM information_schema.triggers
WHERE trigger_name = 'ensure_spam_null_reason';

-- Check policies
SELECT policyname FROM pg_policies WHERE tablename = 'tickets';
```

## If You Get Errors

### Error: "policy already exists"
```sql
-- Drop the policy first, then recreate
DROP POLICY IF EXISTS "policy_name_here" ON tickets;
```

### Error: "function already exists"
```sql
-- The CREATE OR REPLACE should handle this
-- If not, drop it first:
DROP FUNCTION IF EXISTS mark_ticket_as_spam(UUID);
```

### Error: "trigger already exists"
```sql
-- Already handled by DROP TRIGGER IF EXISTS
-- If still error:
DROP TRIGGER ensure_spam_null_reason ON tickets;
```

## Test After Setup
```sql
-- Test SPAM marking (replace with actual ticket UUID)
SELECT mark_ticket_as_spam('your-ticket-uuid-here');

-- Verify reason is NULL
SELECT id, status, reason FROM tickets WHERE status = 'SPAM';

-- Test soft delete visibility
UPDATE tickets SET deleted_by_citizen = TRUE WHERE id = 'some-uuid';
-- Citizen should not see this ticket anymore when querying
```
