# COMPREHENSIVE FIXES IMPLEMENTATION GUIDE

## Overview
This guide covers the complete implementation of three critical fixes:

1. **Council View Fix**: REJECTED tickets show as "COMPLETED" in council dashboard
2. **Delete Functionality**: Deleted tickets are completely hidden from respective role views
3. **SPAM Visibility Fix**: SPAM tickets don't appear in engineer's pending review list

---

## STEP 1: Run SQL Migration in Supabase

### Instructions:
1. Go to your Supabase project dashboard
2. Navigate to **SQL Editor**
3. Open the file: `COMPREHENSIVE_FIXES.sql`
4. Copy and paste the entire content into the SQL Editor
5. Click **Run** to execute the script

### What This Does:
- Adds three boolean columns: `deleted_by_citizen`, `deleted_by_council`, `deleted_by_engineer`
- Creates RLS policies that filter tickets per role:
  - **Citizens**: See only their own non-deleted tickets
  - **Council**: See all non-deleted tickets (from council perspective)
  - **Engineers**: See only assigned, non-deleted, **non-SPAM** tickets
- Creates soft delete functions for each role
- Creates SPAM marking function that sets `reason = NULL`
- Adds trigger to auto-enforce NULL reason for SPAM status

### Verification:
After running the script, execute the verification queries at the bottom to confirm:
```sql
-- Should show 3 new columns
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'tickets'
AND column_name IN ('deleted_by_citizen', 'deleted_by_council', 'deleted_by_engineer');

-- Should show 3 policies
SELECT schemaname, tablename, policyname
FROM pg_policies
WHERE tablename = 'tickets';

-- Should show 5 functions
SELECT routine_name
FROM information_schema.routines
WHERE routine_name IN (
  'mark_ticket_as_spam',
  'soft_delete_ticket_for_citizen',
  'soft_delete_ticket_for_council',
  'soft_delete_ticket_for_engineer',
  'enforce_spam_null_reason'
);
```

---

## STEP 2: Java Code Changes (Already Applied)

### Changes Made:

#### 1. **Ticket.java**
Added new method `getStatusDisplayTextForCouncil()`:
- ACCEPTED → "Completed"
- REJECTED → "Completed" (council sees this as completed work)
- SPAM → "SPAM"
- PENDING → "Pending"
- UNDER_REVIEW → "Under Review"

#### 2. **TicketAdapter.java**
- Added `isCouncilMode` parameter
- Updated constructor to accept council mode flag
- Modified status display logic to use `getStatusDisplayTextForCouncil()` when in council mode

#### 3. **CouncilDashboardActivity.java**
- Updated ticket categorization:
  - `PENDING` → pendingTickets
  - `ACCEPTED`, `UNDER_REVIEW`, `REJECTED` → completedTickets
  - `SPAM` → spamTickets (separate tab)
- Updated adapter initialization: `new TicketAdapter(this, this, false, true)`

