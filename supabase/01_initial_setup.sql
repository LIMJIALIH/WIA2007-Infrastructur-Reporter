-- Initial Database Setup
-- Run this first to set up basic table structure and roles

-- Add role constraints to profiles table
DO $$ 
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'check_role'
  ) THEN
    ALTER TABLE profiles
    ADD CONSTRAINT check_role 
    CHECK (role IN ('citizen', 'engineer', 'council', 'admin'));
  END IF;
END $$;

-- Create index for faster role queries
CREATE INDEX IF NOT EXISTS idx_profiles_role ON profiles(role);

-- Add soft delete columns to hide tickets per role
ALTER TABLE tickets 
ADD COLUMN IF NOT EXISTS deleted_by_citizen BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS deleted_by_council BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS deleted_by_engineer BOOLEAN DEFAULT FALSE;

-- Add columns for engineer workflow
ALTER TABLE tickets 
ADD COLUMN IF NOT EXISTS engineer_notes TEXT,
ADD COLUMN IF NOT EXISTS assigned_engineer_id UUID REFERENCES profiles(id),
ADD COLUMN IF NOT EXISTS assigned_engineer_name TEXT,
ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS council_notes TEXT,
ADD COLUMN IF NOT EXISTS is_spam BOOLEAN DEFAULT FALSE;

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_citizen ON tickets(deleted_by_citizen);
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_council ON tickets(deleted_by_council);
CREATE INDEX IF NOT EXISTS idx_tickets_deleted_by_engineer ON tickets(deleted_by_engineer);
CREATE INDEX IF NOT EXISTS idx_tickets_assigned_engineer ON tickets(assigned_engineer_id);
CREATE INDEX IF NOT EXISTS idx_tickets_is_spam ON tickets(is_spam);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);

-- Update status constraint to include all valid values
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM pg_constraint 
    WHERE conrelid = 'public.tickets'::regclass 
      AND conname = 'tickets_status_check'
  ) THEN
    ALTER TABLE public.tickets DROP CONSTRAINT tickets_status_check;
  END IF;
END $$;

ALTER TABLE tickets
ADD CONSTRAINT tickets_status_check
CHECK (status IN ('Pending', 'UNDER_REVIEW', 'Accepted', 'Rejected', 'SPAM'));

-- Set default status for new tickets
ALTER TABLE tickets ALTER COLUMN status SET DEFAULT 'Pending';
