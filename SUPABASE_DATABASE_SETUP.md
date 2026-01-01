# 🗄️ Supabase Database Setup Guide

## Overview
Your app now submits real tickets to Supabase backend. Here's the complete database structure you need.

---

## 📊 DATABASE TABLES

### 1. **profiles** table (Already exists from auth)
```sql
-- This table should already exist. If not, create it:
CREATE TABLE profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  full_name TEXT,
  role TEXT NOT NULL CHECK (role IN ('citizen', 'council', 'engineer')),
  email TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create index
CREATE INDEX idx_profiles_role ON profiles(role);
```

**Purpose**: Stores user profile information linked to auth.users

**Columns**:
- `id` (UUID): Foreign key to auth.users.id
- `full_name` (TEXT): User's full name
- `role` (TEXT): User role - "citizen", "council", or "engineer"
- `email` (TEXT): User's email
- `created_at` (TIMESTAMPTZ): Account creation timestamp

---

### 2. **tickets** table (NEW - MUST CREATE)
```sql
CREATE TABLE tickets (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ticket_id TEXT NOT NULL UNIQUE,
  reporter_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  issue_type TEXT NOT NULL,
  severity TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'Pending',
  location TEXT,
  description TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create indexes for faster queries
CREATE INDEX idx_tickets_reporter ON tickets(reporter_id);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_created ON tickets(created_at DESC);
CREATE INDEX idx_tickets_ticket_id ON tickets(ticket_id);
```

**Purpose**: Stores infrastructure issue reports

**Columns**:
- `id` (UUID): Primary key (auto-generated)
- `ticket_id` (TEXT): Human-readable ticket ID like "T1735689123456"
- `reporter_id` (UUID): Who reported it (foreign key to auth.users)
- `issue_type` (TEXT): "Pothole", "Broken Streetlight", "Damaged Pipe", or "Other"
- `severity` (TEXT): "Low", "Medium", or "High"
- `status` (TEXT): "Pending", "Accepted", "Rejected", "Spam"
- `location` (TEXT): Where the issue is located
- `description` (TEXT): Details about the issue
- `created_at` (TIMESTAMPTZ): When ticket was created
- `updated_at` (TIMESTAMPTZ): Last update time

---

### 3. **ticket_images** table (NEW - MUST CREATE)
```sql
CREATE TABLE ticket_images (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
  bucket TEXT NOT NULL,
  path TEXT NOT NULL,
  filename TEXT NOT NULL,
  metadata JSONB,
  uploaded_by UUID REFERENCES auth.users(id),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create index
CREATE INDEX idx_ticket_images_ticket ON ticket_images(ticket_id);
```

**Purpose**: Stores metadata for images uploaded with tickets

**Columns**:
- `id` (UUID): Primary key (auto-generated)
- `ticket_id` (UUID): Which ticket this image belongs to (foreign key)
- `bucket` (TEXT): Supabase storage bucket name ("ticket-images")
- `path` (TEXT): File path in storage (e.g., "uuid/image_123.jpg")
- `filename` (TEXT): Original filename
- `metadata` (JSONB): Image metadata (size, content type, etc.)
- `uploaded_by` (UUID): Who uploaded it (foreign key to auth.users)
- `created_at` (TIMESTAMPTZ): Upload timestamp

---

## 🔗 RELATIONSHIPS

```
auth.users (Built-in Supabase table)
    |
    ├─→ profiles (id → auth.users.id)
    |       └─ Stores: full_name, role, email
    |
    ├─→ tickets (reporter_id → auth.users.id)
    |       └─ Stores: ticket details, status, location
    |       └─ Has many: ticket_images
    |
    └─→ ticket_images (uploaded_by → auth.users.id)
            └─ Belongs to: tickets (ticket_id → tickets.id)
```

**Key Foreign Keys**:
1. `profiles.id` → `auth.users.id` (ON DELETE CASCADE)
2. `tickets.reporter_id` → `auth.users.id` (ON DELETE CASCADE)
3. `ticket_images.ticket_id` → `tickets.id` (ON DELETE CASCADE)
4. `ticket_images.uploaded_by` → `auth.users.id` (No cascade)

---

## 🔒 ROW LEVEL SECURITY (RLS) POLICIES

### Enable RLS on tables:
```sql
ALTER TABLE tickets ENABLE ROW LEVEL SECURITY;
ALTER TABLE ticket_images ENABLE ROW LEVEL SECURITY;
```

### Policies for **tickets** table:

```sql
-- Citizens can insert their own tickets
CREATE POLICY "Citizens can insert their own tickets"
ON tickets FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = reporter_id);

-- Users can view their own tickets
CREATE POLICY "Users can view their own tickets"
ON tickets FOR SELECT
TO authenticated
USING (auth.uid() = reporter_id);

-- Council and Engineers can view all tickets
CREATE POLICY "Council and Engineers can view all tickets"
ON tickets FOR SELECT
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role IN ('council', 'engineer')
  )
);

-- Council can update tickets (status, assignments, etc.)
CREATE POLICY "Council can update tickets"
ON tickets FOR UPDATE
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role = 'council'
  )
);
```

### Policies for **ticket_images** table:

```sql
-- Users can insert images for their own tickets
CREATE POLICY "Users can insert images for their tickets"
ON ticket_images FOR INSERT
TO authenticated
WITH CHECK (
  EXISTS (
    SELECT 1 FROM tickets
    WHERE tickets.id = ticket_images.ticket_id
    AND tickets.reporter_id = auth.uid()
  )
);

-- Users can view images for tickets they have access to
CREATE POLICY "Users can view ticket images"
ON ticket_images FOR SELECT
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM tickets
    WHERE tickets.id = ticket_images.ticket_id
    AND (
      tickets.reporter_id = auth.uid()
      OR EXISTS (
        SELECT 1 FROM profiles
        WHERE profiles.id = auth.uid()
        AND profiles.role IN ('council', 'engineer')
      )
    )
  )
);
```

---

## 💾 STORAGE BUCKET

### Create bucket:
1. Go to **Supabase Dashboard** → **Storage**
2. Click **"New bucket"**
3. Settings:
   - **Name**: `ticket-images`
   - **Public bucket**: ❌ UNCHECK (keep private)
   - **File size limit**: 5 MB
   - **Allowed MIME types**: image/jpeg, image/png, image/jpg

### Storage Policies:

```sql
-- Allow authenticated users to upload
CREATE POLICY "Authenticated users can upload ticket images"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (bucket_id = 'ticket-images');

-- Allow users to view images
CREATE POLICY "Users can view ticket images"
ON storage.objects FOR SELECT
TO authenticated
USING (bucket_id = 'ticket-images');

-- Allow users to delete their own images (optional)
CREATE POLICY "Users can delete their own images"
ON storage.objects FOR DELETE
TO authenticated
USING (
  bucket_id = 'ticket-images' 
  AND auth.uid()::text = (storage.foldername(name))[1]
);
```

---

## ⚙️ AUTO-UPDATE TRIGGER

```sql
-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to tickets table
CREATE TRIGGER update_tickets_updated_at
BEFORE UPDATE ON tickets
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
```

---

## ✅ VERIFICATION QUERIES

After creating everything, run these to verify:

```sql
-- 1. Check all tables exist
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('tickets', 'ticket_images', 'profiles');

-- 2. Check foreign key relationships
SELECT
  tc.table_name,
  kcu.column_name,
  ccu.table_name AS foreign_table,
  ccu.column_name AS foreign_column
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
AND tc.table_name IN ('tickets', 'ticket_images');

-- 3. Check RLS is enabled
SELECT schemaname, tablename, rowsecurity 
FROM pg_tables 
WHERE tablename IN ('tickets', 'ticket_images');

-- 4. Check storage bucket exists
SELECT * FROM storage.buckets WHERE name = 'ticket-images';
```

---

## 🧪 TEST DATA

Insert a test ticket to verify everything works:

```sql
-- Get your user ID first
SELECT id, email FROM auth.users LIMIT 1;

-- Insert test ticket (replace 'YOUR_USER_UUID' with actual ID)
INSERT INTO tickets (
  ticket_id, 
  reporter_id, 
  issue_type, 
  severity, 
  status, 
  location, 
  description
)
VALUES (
  'T1234567890',
  'YOUR_USER_UUID',
  'Pothole',
  'High',
  'Pending',
  'Kuala Lumpur',
  'Test ticket from SQL'
);

-- Verify it was created
SELECT * FROM tickets WHERE ticket_id = 'T1234567890';
```

---

## 📱 HOW THE APP USES THIS

1. **Citizen submits ticket** via ReportIssueFragment
   - App calls `SupabaseManager.submitTicket()`
   - Inserts row into `tickets` table
   - Returns ticket UUID

2. **App uploads image**
   - App calls `SupabaseManager.uploadTicketImage()`
   - Uploads binary data to `ticket-images` bucket
   - Inserts metadata into `ticket_images` table

3. **Council/Engineers view tickets**
   - Query `tickets` table filtered by RLS policies
   - Join with `profiles` to get reporter name
   - Join with `ticket_images` to get image URLs

---

## 🚨 IMPORTANT NOTES

1. **Make sure email confirmation is DISABLED** in Supabase:
   - Go to Authentication → Settings → Email Auth
   - Uncheck "Enable email confirmations"

2. **Set JWT expiry** appropriately:
   - Authentication → Settings → JWT Settings
   - Default 3600 seconds is fine for testing

3. **Check API URL and Keys** in your app:
   - `BuildConfig.SUPABASE_URL`
   - `BuildConfig.SUPABASE_KEY`

4. **Storage bucket must be created** before app can upload images

---

## 📋 QUICK SETUP CHECKLIST

- [ ] Create `tickets` table
- [ ] Create `ticket_images` table
- [ ] Verify `profiles` table exists
- [ ] Enable RLS on `tickets`
- [ ] Enable RLS on `ticket_images`
- [ ] Create all RLS policies (6 total)
- [ ] Create `ticket-images` storage bucket
- [ ] Set storage bucket policies (3 total)
- [ ] Create auto-update trigger
- [ ] Run verification queries
- [ ] Test with sample data
- [ ] Disable email confirmation in auth settings

---

✅ **You're all set!** Your app will now submit real tickets to Supabase instead of using mock data.
