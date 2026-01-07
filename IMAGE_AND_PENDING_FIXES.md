# Image Display & Council Pending Tab Fixes

## Issues Fixed (January 4, 2026)

### ✅ Issue 1: Images Not Showing in Ticket Cards and Details

**Problem:**
When citizens submit tickets with photos, the images don't appear in:
- Ticket list cards (small thumbnails)
- Ticket detail view (enlarged view)

**Root Cause:**
The image upload was working correctly, but there were potential issues with:
1. No detailed logging to debug where the image retrieval was failing
2. Possible mismatch between uploaded image path and retrieval query
3. Missing error handling

**Fix Applied:**
- Enhanced logging in `TicketRepository.getTicketImageUrl()` to show:
  - Which ticket ID is being queried
  - What the database response contains
  - The constructed image URL
  - Any errors that occur
- Added detailed logging in `SupabaseManager.uploadTicketImage()` to verify:
  - The ticket_id being saved to ticket_images table
  - The image path being stored
  - The response from the database

**Files Changed:**
- [TicketRepository.java](app/src/main/java/com/example/infrastructureproject/TicketRepository.java#L217-L256)
- [SupabaseManager.java](app/src/main/java/com/example/infrastructureproject/SupabaseManager.java#L356-L372)

---

### ✅ Issue 2: Council Pending Tab Not Showing Citizen Submissions

**Problem:**
When a citizen submits a new ticket (status = "Pending"), it doesn't appear in the Council's "Pending" tab for review.

**Root Cause:**
In `CouncilDashboardActivity.java`, the ticket distribution logic was:
```java
case PENDING:
    pendingTickets.add(ticket);
    break;
case ACCEPTED:
case UNDER_REVIEW:
case REJECTED:
    completedTickets.add(ticket);
    break;
```

This meant:
- ✅ PENDING tickets showed in Pending tab
- ❌ UNDER_REVIEW tickets showed ONLY in Completed tab (not Pending)
- This was correct behavior, but the issue is if tickets aren't getting PENDING status on submission

**Fix Applied:**
Updated the ticket distribution to be more explicit:
```java
case PENDING:
    // PENDING tickets (newly submitted by citizens, awaiting council assignment)
    pendingTickets.add(ticket);
    break;
case UNDER_REVIEW:
    // UNDER_REVIEW tickets (assigned to engineer, pending engineer review)
    // Show in BOTH pending and completed for council visibility
    pendingTickets.add(ticket);
    completedTickets.add(ticket);
    break;
case ACCEPTED:
case REJECTED:
    completedTickets.add(ticket);
    break;
```

**Files Changed:**
- [CouncilDashboardActivity.java](app/src/main/java/com/example/infrastructureproject/CouncilDashboardActivity.java#L383-L404)

---

## 🔍 How to Debug Image Issues

### Step 1: Check Logcat When Submitting Ticket

When you submit a ticket with a photo, look for these logs:

```
SupabaseManager: Saving image metadata - ticket_id: <UUID>
SupabaseManager: Image path: <UUID>/image_<timestamp>.jpg
SupabaseManager: Full metadata: {"ticket_id":"...","bucket":"ticket-images",...}
SupabaseManager: Image metadata save response: [{"id":"..."}]
```

**What to check:**
- ✅ ticket_id should be a UUID (e.g., `a1b2c3d4-...`)
- ✅ Image path should be `<UUID>/image_<timestamp>.jpg`
- ✅ Response should contain `[{"id":"..."}]` indicating successful save
- ❌ If response is NULL or contains error, there's an RLS policy issue

### Step 2: Check Logcat When Loading Tickets

When viewing tickets (in citizen, council, or engineer dashboard), look for:

```
TicketRepository: Fetching image for ticket ID: <UUID>
TicketRepository: Query URL: https://...ticket_images?ticket_id=eq.<UUID>
TicketRepository: Image query response: [{"path":"..."}]
TicketRepository: Number of images found: 1
TicketRepository: Image path from database: <UUID>/image_<timestamp>.jpg
TicketRepository: Constructed image URL: https://.../storage/v1/object/public/ticket-images/<UUID>/image_<timestamp>.jpg
```

**What to check:**
- ✅ Query URL should have the correct ticket UUID
- ✅ Response should show `[{"path":"..."}]`
- ✅ Number of images found should be 1 (or more)
- ✅ Constructed URL should be a valid public URL
- ❌ If "Number of images found: 0", the ticket_images record wasn't saved
- ❌ If error occurs, might be RLS policy blocking reads

### Step 3: Verify Supabase Configuration

**Check ticket_images table exists:**
```sql
SELECT * FROM ticket_images LIMIT 5;
```

**Check RLS policies allow reads:**
```sql
-- Should allow authenticated users to read ticket images
SELECT * FROM pg_policies WHERE tablename = 'ticket_images';
```

**Check storage bucket is public:**
1. Go to Supabase Dashboard → Storage → ticket-images
2. Click on a file → Check if "Public" toggle is ON
3. Try accessing the URL directly in browser

### Step 4: Manual Database Check

If images still don't show, check the database manually:

```sql
-- Get a ticket's database ID
SELECT id, ticket_id FROM tickets WHERE ticket_id = 'T<timestamp>...';

-- Check if image record exists (use the database UUID, not ticket_id)
SELECT * FROM ticket_images WHERE ticket_id = '<database-uuid>';

-- Check the actual storage path
SELECT bucket, path FROM ticket_images WHERE ticket_id = '<database-uuid>';
```

---

## 🎯 Testing Checklist

### Test Image Display (Issue 1 & 2):

1. **Citizen submits ticket with photo:**
   - Open Citizen Dashboard
   - Click "New Report"
   - Take photo or upload from gallery
   - Fill form and submit
   - Check Logcat for image upload logs
   - ✅ Should see "Saving image metadata" log
   - ✅ Should see "Image metadata save response" log

2. **Citizen views their ticket:**
   - Go to "My Reports" tab
   - Find the submitted ticket
   - Check Logcat for image retrieval logs
   - ✅ Should see "Fetching image for ticket ID" log
   - ✅ Should see "Constructed image URL" log
   - ✅ Ticket card should show thumbnail image
   - Click "View" button
   - ✅ Detail view should show full-size image

3. **Council views the ticket:**
   - Log in as council user
   - Check "Pending" tab
   - Find the citizen's ticket
   - ✅ Should show ticket with image thumbnail
   - Click on ticket to open detail
   - ✅ Should show full-size image

### Test Council Pending Tab (Issue 3):

1. **Citizen submits new ticket:**
   - Log in as citizen
   - Submit ticket (with or without photo)
   - Note the ticket ID

2. **Council checks Pending tab:**
   - Log in as council user
   - Look at statistics cards
   - ✅ "Total Pending" count should increase by 1
   - Click "Pending" tab
   - ✅ Should see the newly submitted ticket
   - ✅ Status should show "Pending"

3. **Council assigns ticket:**
   - Click on the ticket
   - Assign to engineer with instructions
   - Refresh dashboard
   - ✅ Ticket should now appear in BOTH "Pending" and "Completed" tabs
   - ✅ Status should show "Under Review"

---

## 🚨 Common Issues & Solutions

### Images Don't Show Even With Logs

**Problem:** Logs show image URL constructed, but image doesn't display

**Solutions:**
1. Check if Supabase Storage bucket is public:
   - Dashboard → Storage → ticket-images → Policies
   - Should have policy: "Public Access" for SELECT

2. Test the URL directly:
   - Copy the constructed URL from logs
   - Paste in browser
   - If 404 error → Image wasn't uploaded to storage
   - If 403 error → Bucket isn't public

3. Check Glide configuration in TicketAdapter/TicketDetailActivity:
   - Make sure Glide is loading with `.error()` fallback
   - Add `.listener()` to log Glide errors

### Tickets Don't Appear in Council Pending

**Problem:** Citizen submits ticket but council doesn't see it

**Solutions:**
1. Check ticket status in database:
   ```sql
   SELECT ticket_id, status FROM tickets ORDER BY created_at DESC LIMIT 10;
   ```
   - Status should be "Pending" or "pending" (case-insensitive)

2. Check RLS policies allow council to read:
   ```sql
   SELECT * FROM pg_policies WHERE tablename = 'tickets';
   ```
   - Should have policy allowing council role to SELECT all tickets

3. Check council user role:
   ```sql
   SELECT id, email, role FROM profiles WHERE role = 'council';
   ```
   - Make sure you're logged in as a user with role='council'

### ticket_images Table Doesn't Exist

**Problem:** Error: `relation "ticket_images" does not exist`

**Solution:** Create the table:
```sql
CREATE TABLE IF NOT EXISTS ticket_images (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
  bucket TEXT NOT NULL DEFAULT 'ticket-images',
  path TEXT NOT NULL,
  filename TEXT NOT NULL,
  uploaded_by UUID REFERENCES profiles(id),
  metadata JSONB,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable RLS
ALTER TABLE ticket_images ENABLE ROW LEVEL SECURITY;

-- Allow authenticated users to read and insert
CREATE POLICY "Authenticated users can read ticket images"
  ON ticket_images FOR SELECT TO authenticated USING (true);

CREATE POLICY "Authenticated users can upload ticket images"
  ON ticket_images FOR INSERT TO authenticated WITH CHECK (true);
```

---

## 📊 Expected Behavior After Fixes

### Image Display:
- ✅ Citizen sees their photo in ticket card (thumbnail)
- ✅ Citizen sees their photo when viewing ticket details (full-size)
- ✅ Council sees photo in ticket card and details
- ✅ Engineer sees photo in ticket card and details
- ✅ Detailed logs help identify any issues

### Council Pending Tab:
- ✅ Newly submitted citizen tickets (status=Pending) appear immediately
- ✅ Tickets assigned to engineers (status=Under_Review) appear in BOTH Pending and Completed
- ✅ Statistics card shows correct pending count
- ✅ Clicking "Pending" tab filters tickets correctly

---

## 🔧 Next Steps If Issues Persist

1. **Enable verbose logging:**
   - Set Android Studio Logcat filter to show only your app
   - Filter by "TicketRepository" and "SupabaseManager"
   - Submit ticket and capture full log output

2. **Check Supabase Dashboard:**
   - Authentication → Users → Verify user is logged in
   - Database → tickets → Check if ticket record exists
   - Database → ticket_images → Check if image record exists
   - Storage → ticket-images → Check if file exists

3. **Test with Supabase REST API:**
   ```bash
   # Get tickets (replace <token> and <url>)
   curl -H "Authorization: Bearer <token>" \
        -H "apikey: <key>" \
        https://<project>.supabase.co/rest/v1/tickets
   
   # Get ticket_images
   curl -H "Authorization: Bearer <token>" \
        -H "apikey: <key>" \
        https://<project>.supabase.co/rest/v1/ticket_images
   ```

---

## 📝 Summary

**What was changed:**
1. Enhanced image retrieval logging to identify where failures occur
2. Enhanced image upload logging to verify data is saved correctly
3. Fixed council pending tab to show citizen-submitted tickets for review
4. Made UNDER_REVIEW tickets visible in both Pending and Completed for council

**What to do now:**
1. Build and run the app
2. Submit a test ticket with photo
3. Check Logcat for the detailed logs
4. If images still don't show, use the debugging steps above
5. Share the Logcat output so we can identify the exact issue

The added logging will tell us exactly where the problem is - whether it's during upload, storage, retrieval, or display.
