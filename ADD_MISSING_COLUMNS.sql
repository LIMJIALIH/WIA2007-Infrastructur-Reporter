-- ========================================
-- ADD MISSING SOFT DELETE COLUMNS
-- ========================================
-- ERROR: column "deleted_by_citizen" does not exist
-- This must be run FIRST before any policies will work!
-- ========================================

-- Add the three soft delete columns
ALTER TABLE tickets 
ADD COLUMN IF NOT EXISTS deleted_by_citizen BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS deleted_by_council BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS deleted_by_engineer BOOLEAN DEFAULT FALSE;

-- Verify the columns were added
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'tickets'
AND column_name IN ('deleted_by_citizen', 'deleted_by_council', 'deleted_by_engineer');

-- You should see 3 rows with these columns
-- If you see 0 rows, there was an error

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_citizen ON tickets(deleted_by_citizen);
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_council ON tickets(deleted_by_council);
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_engineer ON tickets(deleted_by_engineer);

-- Test it now
UPDATE tickets SET deleted_by_citizen = TRUE WHERE id = '47b9c963-10ba-49b2-ae6d-e719d2e7dd36';

-- If this works, undo it:
UPDATE tickets SET deleted_by_citizen = FALSE WHERE id = '47b9c963-10ba-49b2-ae6d-e719d2e7dd36';

-- ========================================
-- AFTER ADDING COLUMNS, RUN FIX_UPDATE_POLICY.sql
-- ========================================
