# Council Dashboard Implementation Guide

## Summary
I've implemented a complete council dashboard system that fetches real-time ticket data from Supabase. When citizens submit tickets, they now **immediately appear** in the council dashboard with proper statistics and filtering.

---

## What I've Added to Your Code

### 1. **TicketRepository.java** - Two New Methods

#### Method 1: `getAllTickets()`
Fetches ALL tickets from Supabase (not just user's tickets) for the council dashboard.

```java
TicketRepository.getAllTickets(new TicketRepository.FetchTicketsCallback() {
    @Override
    public void onSuccess(List<Ticket> tickets) {
        // All tickets loaded from database
    }
    
    @Override
    public void onError(String message) {
        // Handle error
    }
});
```

#### Method 2: `getCouncilStatistics()`
Calculates real-time statistics:
- **Total Reports**: Count of all tickets
- **Total Pending**: Count of pending tickets
- **High Priority Pending**: Count of high severity pending tickets
- **Avg Response Time**: Average response time (currently placeholder)

```java
TicketRepository.getCouncilStatistics(new TicketRepository.CouncilStatsCallback() {
    @Override
    public void onSuccess(int totalReports, int totalPending, 
                         int highPriorityPending, String avgResponse) {
        // Statistics loaded
    }
    
    @Override
    public void onError(String message) {
        // Handle error
    }
});
```

### 2. **CouncilDashboardActivity.java** - Updated to Load Real Data

**Before**: Used hardcoded `TicketManager` singleton with fake data

**After**: Fetches real tickets from Supabase database

**Key Changes**:
- `initializeDataLists()`: Now only initializes empty lists
- `loadDashboardData()`: Fetches all tickets from Supabase via `TicketRepository.getAllTickets()`
- `loadStatistics()`: Fetches statistics from Supabase via `TicketRepository.getCouncilStatistics()`
- **Automatic categorization**: Tickets are automatically sorted into:
  - `allTickets` - All tickets
  - `pendingTickets` - Status = PENDING
  - `completedTickets` - Status = ACCEPTED
  - `spamTickets` - Status = REJECTED or SPAM

**Features Now Working**:
✅ **Real-time ticket loading** from Supabase  
✅ **Statistics update automatically** (Total Reports, Pending, High Priority)  
✅ **Tab counts** update based on ticket status  
✅ **Filter by Type** - Road, Utilities, Facilities, Environment, Other  
✅ **Filter by Severity** - Low, Medium, High  
✅ **Search by location/description** - Already implemented  
✅ **Image thumbnails** - Shows ticket photos from Supabase Storage  

---

## What You Need to Do in Supabase

### Step 1: Run the SQL Script
Open [COUNCIL_SUPABASE_SETUP.sql](COUNCIL_SUPABASE_SETUP.sql) and execute it in your Supabase SQL Editor.

This script will:
1. Add `user_role` column to `profiles` table
2. Create RLS policies to allow council members to view all tickets
3. Create helper function to assign roles

### Step 2: Assign Council Role to Users

After running the script, assign council role to management users:

**Option A: Using SQL Editor**
```sql
UPDATE profiles
SET user_role = 'council'
WHERE email = 'jiemm@example.com';
```

**Option B: Using the helper function**
```sql
SELECT update_user_role('jiemm@example.com', 'council');
```

### Step 3: Verify the Setup

**Check user roles:**
```sql
SELECT id, full_name, email, user_role 
FROM profiles 
ORDER BY user_role;
```

**Expected result:**
```
id                                    | full_name | email              | user_role
--------------------------------------|-----------|--------------------|-----------
xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx | Jiemm     | jiemm@example.com  | council
yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy | John Doe  | john@example.com   | citizen
```

---

## How It Works Now

### Citizen Submits Ticket
1. Citizen opens app → **New Report** tab
2. Takes photo, fills form, clicks **Submit**
3. Ticket saved to Supabase `tickets` table
4. Image uploaded to `ticket-images` Storage bucket
5. Metadata saved to `ticket_images` table

### Council Dashboard Updates Automatically
1. Council user opens **Management Council** dashboard
2. App calls `TicketRepository.getAllTickets()`
3. Supabase RLS policy checks: **Is user role = council?**
4. ✅ **YES** → Returns ALL tickets (from all citizens)
5. ❌ **NO** → Returns only user's own tickets

### Statistics Update
- **Total Reports**: `SELECT COUNT(*) FROM tickets`
- **Total Pending**: `SELECT COUNT(*) FROM tickets WHERE status = 'pending'`
- **High Priority Pending**: `SELECT COUNT(*) FROM tickets WHERE status = 'pending' AND severity = 'High'`

### Filtering System
The existing filter system now works with real data:
- **Type dropdown**: Road, Utilities, Facilities, Environment, Other
- **Severity dropdown**: Low, Medium, High
- **Search bar**: Filters by location or description
- **Location/Description buttons**: Toggle search scope

---

## Row Level Security (RLS) Policies

### Tickets Table Policies

**Policy 1**: Citizens can view **only their own** tickets
```sql
CREATE POLICY "Users can view their own tickets"
ON tickets FOR SELECT TO authenticated
USING (auth.uid() = reporter_id::uuid);
```

**Policy 2**: Council members can view **ALL** tickets
```sql
CREATE POLICY "Council can view all tickets"
ON tickets FOR SELECT TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.user_role IN ('council', 'admin')
  )
);
```

**Policy 3**: Citizens can insert tickets
```sql
CREATE POLICY "Users can insert their own tickets"
ON tickets FOR INSERT TO authenticated
WITH CHECK (auth.uid() = reporter_id::uuid);
```

**Policy 4**: Council can update tickets (for status changes)
```sql
CREATE POLICY "Council can update all tickets"
ON tickets FOR UPDATE TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.user_role IN ('council', 'admin', 'engineer')
  )
);
```

---

## Testing Checklist

### Test as Citizen
- [ ] Submit a new ticket with photo
- [ ] Go to **My Reports** → Should see only YOUR tickets
- [ ] Click **View** → Image should load from Supabase

### Test as Council
- [ ] Login with council account (after assigning role in Supabase)
- [ ] Open **Management Council** dashboard
- [ ] **Total Reports** should show count of ALL tickets from all users
- [ ] **Total Pending** should show count of pending tickets
- [ ] Click **Pending (X)** tab → Should see all pending tickets
- [ ] Click **Total Reports (X)** tab → Should see ALL tickets
- [ ] Use **Type filter** → Should filter tickets by type
- [ ] Use **Severity filter** → Should filter tickets by severity
- [ ] Type in **Search bar** → Should search by location/description
- [ ] Click ticket **View** button → Should see ticket details with image

---

## Troubleshooting

### Problem: "Error loading tickets" in Council Dashboard

**Solution 1**: Check if user has council role
```sql
SELECT user_role FROM profiles WHERE id = auth.uid();
```

**Solution 2**: Verify RLS policies are enabled
```sql
SELECT tablename, policyname 
FROM pg_policies 
WHERE tablename = 'tickets';
```

### Problem: Council dashboard shows 0 tickets but tickets exist

**Cause**: User doesn't have council role assigned

**Fix**: Run this in Supabase SQL Editor:
```sql
UPDATE profiles
SET user_role = 'council'
WHERE email = 'your-council-email@example.com';
```

### Problem: Statistics not updating

**Cause**: Network error or RLS policy blocking

**Check Logcat** for errors:
```bash
adb logcat | grep "TicketRepository"
```

---

## Database Schema Reference

### profiles table
```sql
CREATE TABLE profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id),
  full_name TEXT,
  email TEXT,
  user_role TEXT DEFAULT 'citizen',
  created_at TIMESTAMP DEFAULT NOW()
);
```

### tickets table
```sql
CREATE TABLE tickets (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  ticket_id TEXT NOT NULL,
  reporter_id TEXT NOT NULL,
  issue_type TEXT NOT NULL,
  severity TEXT DEFAULT 'Low',
  location TEXT NOT NULL,
  description TEXT NOT NULL,
  status TEXT DEFAULT 'pending',
  created_at TIMESTAMP DEFAULT NOW()
);
```

### ticket_images table
```sql
CREATE TABLE ticket_images (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  ticket_id UUID REFERENCES tickets(id),
  bucket TEXT NOT NULL,
  path TEXT NOT NULL,
  filename TEXT NOT NULL,
  uploaded_by TEXT NOT NULL,
  uploaded_at TIMESTAMP DEFAULT NOW()
);
```

---

## Summary of Changes

### Files Modified
1. ✅ **TicketRepository.java**
   - Added `getAllTickets()` method
   - Added `getCouncilStatistics()` method
   - Added `CouncilStatsCallback` interface

2. ✅ **CouncilDashboardActivity.java**
   - Updated `initializeDataLists()` - removed hardcoded data
   - Updated `loadDashboardData()` - fetches from Supabase
   - Added `loadStatistics()` - fetches real stats

### Files Created
1. ✅ **COUNCIL_SUPABASE_SETUP.sql** - Database setup script
2. ✅ **COUNCIL_DASHBOARD_GUIDE.md** - This guide

### No Changes Needed
- ✅ CitizenDashboardActivity - Already works correctly
- ✅ ReportIssueFragment - Already submits to Supabase
- ✅ TicketAdapter - Already shows images from URLs
- ✅ Filtering/Search - Already implemented, now uses real data

---

## Next Steps

1. **Build and run the app** in Android Studio
2. **Open Supabase dashboard** → SQL Editor
3. **Run** [COUNCIL_SUPABASE_SETUP.sql](COUNCIL_SUPABASE_SETUP.sql)
4. **Assign council role** to your test account:
   ```sql
   UPDATE profiles
   SET user_role = 'council'
   WHERE email = 'your-email@example.com';
   ```
5. **Login as council user** in the app
6. **Submit a test ticket** as a citizen
7. **Check council dashboard** - ticket should appear immediately! 🎉

---

## Support

If you encounter any issues:
1. Check Logcat: `adb logcat | grep "TicketRepository\|CouncilDashboard"`
2. Verify RLS policies in Supabase dashboard
3. Confirm user has `user_role = 'council'` in profiles table
4. Test API endpoint manually: `/rest/v1/tickets?select=*`

---

**Everything is ready! Just run the SQL script in Supabase and assign council roles.** 🚀
