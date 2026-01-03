## URGENT FIX: PGRST204 Error When Deleting Tickets

### What's Happening:
Your app shows: **"Error: Error: HTTP 400: {"code":"PGRST204"...}"**

**PGRST204** means: "UPDATE affected 0 rows" - The database rejected the update.

---

## IMMEDIATE ACTIONS (Do These In Order):

### 1. Run the Updated SQL Script
Open Supabase SQL Editor and run **SPAM_AND_DELETE_FIXES.sql** (the entire file).

This will create **UPDATE policies** that were missing.

### 2. Verify Policies Were Created
In Supabase SQL Editor, run:
```sql
SELECT policyname, cmd
FROM pg_policies
WHERE tablename = 'tickets'
AND cmd = 'UPDATE';
```

**You MUST see these 3 policies:**
- Citizens can soft delete own tickets
- Council can soft delete tickets  
- Engineers can soft delete assigned tickets

**If you see 0 rows**, the policies weren't created. Check for errors in Step 1.

### 3. Run Diagnostics
Run **DIAGNOSE_DELETE_ERROR.sql** (Steps 1-4) to check:
- ✅ Column exists
- ✅ RLS is enabled
- ✅ Policies exist
- ✅ Your user role is 'citizen'

### 4. Check Android Logcat
1. Open Android Studio → Logcat
2. Filter by: `TicketRepository`
3. Try to delete a ticket
4. Look for these log lines:
   ```
   === SOFT DELETE DEBUG ===
   Ticket DB ID: <some UUID>
   Access Token: EXISTS
   User ID: <your user UUID>
   URL: https://...
   HTTP Response Code: 200 or 400
   ```

5. **Take a screenshot** of the Logcat output and send it to me

### 5. Check Your User Profile
In Supabase SQL Editor (while NOT logged in to the app):
```sql
SELECT id, email, full_name, role
FROM profiles;
```

Find your citizen account and verify the **role** column says **'citizen'** (not NULL, not 'user', not empty).

---

## Most Likely Causes (Based on PGRST204):

### Cause 1: No UPDATE Policy (90% chance)
**Symptom:** PGRST204 error  
**Fix:** Run SPAM_AND_DELETE_FIXES.sql

### Cause 2: Wrong User Role (5% chance)
**Symptom:** Policy exists but still PGRST204  
**Fix:** 
```sql
UPDATE profiles 
SET role = 'citizen' 
WHERE email = 'your-email@example.com';
```

### Cause 3: Wrong Ticket ID Format (5% chance)
**Symptom:** Logcat shows ticket_id like "T1767385960688617" instead of UUID  
**Fix:** Check if `db_id` is being passed correctly from CitizenDashboardActivity

---

## After You Complete Steps 1-5:

Send me:
1. Screenshot of Logcat showing "=== SOFT DELETE DEBUG ===" section
2. Result of the policies query (Step 2)
3. Your user's role from profiles table (Step 5)

Then I can pinpoint the exact issue!

---

## Why This Happens:

Supabase checks: "Can this user UPDATE this ticket?"

Without an UPDATE policy, Supabase says "No" and returns PGRST204 (0 rows updated).

The fix adds a policy that says: "Citizens can UPDATE deleted_by_citizen column on their own tickets."
