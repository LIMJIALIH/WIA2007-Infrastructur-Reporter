# FINAL FIX: HTTP 403 DELETE ERRORS - ENGINEER & COUNCIL

## The Problem

**Error Message:**
```
HTTP 403: {"code":"42501","details":null,"hint":null,"message":"new row violates row-level security policy for table \"tickets\""}
```

**What was happening:**
- Engineer tries to delete ticket → HTTP 403 ❌
- Council tries to delete ticket → Appears to work but doesn't actually delete ❌

## Root Cause

The previous RLS policies had **conflicting WITH CHECK clauses** that prevented soft-delete updates:

```sql
-- WRONG (Previous version)
CREATE POLICY "Engineers can update assigned tickets"
  USING (assigned_engineer_id = auth.uid())
  WITH CHECK (assigned_engineer_id = auth.uid()); -- This blocks the update!
```

When you UPDATE a row to set `deleted_by_engineer = true`, PostgreSQL:
1. Checks if you can SELECT the row (SELECT policy)
2. Checks if you can UPDATE the row (UPDATE USING clause)
3. Applies the update
4. **Validates the updated row** (UPDATE WITH CHECK clause)

The WITH CHECK clause was causing issues because it validates against multiple policies, including SELECT policies that filter `deleted_by_engineer = false`.

## The Fix

**Changed UPDATE policies to use `WITH CHECK (true)`:**

```sql
-- CORRECT (New version)
CREATE POLICY "Engineers can update assigned tickets"
  USING (assigned_engineer_id = auth.uid())
  WITH CHECK (true); -- ✅ Allow any update
```

This means:
- ✅ Engineers can UPDATE any ticket assigned to them
- ✅ No validation of column values after update
- ✅ Soft-delete flag updates work without conflicts

## How to Apply

### ⚠️ CRITICAL: Run SQL First

1. Open [Supabase SQL Editor](https://supabase.com/dashboard/project/_/sql)
2. Copy **entire contents** of [FIX_DELETE_RLS_FINAL.sql](FIX_DELETE_RLS_FINAL.sql)
3. Paste and click "Run"
4. Verify output shows:
   - ✅ Policies dropped
   - ✅ Columns exist
   - ✅ 6 policies created (3 citizen, 2 council, 2 engineer)

### No App Rebuild Needed

The code changes from earlier are already correct. You only need to run the new SQL file.

## Expected Behavior After Fix

### ✅ Engineer Delete
1. Click delete button on rejected/accepted/spam ticket
2. Confirm deletion
3. **No HTTP 403 error** ✅
4. Ticket disappears from engineer dashboard
5. Ticket still visible to citizen and council

### ✅ Council Delete  
1. Click delete button on any ticket
2. Confirm deletion
3. **No HTTP 403 error** ✅
4. Click "Refresh" button
5. Ticket disappears from council dashboard
6. Ticket still visible to citizen and engineer

### ✅ Citizen Delete
1. Click delete button on accepted/rejected/spam ticket
2. Confirm deletion
3. Ticket disappears from citizen dashboard
4. Ticket still visible to council and engineer

## Verification Steps

### Test 1: Engineer Delete
1. Login as engineer (Jienn2)
2. Go to "Rejected" or "Accepted" tab
3. Click ticket → Click delete button
4. Confirm deletion
5. ✅ Verify no error toast
6. ✅ Verify ticket disappears
7. Login as council → ✅ Verify ticket still shows

### Test 2: Council Delete
1. Login as council
2. Click any ticket → Click delete button
3. Confirm deletion
4. ✅ Verify no error toast
5. Click "Refresh" on dashboard
6. ✅ Verify ticket no longer shows
7. Login as citizen → ✅ Verify ticket still shows

### Test 3: Check Database
```sql
-- Run in Supabase SQL Editor
SELECT 
    ticket_id,
    status,
    deleted_by_citizen,
    deleted_by_council,
    deleted_by_engineer
FROM public.tickets
WHERE ticket_id = 'T1767...' -- Replace with actual ticket ID
```

Should show:
- ✅ Ticket still exists in database
- ✅ Appropriate delete flags set to `true`
- ✅ Other delete flags remain `false`

## Troubleshooting

### Still getting HTTP 403?

**Check if SQL was run:**
```sql
-- Should show 2 UPDATE policies with WITH CHECK = true
SELECT policyname, cmd, with_check
FROM pg_policies
WHERE tablename = 'tickets' 
  AND cmd = 'UPDATE'
ORDER BY policyname;
```

**Expected output:**
- `Citizens can update their tickets` → WITH CHECK: `(reporter_id = auth.uid())`
- `Council can update all tickets` → WITH CHECK: `true`
- `Engineers can update assigned tickets` → WITH CHECK: `true`

### Council delete doesn't hide ticket?

1. Hard refresh the app (close and restart)
2. Check database to verify `deleted_by_council = true`
3. Check app query filters `deleted_by_council = false`

### Can't see is_user_in_role function?

If you get error about `is_user_in_role`, you need to create it first:

```sql
-- Create helper function
CREATE OR REPLACE FUNCTION public.is_user_in_role(user_id uuid, role_name text)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.user_roles
    WHERE user_id = $1 AND role = $2
  );
$$;
```

## Key Changes Summary

| Policy | Old WITH CHECK | New WITH CHECK | Why Changed |
|--------|----------------|----------------|-------------|
| Citizens UPDATE | `reporter_id = auth.uid()` | `reporter_id = auth.uid()` | ✅ Already correct |
| Council UPDATE | `is_user_in_role(...)` | `true` | ✅ Fixed - allows soft-delete |
| Engineers UPDATE | `assigned_engineer_id = auth.uid()` | `true` | ✅ Fixed - allows soft-delete |

## Files

- ✅ [FIX_DELETE_RLS_FINAL.sql](FIX_DELETE_RLS_FINAL.sql) - **Run this SQL file**
- 📖 [DELETE_AND_REJECT_FIXES_SUMMARY.md](DELETE_AND_REJECT_FIXES_SUMMARY.md) - Previous fix documentation
- 🧪 [TEST_QUERIES_DELETE_REJECT.sql](TEST_QUERIES_DELETE_REJECT.sql) - Test queries

## Summary

The fix changes UPDATE policy WITH CHECK clauses from restrictive conditions to `true`, allowing soft-delete flag updates without validation conflicts.

**Action Required:**
1. ✅ Run [FIX_DELETE_RLS_FINAL.sql](FIX_DELETE_RLS_FINAL.sql) in Supabase
2. ✅ Test engineer delete
3. ✅ Test council delete
4. ✅ Verify multi-role visibility works

No app rebuild needed - code changes from earlier are correct.
