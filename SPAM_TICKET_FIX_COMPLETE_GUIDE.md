# SPAM Ticket Flow Fix - Complete Implementation Guide

## Problem Statement
When a ticket is marked as SPAM by an engineer after the council assigned it, the flow wasn't working correctly across all three user roles:
- ✅ Engineer should see: Status = "SPAM", Engineer's Response = "Marked As Spam"
- ✅ Council should see: Status = "SPAM", Engineer's Response = "Marked As Spam" 
- ✅ Citizen should see: Status = "Rejected", Reason = "Marked As Spam"

## Root Causes Identified

### 1. Java Code Issues
- `engineerProcessTicket()` was setting `status = "Spam"` (lowercase) instead of `"SPAM"`
- When action = SPAM, it wasn't setting `engineer_notes` field at all
- Citizen view was using generic `getStatusDisplayText()` which showed "SPAM" instead of "Rejected"

### 2. Display Logic Issues  
- Citizen view was hiding the reason field when status = SPAM
- No differentiation between how SPAM status should appear to citizens vs other roles

## Solutions Implemented

### 1. Java Changes - TicketRepository.java
**File:** `app/src/main/java/com/example/infrastructureproject/TicketRepository.java`

**Changes:**
```java
// In engineerProcessTicket() method:

// OLD CODE:
case "SPAM": statusValue = "Spam"; break;  // Wrong case
// ...
if (reason != null && !reason.isEmpty()) {
    updateData.put("engineer_notes", reason);  // SPAM had null reason
}

// NEW CODE:
case "SPAM": statusValue = "SPAM"; break;  // Uppercase to match trigger

// For SPAM, always set engineer_notes to "Marked As Spam"
// For ACCEPTED/REJECTED, use the provided reason
if (actionType.equals("SPAM")) {
    updateData.put("engineer_notes", "Marked As Spam");
} else if (reason != null && !reason.isEmpty()) {
    updateData.put("engineer_notes", reason);
}
```

**What this fixes:**
- ✅ Ensures status is stored as `"SPAM"` (uppercase) in database
- ✅ Always sets `engineer_notes = "Marked As Spam"` when engineer clicks SPAM button
- ✅ Database trigger can now properly detect and handle SPAM tickets

---

### 2. Java Changes - Ticket.java
**File:** `app/src/main/java/com/example/infrastructureproject/Ticket.java`

**Changes:**
```java
// ADDED NEW METHOD for citizen-specific status display:
public String getStatusDisplayTextForCitizen() {
    if (status == null) return "Pending";
    switch (status) {
        case ACCEPTED:
            return "Completed";
        case REJECTED:
        case SPAM: // Citizens see SPAM as REJECTED
            return "Rejected";
        case UNDER_REVIEW:
            return "Under Review";
        case PENDING:
        default:
            return "Pending";
    }
}
```

**What this fixes:**
- ✅ Citizens now see "Rejected" instead of "SPAM" for spam tickets
- ✅ Maintains proper status display for other roles (engineers/council see "SPAM")
- ✅ Follows business logic: citizens shouldn't see "SPAM" status

---

### 3. Java Changes - TicketDetailActivity.java
**File:** `app/src/main/java/com/example/infrastructureproject/TicketDetailActivity.java`

**Changes:**

**A. Status Display (Line ~143):**
```java
// OLD CODE:
String statusText = ticket.getStatus().toString();

// NEW CODE:  
String statusText = ticket.getStatusDisplayTextForCitizen();
```

**B. Reason Field Display (Line ~437):**
```java
// OLD CODE:
// Show reason if exists (for accepted/rejected tickets, but not SPAM)
if (ticket != null && ticket.getReason() != null && !ticket.getReason().isEmpty() 
    && !ticket.getReason().equalsIgnoreCase("null") 
    && ticket.getStatus() != Ticket.TicketStatus.SPAM) {  // <-- Excluded SPAM!
    
// NEW CODE:
// Show reason if exists (for accepted/rejected/spam tickets in citizen view)
if (ticket != null && ticket.getReason() != null && !ticket.getReason().isEmpty() 
    && !ticket.getReason().equalsIgnoreCase("null")) {  // <-- Now includes SPAM!
    if (labelReason != null) {
        labelReason.setText("Reason");  // Changed from "Engineer's Reason"
```

