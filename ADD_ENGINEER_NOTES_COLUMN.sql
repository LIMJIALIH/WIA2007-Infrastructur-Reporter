-- ========================================
-- ADD ENGINEER_NOTES COLUMN TO TICKETS TABLE
-- ========================================
-- This column stores the engineer's reason for accepting or rejecting a ticket
-- Citizens will see this reason along with the final status

-- Add engineer_notes column if it doesn't exist
ALTER TABLE tickets 
ADD COLUMN IF NOT EXISTS engineer_notes TEXT;

-- Add index for faster queries
CREATE INDEX IF NOT EXISTS idx_tickets_engineer_notes 
ON tickets(engineer_notes) 
WHERE engineer_notes IS NOT NULL;

-- ========================================
-- VERIFICATION
-- ========================================
-- Run this to verify the column was added:
-- SELECT column_name, data_type, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'tickets' AND column_name = 'engineer_notes';
