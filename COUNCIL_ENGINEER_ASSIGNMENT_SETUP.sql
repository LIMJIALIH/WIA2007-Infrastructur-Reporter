-- ========================================
-- COUNCIL DASHBOARD UPDATES - Part 2
-- ========================================
-- Additional database changes for engineer assignment
-- ========================================

-- Step 1: Add assigned_engineer_id column to tickets table
-- This will track which engineer is assigned to each ticket
ALTER TABLE tickets
ADD COLUMN IF NOT EXISTS assigned_engineer_id UUID REFERENCES auth.users(id);

-- Add index for faster queries
CREATE INDEX IF NOT EXISTS idx_tickets_assigned_engineer ON tickets(assigned_engineer_id);

-- Step 2: Add assigned_engineer_name column (denormalized for easier display)
-- This avoids needing to join with profiles every time
ALTER TABLE tickets
ADD COLUMN IF NOT EXISTS assigned_engineer_name TEXT;

-- Step 3: Add council_notes column for instructions from council to engineers
ALTER TABLE tickets
ADD COLUMN IF NOT EXISTS council_notes TEXT;

-- Step 4: Add assigned_at timestamp
ALTER TABLE tickets
ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMPTZ;

-- ========================================
-- OPTIONAL: Migrate existing data
-- ========================================
-- If you have existing tickets that are marked as ACCEPTED,
-- you might want to add default values or leave them NULL

-- Example: Set default for existing accepted tickets
-- UPDATE tickets
-- SET assigned_at = updated_at
-- WHERE status = 'accepted' AND assigned_at IS NULL;

-- ========================================
-- VERIFICATION QUERIES
-- ========================================

-- Check the new columns
-- SELECT column_name, data_type, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'tickets'
-- AND column_name IN ('assigned_engineer_id', 'assigned_engineer_name', 'council_notes', 'assigned_at');

-- ========================================
-- EXAMPLE USAGE
-- ========================================

-- Assign a ticket to an engineer:
-- UPDATE tickets
-- SET 
--   status = 'accepted',
--   assigned_engineer_id = '<engineer_user_id>',
--   assigned_engineer_name = '<engineer_full_name>',
--   council_notes = 'Please check the electrical wiring',
--   assigned_at = NOW()
-- WHERE ticket_id = 'TKT123';

-- Get all tickets assigned to a specific engineer:
-- SELECT * FROM tickets
-- WHERE assigned_engineer_id = '<engineer_user_id>';

-- Get all engineers with their ticket counts:
-- SELECT 
--   p.id,
--   p.full_name,
--   p.email,
--   COUNT(t.id) as total_tickets,
--   COUNT(CASE WHEN t.severity = 'High' THEN 1 END) as high_priority_tickets
-- FROM profiles p
-- LEFT JOIN tickets t ON t.assigned_engineer_id = p.id
-- WHERE p.role = 'engineer'
-- GROUP BY p.id, p.full_name, p.email
-- ORDER BY total_tickets DESC;

-- ========================================
-- NOTES:
-- ========================================
-- 1. After running this script, rebuild your Android app
-- 2. The assigned_engineer_id will be NULL for unassigned tickets
-- 3. Council members can now assign tickets to engineers through the app
-- 4. Engineers will see their assigned tickets in their dashboard
-- ========================================