#### 4. **EngineerDashboardActivity.java**
- Added safety check to filter out SPAM tickets
- SPAM tickets won't appear in any engineer tab (RLS handles this at DB level)
- Engineer categorization:
  - `PENDING`, `UNDER_REVIEW` → pendingTickets
  - `ACCEPTED` → acceptedTickets
  - `REJECTED` → rejectedTickets
  - `SPAM` → **skipped** (won't show)

---

## STEP 3: Rebuild Application

Run the following command in terminal:
```bash
./gradlew assembleDebug
```

---

## How It Works: Complete Flow

### Flow 1: Council Marks Ticket as SPAM
1. Council member opens ticket detail
2. Clicks "Mark as SPAM"
3. App calls `TicketRepository.markTicketAsSpam()`
4. Supabase function `mark_ticket_as_spam()` is invoked
5. Ticket status → `SPAM`, reason → `NULL`
6. Trigger `ensure_spam_null_reason` enforces NULL reason
7. RLS policy hides ticket from engineer's view
8. Council can still see it in "SPAM" tab

### Flow 2: Engineer Rejects Ticket
1. Engineer opens ticket detail
2. Enters rejection reason
3. Clicks "Reject"
4. Ticket status → `REJECTED`, reason → engineer's explanation
5. Council dashboard receives update
6. Ticket moves to "Completed" tab (REJECTED shown as "Completed")
7. Council can still see the rejection reason in ticket detail

### Flow 3: User Deletes Ticket
**Citizen deletes:**
1. Citizen clicks delete on their ticket
2. `softDeleteTicketForCitizen()` called
3. Sets `deleted_by_citizen = TRUE`
4. RLS policy filters it out from citizen's view
5. Council and engineer can still see it (if assigned)

**Council deletes:**
1. Council clicks delete
2. `softDeleteTicketForCouncil()` called
3. Sets `deleted_by_council = TRUE`
4. Hidden from council dashboard only

**Engineer deletes:**
1. Engineer clicks delete
2. `softDeleteTicketForEngineer()` called
3. Sets `deleted_by_engineer = TRUE`
4. Hidden from engineer dashboard only

---

## Testing Checklist

### Test 1: Council View of REJECTED Tickets
- [ ] Engineer rejects a ticket with reason
- [ ] Council dashboard refreshes
- [ ] REJECTED ticket appears in "Completed" tab
- [ ] Card shows "Status: Completed" (not "Status: Rejected")
- [ ] Opening ticket detail still shows rejection reason
- [ ] Ticket does NOT appear in "SPAM" tab

### Test 2: SPAM Tickets Hidden from Engineer
- [ ] Council marks ticket as SPAM
- [ ] Engineer dashboard refreshes
- [ ] SPAM ticket does NOT appear in "Pending Review" tab
- [ ] SPAM ticket does NOT appear in any engineer tab
- [ ] Council can still see it in "SPAM" tab
- [ ] Citizen can still see it (if not deleted)

### Test 3: Soft Delete - Citizen
- [ ] Citizen deletes their own ticket
- [ ] Ticket disappears from citizen dashboard
- [ ] Refresh citizen dashboard - ticket still gone
- [ ] Log out and log back in - ticket still gone
- [ ] Council can still see the ticket (not deleted for them)
- [ ] Engineer can still see the ticket (if assigned)

### Test 4: Soft Delete - Council
- [ ] Council deletes a ticket
- [ ] Ticket disappears from council dashboard
- [ ] Refresh council dashboard - ticket still gone
- [ ] Citizen can still see their ticket
- [ ] Engineer can still see the ticket (if assigned)

### Test 5: Soft Delete - Engineer
- [ ] Engineer deletes a completed ticket
- [ ] Ticket disappears from engineer dashboard
- [ ] Refresh engineer dashboard - ticket still gone
- [ ] Citizen can still see their ticket
- [ ] Council can still see the ticket

### Test 6: SPAM with NULL Reason
- [ ] Council marks ticket as SPAM (no reason entered)
- [ ] Check ticket detail in council view
- [ ] "Reason" section should be hidden or show "No reason provided"
- [ ] Database verification: `SELECT id, status, reason FROM tickets WHERE status = 'SPAM'`
- [ ] All SPAM tickets should have `reason = NULL`

---

## Database Verification Queries

Run these in Supabase SQL Editor to verify everything is working:

### Check Soft Delete Status
```sql
SELECT 
  id,
  status,
  deleted_by_citizen,
  deleted_by_council,
  deleted_by_engineer,
  created_at
FROM tickets
WHERE 
  deleted_by_citizen = TRUE 
  OR deleted_by_council = TRUE 
  OR deleted_by_engineer = TRUE
ORDER BY created_at DESC
LIMIT 10;
```

### Check SPAM Tickets (should have NULL reason)
```sql
SELECT 
  id,
  status,
  reason,
  created_at,
  updated_at
FROM tickets
WHERE status = 'SPAM'
ORDER BY updated_at DESC;
```

### Check Tickets Visible to Engineers (should exclude SPAM)
```sql
-- Simulate engineer view (replace 'engineer-uuid' with actual UUID)
SELECT 
  id,
  status,
  assigned_engineer_id
FROM tickets
WHERE 
  deleted_by_engineer = FALSE
  AND status != 'SPAM'
  AND (assigned_engineer_id = 'engineer-uuid' OR status = 'UNDER_REVIEW')
ORDER BY created_at DESC;
```

### Count Tickets by Category
```sql
SELECT 
  status,
  COUNT(*) as count,
  SUM(CASE WHEN deleted_by_citizen THEN 1 ELSE 0 END) as deleted_by_citizen,
  SUM(CASE WHEN deleted_by_council THEN 1 ELSE 0 END) as deleted_by_council,
  SUM(CASE WHEN deleted_by_engineer THEN 1 ELSE 0 END) as deleted_by_engineer
FROM tickets
GROUP BY status
ORDER BY status;
```

---

## Troubleshooting

### Issue: REJECTED tickets still showing in SPAM tab for council
**Solution**: Rebuild the app. The categorization logic was updated.

### Issue: Engineer can still see SPAM tickets
**Solution**: 
1. Verify RLS policy is active: Check `pg_policies` table
2. Make sure SQL migration ran successfully
3. Rebuild the app

### Issue: Deleted tickets reappear after refresh
**Solution**:
1. Verify soft delete function executed successfully
2. Check RLS policies are filtering correctly
3. Run verification query to see column values

### Issue: SPAM tickets still have reasons
**Solution**:
1. Verify trigger `ensure_spam_null_reason` exists
2. Re-run SQL migration
3. Update existing SPAM tickets: `UPDATE tickets SET reason = NULL WHERE status = 'SPAM';`

---

## Summary of Changes

| Component | Change | Purpose |
|-----------|--------|---------|
| **Database** | Added 3 delete columns | Track soft deletes per role |
| **RLS Policies** | 3 role-specific policies | Filter tickets per role visibility |
| **Functions** | 4 Supabase functions | Handle SPAM & soft deletes safely |
| **Trigger** | Auto NULL reason for SPAM | Data integrity enforcement |
| **Ticket.java** | Added council status method | Display REJECTED as COMPLETED |
| **TicketAdapter** | Added council mode | Use correct status display |
| **CouncilDashboard** | Updated categorization | REJECTED → completed, SPAM → spam tab |
| **EngineerDashboard** | Filter SPAM tickets | Safety check + RLS enforcement |

---

## Next Steps

1. ✅ Run `COMPREHENSIVE_FIXES.sql` in Supabase SQL Editor
2. ✅ Rebuild the application: `./gradlew assembleDebug`
3. ✅ Test all scenarios in the testing checklist
4. ✅ Verify database queries return expected results
5. ✅ Deploy to production when satisfied with testing

---

## Questions or Issues?

If you encounter any issues:
1. Check the verification queries in Supabase
2. Review the troubleshooting section
3. Verify all SQL functions and triggers were created
4. Confirm RLS policies are active
5. Ensure app was rebuilt after code changes
