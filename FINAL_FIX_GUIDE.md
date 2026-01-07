## PGRST204 Error - Final Diagnosis & Fix

### Current Situation:
- ✅ UPDATE policy exists: "Users can update own tickets"  
- ❌ Still getting PGRST204 error
- **Reason:** The existing policy is too restrictive or has wrong conditions

---

## The Fix (Do This Now):

### 1. Run This SQL In Supabase SQL Editor:

**Open:** [FIX_UPDATE_POLICY.sql](FIX_UPDATE_POLICY.sql)

This will:
- Drop the old "Users can update own tickets" policy
- Create 3 new specific policies with correct permissions
- Citizens can update their tickets (including deleted_by_citizen column)

### 2. After Running The SQL, Verify:

```sql
SELECT policyname, cmd
FROM pg_policies
WHERE tablename = 'tickets'
AND cmd = 'UPDATE';
```

**You should now see 3 policies:**
- Citizens can soft delete own tickets
- Council can update tickets
- Engineers can update assigned tickets

### 3. Test In Supabase First:

```sql
-- Get your user ID (while logged in as citizen in Supabase)
SELECT auth.uid();

-- Find one of your tickets
SELECT id, ticket_id, reporter_id, deleted_by_citizen
FROM tickets
WHERE reporter_id = auth.uid()
LIMIT 1;

-- Try updating it manually (copy the 'id' UUID from above)
UPDATE tickets 
SET deleted_by_citizen = TRUE 
WHERE id = 'paste-the-UUID-here';
```

**If this UPDATE succeeds** → The database is fixed! Try the app again.
**If this UPDATE fails** → Check Step 4 below.

### 4. If Still Failing - Check Your Profile:

```sql
SELECT id, email, full_name, role 
FROM profiles 
WHERE id = auth.uid();
```

**The `role` column MUST be 'citizen'** (not NULL, not empty, not 'user')

If it's wrong, fix it:
```sql
UPDATE profiles 
SET role = 'citizen' 
WHERE id = auth.uid();
```

---

## Why The Old Policy Didn't Work:

The old "Users can update own tickets" policy probably:
1. Didn't check the user's role in profiles table
2. Had a WITH CHECK condition that blocked the update
3. Used wrong comparison for reporter_id vs auth.uid()

The new policies explicitly:
- ✅ Check user role = 'citizen'
- ✅ Allow citizens to update their own tickets (reporter_id = auth.uid())
- ✅ Use proper UUID casting (reporter_id::uuid)

---

## After The Fix:

1. **Rebuild your Android app** (just to be safe)
2. **Try deleting a ticket** in the app
3. **Check Logcat** - You should see:
   ```
   HTTP Response Code: 200
   Ticket soft-deleted for citizen
   ```

If you still get errors after running FIX_UPDATE_POLICY.sql, send me:
- Screenshot of the 3 policies verification query
- Screenshot of your user profile (id, email, role)
- Full Logcat output including the "=== SOFT DELETE DEBUG ===" section
