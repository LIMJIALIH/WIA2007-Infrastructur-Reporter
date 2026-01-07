# SPAM Status and Soft Delete Implementation - Summary

## Overview
This implementation fixes four key issues:
1. SPAM status shows correctly across all roles with reason = null
2. Soft delete functionality - tickets hidden per user role, not actually deleted
3. Citizen delete functionality now works properly
4. Status display shows "Rejected" correctly (not "SPAM") in ticket cards

## SQL Changes (SPAM_AND_DELETE_FIXES.sql)

### New Database Columns
Added to `tickets` table:
- `deleted_by_citizen` (BOOLEAN, default FALSE)
- `deleted_by_council` (BOOLEAN, default FALSE)
- `deleted_by_engineer` (BOOLEAN, default FALSE)

### New Functions
1. **mark_ticket_as_spam(UUID)** - Sets status to SPAM with NULL reason
2. **soft_delete_ticket_for_citizen(UUID)** - Hides ticket from citizen view
3. **soft_delete_ticket_for_council(UUID)** - Hides ticket from council view
4. **soft_delete_ticket_for_engineer(UUID)** - Hides ticket from engineer view

### Database Trigger
**ensure_spam_null_reason** - Automatically sets reason = NULL when status = SPAM

### Updated RLS Policies
- Citizens see only their non-deleted tickets
- Council sees all non-deleted tickets (from council perspective)
- Engineers see assigned non-deleted tickets (from engineer perspective)

## Java Code Changes

### 1. Ticket.java
**Fixed:** `getStatusDisplayText()` method
- Now correctly returns "Rejected" for REJECTED status
- Returns "SPAM" only for SPAM status
- Previously both returned "SPAM"

### 2. TicketDetailActivity.java
**Changes:**
- Engineer SPAM button: Now passes `null` for reason instead of empty string
- Hides reason section when status is SPAM
- Fixed delete confirmation message
- Properly retrieves db_id from intent or ticket object for delete operations
- Shows correct status in citizen view

### 3. CouncilTicketDetailActivity.java
**Changes:**
- `showMarkAsSpamDialog()` now calls `TicketRepository.markTicketAsSpam()`
- Properly updates Supabase with SPAM status and null reason
- Updated delete confirmation message

### 4. TicketRepository.java
**New Methods:**
- `softDeleteTicketForEngineer(String ticketDbId, AssignTicketCallback callback)`
- `markTicketAsSpam(String ticketDbId, AssignTicketCallback callback)`

**Updated Methods:**
- `softDeleteTicketForCitizen()` - Already existed, now properly documented
- `softDeleteTicketForCouncil()` - Already existed, now properly documented

### 5. CitizenDashboardActivity.java
**Changes:**
- Now passes `db_id` in intent extras when opening ticket details
- This enables proper delete functionality

### 6. TicketsDialogFragment.java
**Changes:**
- Now passes `db_id` in intent extras when opening ticket details from dialog
- Ensures delete works from pending/accepted/rejected popups

## How It Works

### SPAM Status Flow

#### When Engineer marks as SPAM:
1. Engineer clicks SPAM button on TicketDetailActivity
2. Calls `TicketRepository.engineerProcessTicket()` with status="SPAM", reason=null
3. Updates Supabase: `status = "SPAM"`, `reason = NULL`
4. Database trigger ensures reason stays NULL
5. All roles (citizen, council, engineer) see: **Status: SPAM**, no reason displayed

#### When Council marks as SPAM:
1. Council clicks "Mark as Spam" button
2. Calls `TicketRepository.markTicketAsSpam()`
3. Updates Supabase: `status = "SPAM"`, `reason = NULL`
4. Database trigger ensures reason stays NULL
5. All roles see: **Status: SPAM**, no reason displayed

### Soft Delete Flow

#### When Citizen deletes ticket:
1. Citizen clicks delete button on completed ticket
2. Calls `TicketRepository.softDeleteTicketForCitizen(db_id)`
3. Updates Supabase: `deleted_by_citizen = TRUE`
4. RLS policy hides ticket from citizen's queries
5. **Citizen never sees it again** after re-login
6. **Council and Engineer still see it** (their flags are FALSE)

#### When Council deletes ticket:
1. Council clicks delete button
2. Calls `TicketRepository.softDeleteTicketForCouncil(db_id)`
3. Updates Supabase: `deleted_by_council = TRUE`
4. RLS policy hides ticket from council's queries
5. **Council never sees it again** after re-login
6. **Citizen and Engineer still see it** (their flags are FALSE)

#### When Engineer deletes ticket:
1. Engineer clicks delete button (if implemented in UI)
2. Calls `TicketRepository.softDeleteTicketForEngineer(db_id)`
3. Updates Supabase: `deleted_by_engineer = TRUE`
4. RLS policy hides ticket from engineer's queries
5. **Engineer never sees it again** after re-login
6. **Citizen and Council still see it** (their flags are FALSE)

### Status Display in Cards

**Before Fix:**
- REJECTED tickets showed as "SPAM" in the card view
- SPAM tickets also showed as "SPAM"
- Confusing for users

**After Fix:**
- REJECTED tickets show as "Rejected" ✓
- SPAM tickets show as "SPAM" ✓
- ACCEPTED tickets show as "Completed" ✓
- Clear distinction between statuses

## Database Setup Instructions

1. **Run the SQL file in Supabase SQL Editor:**
   ```sql
   -- Copy and paste SPAM_AND_DELETE_FIXES.sql content
   -- Execute in Supabase dashboard
   ```

2. **Verify setup:**
   - Check if columns were added (see verification queries at end of SQL file)
   - Check if functions exist
   - Check if trigger exists
   - Check if RLS policies are active

3. **Test the functionality:**
   - Mark tickets as SPAM from engineer/council
   - Delete tickets from citizen/council/engineer views
   - Re-login and verify tickets stay hidden

## Testing Checklist

### SPAM Status
- [ ] Engineer marks ticket as SPAM → Shows "Status: SPAM" to all roles
- [ ] Council marks ticket as SPAM → Shows "Status: SPAM" to all roles
- [ ] SPAM tickets show NO reason section
- [ ] REJECTED tickets show "Status: Rejected" with reason
- [ ] Status displays correctly in both card view and detail view

### Soft Delete
- [ ] Citizen deletes ticket → Can't see it after re-login, others can
- [ ] Council deletes ticket → Can't see it after re-login, others can
- [ ] Engineer deletes ticket → Can't see it after re-login, others can
- [ ] Same ticket can be deleted by all 3 roles independently
- [ ] Ticket still exists in database (check Supabase directly)

### Delete Button
- [ ] Citizen can delete completed tickets from "My Reports"
- [ ] Citizen can delete tickets from dialog popups
- [ ] Council can delete tickets from detail view
- [ ] Proper confirmation message shown

### Status Display
- [ ] Pending tickets show "Pending"
- [ ] Under Review tickets show "Under Review"
- [ ] Accepted tickets show "Completed"
- [ ] Rejected tickets show "Rejected" (NOT "SPAM")
- [ ] SPAM tickets show "SPAM"

## Key Benefits

1. **Data Preservation:** Tickets never actually deleted from database
2. **Audit Trail:** Can track who deleted what by checking boolean flags
3. **Independent Views:** Each role manages their own view without affecting others
4. **Proper SPAM Handling:** SPAM tickets correctly identified with null reasons
5. **Correct Status Display:** Users see accurate ticket statuses

## Notes

- The old TicketManager in-memory system is deprecated
- All operations now use Supabase via TicketRepository
- RLS policies ensure security and proper data visibility
- Database triggers enforce data integrity (SPAM always has null reason)
