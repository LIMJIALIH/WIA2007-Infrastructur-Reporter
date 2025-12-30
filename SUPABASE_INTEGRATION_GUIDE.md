# Supabase Integration Guide

## ✅ What's Done

I've created:
1. **Database schema** with `tickets` and `ticket_images` tables
2. **Android models** - `Ticket.java`
3. **Repository layer** - `TicketRepository.java` (handles API calls)
4. **Location helper** - `LocationHelper.java` (GPS + manual entry)
5. **Updated ReportIssueFragment** - now submits to Supabase

## 🔧 Steps YOU Need To Do

### Step 1: Get Your Supabase Credentials

1. Go to Supabase Dashboard → Your Project → **Settings** (gear icon) → **API**
2. Copy these two values:
   - **Project URL** (looks like `https://xxxxx.supabase.co`)
   - **anon public key** (long string starting with `eyJ...`)

### Step 2: Add Credentials to Your App

Open `app/src/main/java/com/example/infrastructureproject/SupabaseConfig.java` and replace:

```java
public static final String SUPABASE_URL = "https://your-project.supabase.co";
public static final String SUPABASE_ANON_KEY = "your-anon-key-here";
```

With your actual values from Step 1.

### Step 3: Get User Authentication Token

Your app needs to know which user is logged in. You need to:

**Option A: If you already have authentication:**
- Get the user's JWT token and UUID from your login system
- Update these lines in `ReportIssueFragment.java` (around line 75):
  ```java
  private String userToken = "YOUR_USER_TOKEN_HERE"; // Replace with actual token
  private String userId = "YOUR_USER_ID_HERE"; // Replace with actual user UUID
  ```

**Option B: If you don't have auth yet:**
- For testing, manually create a user in Supabase:
  1. Supabase Dashboard → **Authentication** → **Users** → **Add user**
  2. Add email/password
  3. Copy the user's `id` (UUID)
  4. Get a token by signing in via Supabase client or use the Service Role key temporarily (ONLY for testing!)

### Step 4: Create Storage Bucket

1. Supabase Dashboard → **Storage** → **New bucket**
2. Name: `tickets`
3. Privacy: **Private** (recommended)
4. Click **Save**

### Step 5: Set Storage Policies

After creating the bucket:

1. Click the `tickets` bucket → **Policies** tab
2. Click **"New Policy"** → **"For full customization"**

**Policy 1: Allow authenticated uploads**
- Name: `Allow authenticated uploads`
- Operation: `INSERT`
- Target roles: `authenticated`
- WITH CHECK: `(bucket_id = 'tickets')`

**Policy 2: Allow users to read**
- Name: `Allow users to read`
- Operation: `SELECT`
- Target roles: `authenticated`
- USING: `true`

### Step 6: Test the Database (Optional but Recommended)

Run this in Supabase **SQL Editor** to insert test data:

```sql
-- Check if tables exist
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('tickets', 'ticket_images', 'profiles');

-- Insert test ticket (make sure you have a profile first)
INSERT INTO public.tickets (reporter_id, issue_type, severity, location, description)
SELECT id, 'Pothole', 'High', 'Test Location', 'Test description'
FROM public.profiles LIMIT 1;

-- View result
SELECT * FROM public.tickets;
```

### Step 7: Sync Gradle and Build

1. Open Android Studio
2. Click **File** → **Sync Project with Gradle Files**
3. Wait for sync to complete
4. Click **Build** → **Make Project**
5. Fix any errors (should be minimal)

### Step 8: Test the Flow

1. Run your app on emulator/device
2. Go to "Report Issue" screen
3. Take/upload a photo
4. Select Issue Type and Severity
5. Click "Enter Manually" to set location (or grant GPS permission)
6. Click **Submit Report**
7. Check Supabase Dashboard → **Table Editor** → `tickets` to see your new row!

## 📊 Displaying Reports on Home Screen

To show the stats (Total Reports: 8, Pending: 4, etc.) from real data, you need to:

### Next Steps (I can help with this):

1. **Fetch ticket stats** - Query Supabase to count tickets by status
2. **Display in HomeFragment** - Update the UI cards with real numbers
3. **My Reports list** - Fetch and display user's submitted tickets
4. **Handle image display** - Get signed URLs from Storage to show images

Would you like me to implement the home screen data fetching next?

## 🐛 Troubleshooting

**"Failed to create ticket: 401"**
- Check your `userToken` is valid
- Make sure RLS policies are created

**"Failed to upload image: 403"**
- Check Storage bucket policies are set
- Verify bucket name is `tickets`

**"Location permission denied"**
- The app will use default location (Kuala Lumpur)
- User can still enter location manually

**Build errors about missing classes**
- Make sure you ran Gradle sync
- Check all dependencies are in `build.gradle.kts`

## 📝 Summary

Your submit flow now:
1. ✅ User fills form + uploads photo
2. ✅ App validates inputs
3. ✅ Creates `Ticket` record in Supabase
4. ✅ Uploads image to Storage bucket
5. ✅ Saves image path to `ticket_images` table
6. ✅ Shows success message with ticket ID
7. 🔄 Next: Display reports on home screen

Let me know if you hit any errors or want me to implement the home screen data fetching!