**What this fixes:**
- ✅ Citizens see "Rejected" status for SPAM tickets
- ✅ Citizens see the reason "Marked As Spam" (no longer hidden)
- ✅ Consistent user experience

---

### 4. Java Changes - TicketAdapter.java
**File:** `app/src/main/java/com/example/infrastructureproject/TicketAdapter.java`

**Changes:**
```java
// OLD CODE:
String statusText = isCouncilMode 
    ? "Status: " + ticket.getStatusDisplayTextForCouncil()
    : "Status: " + ticket.getStatusDisplayText();  // Generic method

// NEW CODE:
String statusText;
if (isCouncilMode) {
    statusText = "Status: " + ticket.getStatusDisplayTextForCouncil();
} else {
    // Citizen mode - SPAM tickets show as "Rejected"
    statusText = "Status: " + ticket.getStatusDisplayTextForCitizen();
}
```

**What this fixes:**
- ✅ Ticket cards in citizen dashboard show "Rejected" for SPAM tickets
- ✅ Consistent with detail view

---

### 5. SQL Changes - Database Trigger
**File:** `ENGINEER_SPAM_FIX.sql` (NEW FILE)

**Purpose:** Comprehensive database trigger to handle SPAM tickets correctly

**Key Features:**
1. Detects when status is set to 'SPAM' (case-insensitive)
2. Sets `is_spam = true` flag
3. Normalizes status to uppercase 'SPAM'
4. Ensures `engineer_notes` defaults to 'Marked As Spam' if empty
5. Records action in `ticket_actions` table
6. Updates status constraint to allow 'SPAM' value

**SQL Trigger Logic:**
```sql
CREATE OR REPLACE FUNCTION public.handle_spam_tickets()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  -- Case 1: Engineer marks as SPAM (status = 'SPAM')
  IF NEW.status IS NOT NULL AND upper(NEW.status) = 'SPAM' THEN
    NEW.is_spam := true;
    NEW.status := 'SPAM';
    
    -- Ensure engineer_notes contains standard text
    IF NEW.engineer_notes IS NULL OR trim(NEW.engineer_notes) = '' THEN
      NEW.engineer_notes := 'Marked As Spam';
    END IF;
    
    -- Record action in ticket_actions
    INSERT INTO public.ticket_actions (ticket_id, created_by, action_type, reason)
    VALUES (COALESCE(NEW.id, OLD.id), NEW.assigned_engineer_id, 'SPAM', NEW.engineer_notes);
  END IF;
  
  RETURN NEW;
END;
$$;
```

**What this fixes:**
- ✅ Guarantees `engineer_notes = "Marked As Spam"` even if Java code fails
- ✅ Maintains data consistency
- ✅ Provides audit trail in `ticket_actions` table
- ✅ Works for both new and existing tickets

---

## Complete Flow After Fixes

### Scenario: Engineer Marks Ticket as SPAM

**1. User Action:**
- Engineer views assigned ticket in detail view
- Clicks "SPAM" button

**2. Java Processing (TicketRepository.java):**
```
engineerProcessTicket(ticketDbId, engineerId, "SPAM", null, callback)
↓
Updates database:
- status = "SPAM"
- engineer_notes = "Marked As Spam"  // ✅ NOW SET
```

**3. Database Trigger (handle_spam_tickets):**
```
BEFORE UPDATE trigger fires
↓
Detects: NEW.status = 'SPAM'
↓
Sets:
- is_spam = true
- status = 'SPAM' (normalized)
- engineer_notes = 'Marked As Spam' (if empty)
↓
Inserts ticket_action record:
- action_type = 'SPAM'
- reason = 'Marked As Spam'
```

**4. View Display:**

**Engineer sees (in their dashboard):**
- ✅ Status: SPAM
- ✅ Engineer's Response: Marked As Spam (in engineer detail view)

**Council sees (in their dashboard):**
- ✅ Status: SPAM (appears in Spam tab)
- ✅ Engineer's Response: Marked As Spam (in council notes section)

**Citizen sees (in their dashboard):**
- ✅ Status: Rejected (NOT "SPAM")  // getStatusDisplayTextForCitizen()
- ✅ Reason: Marked As Spam // Now visible, not hidden

---

## Testing Checklist

### Test 1: Engineer Marks Ticket as SPAM
1. ✅ Login as engineer
2. ✅ Open assigned ticket detail
3. ✅ Click "SPAM" button
4. ✅ Verify success message appears
5. ✅ Go back to dashboard - ticket should appear in Spam(1) tab

