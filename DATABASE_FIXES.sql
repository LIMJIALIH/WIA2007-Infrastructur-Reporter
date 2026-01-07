-- =====================================================
-- DATABASE FIXES FOR ALL NEW FEATURES
-- =====================================================
-- Run this SQL in your Supabase SQL Editor to enable:
-- 1. Soft delete for citizen and council views
-- 2. Ticket actions tracking (engineer accept/reject/spam)
-- 3. Ticket images storage
-- =====================================================

-- 1. ADD SOFT DELETE COLUMNS
-- These allow hiding tickets from citizen or council view without actually deleting them
ALTER TABLE tickets 
ADD COLUMN IF NOT EXISTS deleted_by_citizen BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS deleted_by_council BOOLEAN DEFAULT FALSE;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_citizen ON tickets(deleted_by_citizen) WHERE deleted_by_citizen = false;
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_council ON tickets(deleted_by_council) WHERE deleted_by_council = false;

-- 2. CREATE TICKET_ACTIONS TABLE
-- Tracks all actions taken on tickets by engineers
CREATE TABLE IF NOT EXISTS ticket_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES profiles(id),
    action_type TEXT NOT NULL CHECK (action_type IN ('ACCEPTED', 'REJECTED', 'SPAM')),
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for ticket_actions
CREATE INDEX IF NOT EXISTS idx_ticket_actions_ticket_id ON ticket_actions(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_actions_created_by ON ticket_actions(created_by);
CREATE INDEX IF NOT EXISTS idx_ticket_actions_created_at ON ticket_actions(created_at);

-- Enable RLS (Row Level Security) on ticket_actions
ALTER TABLE ticket_actions ENABLE ROW LEVEL SECURITY;

-- Policy: Engineers and council can view all ticket actions
CREATE POLICY "Engineers and council can view ticket actions" ON ticket_actions
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM profiles
            WHERE profiles.id = auth.uid()
            AND (profiles.role = 'engineer' OR profiles.role = 'council')
        )
    );

-- Policy: Engineers can insert their own actions
CREATE POLICY "Engineers can insert actions" ON ticket_actions
    FOR INSERT
    WITH CHECK (
        created_by = auth.uid() AND
        EXISTS (
            SELECT 1 FROM profiles
            WHERE profiles.id = auth.uid()
            AND profiles.role = 'engineer'
        )
    );

-- 3. CREATE TICKET_IMAGES TABLE
-- Stores paths to images in Supabase Storage
CREATE TABLE IF NOT EXISTS ticket_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    path TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create index for ticket_images
CREATE INDEX IF NOT EXISTS idx_ticket_images_ticket_id ON ticket_images(ticket_id);

-- Enable RLS on ticket_images
ALTER TABLE ticket_images ENABLE ROW LEVEL SECURITY;

-- Policy: Anyone can view ticket images
CREATE POLICY "Anyone can view ticket images" ON ticket_images
    FOR SELECT
    USING (true);

-- Policy: Users can insert images for their own tickets
CREATE POLICY "Users can insert images for own tickets" ON ticket_images
    FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM tickets
            WHERE tickets.id = ticket_id
            AND tickets.reporter_id = auth.uid()
        )
    );

-- 4. ADD ENGINEER_NOTES COLUMN (if not exists)
-- Stores reasons provided by engineers when accepting/rejecting tickets
ALTER TABLE tickets 
ADD COLUMN IF NOT EXISTS engineer_notes TEXT;

-- 5. CREATE STORAGE BUCKET FOR IMAGES (if not exists)
-- Run this separately in Supabase Storage dashboard or via Storage SQL
-- INSERT INTO storage.buckets (id, name, public) VALUES ('ticket-images', 'ticket-images', true)
-- ON CONFLICT (id) DO NOTHING;

-- 6. STORAGE BUCKET POLICIES
-- Enable public access to ticket images
-- Note: Run these in Supabase Storage Policies section:
-- CREATE POLICY "Public Access" ON storage.objects FOR SELECT TO public USING (bucket_id = 'ticket-images');
-- CREATE POLICY "Authenticated users can upload" ON storage.objects FOR INSERT TO authenticated WITH CHECK (bucket_id = 'ticket-images');

-- 7. UPDATE EXISTING TICKETS
-- Set default values for existing records
UPDATE tickets 
SET deleted_by_citizen = FALSE 
WHERE deleted_by_citizen IS NULL;

UPDATE tickets 
SET deleted_by_council = FALSE 
WHERE deleted_by_council IS NULL;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================
-- Run these to verify the changes were applied:

-- Check soft delete columns exist
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'tickets' 
AND column_name IN ('deleted_by_citizen', 'deleted_by_council', 'engineer_notes');

-- Check ticket_actions table exists
SELECT table_name 
FROM information_schema.tables 
WHERE table_name = 'ticket_actions';

-- Check ticket_images table exists
SELECT table_name 
FROM information_schema.tables 
WHERE table_name = 'ticket_images';

-- =====================================================
-- OPTIONAL: SAMPLE QUERIES
-- =====================================================

-- Get all tickets visible to citizens (not soft-deleted by citizen)
-- SELECT * FROM tickets WHERE deleted_by_citizen = FALSE;

-- Get all tickets visible to council (not soft-deleted by council)
-- SELECT * FROM tickets WHERE deleted_by_council = FALSE;

-- Get all actions for a specific ticket
-- SELECT * FROM ticket_actions WHERE ticket_id = 'your-ticket-id' ORDER BY created_at DESC;

-- Get all images for a specific ticket
-- SELECT * FROM ticket_images WHERE ticket_id = 'your-ticket-id';