### Test 2: Engineer View Shows Correct Data
1. ✅ In engineer dashboard, click Spam(1) tab
2. ✅ Click on the SPAM ticket
3. ✅ Verify detail view shows:
   - Status badge or indication = "SPAM"
   - If there's a reason/response field, it shows "Marked As Spam"

### Test 3: Council View Shows Correct Data
1. ✅ Login as council
2. ✅ Navigate to the same ticket
3. ✅ Verify:
   - Status shows "⚠ SPAM" or similar
   - Council Notes / Engineer's Response section shows "Engineer's Response: Marked As Spam"

### Test 4: Citizen View Shows Rejected Status
1. ✅ Login as the citizen who reported the ticket
2. ✅ View ticket in dashboard
3. ✅ Verify:
   - Status shows "Rejected" (NOT "SPAM")
   - Reason field shows "Marked As Spam"
4. ✅ Click into ticket detail
5. ✅ Verify same display: Status = "Rejected", Reason = "Marked As Spam"

### Test 5: Database Verification
Run in Supabase SQL Editor:
```sql
-- Check the ticket was properly updated
SELECT 
  ticket_id,
  status,
  is_spam,
  engineer_notes,
  assigned_engineer_id
FROM tickets
WHERE ticket_id = 'T1234567890';  -- Replace with actual ticket ID

-- Expected results:
-- status = 'SPAM'
-- is_spam = true
-- engineer_notes = 'Marked As Spam'
```

---

## Files Modified

### Java Files (4 files):
1. ✅ `app/src/main/java/com/example/infrastructureproject/TicketRepository.java`
2. ✅ `app/src/main/java/com/example/infrastructureproject/Ticket.java`
3. ✅ `app/src/main/java/com/example/infrastructureproject/TicketDetailActivity.java`
4. ✅ `app/src/main/java/com/example/infrastructureproject/TicketAdapter.java`

### SQL Files (1 new file):
5. ✅ `ENGINEER_SPAM_FIX.sql` (NEW - Run in Supabase SQL Editor)

---

## Installation Steps

### Step 1: Apply SQL Changes
1. Open Supabase Dashboard
2. Go to SQL Editor
3. Run the script: `ENGINEER_SPAM_FIX.sql`
4. Verify no errors

### Step 2: Build and Deploy Android App
```bash
./gradlew clean
./gradlew build
```

### Step 3: Test the Flow
Follow the Testing Checklist above

---

## Troubleshooting

### Issue: Citizen still sees "SPAM" status
**Cause:** Old code still being used
**Fix:** 
1. Force stop the app
2. Clear app data
3. Rebuild and reinstall
4. Check if `getStatusDisplayTextForCitizen()` is being called

### Issue: Engineer's Response not showing "Marked As Spam"
**Cause:** Database trigger not running or engineer_notes not being set
**Fix:**
1. Verify SQL trigger is installed:
```sql
SELECT tgname, tgtype, tgenabled 
FROM pg_trigger 
WHERE tgrelid = 'public.tickets'::regclass;
```
2. Check if engineer_notes column exists:
```sql
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'tickets' AND column_name = 'engineer_notes';
```

### Issue: Ticket not appearing in engineer's Spam tab
**Cause:** RLS policy filtering out SPAM tickets
**Fix:** Check RLS policies - engineers SHOULD NOT see SPAM tickets in pending, but may have a separate Spam tab. If Spam tab is empty, verify:
1. Dashboard code correctly filters by status = SPAM
2. RLS policy allows engineers to view their SPAM tickets

---

## Summary

This comprehensive fix ensures that when an engineer marks a ticket as SPAM:

1. **Engineer Experience:**
   - ✅ Sees status = "SPAM"
   - ✅ Sees engineer's response = "Marked As Spam"

2. **Council Experience:**
   - ✅ Sees status = "SPAM" (in Spam tab)
   - ✅ Sees engineer's response = "Marked As Spam"

3. **Citizen Experience:**
   - ✅ Sees status = "Rejected" (user-friendly, not "SPAM")
   - ✅ Sees reason = "Marked As Spam"

All three roles now have the correct view of spam tickets, maintaining data consistency while presenting appropriate information to each user type.

---

## Date Implemented
January 5, 2026

## Modified By
GitHub Copilot (AI Assistant)
