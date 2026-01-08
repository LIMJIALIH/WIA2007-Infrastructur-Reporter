-- Soft Delete Functions
-- Allows each role to hide tickets from their view without permanently deleting

-- Citizen soft delete
CREATE OR REPLACE FUNCTION soft_delete_ticket_for_citizen(ticket_id_param UUID)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  UPDATE tickets
  SET 
    deleted_by_citizen = TRUE,
    updated_at = NOW()
  WHERE id = ticket_id_param
  AND reporter_id::uuid = auth.uid();
END;
$$;

-- Council soft delete
CREATE OR REPLACE FUNCTION soft_delete_ticket_for_council(ticket_id_param UUID)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  UPDATE tickets
  SET 
    deleted_by_council = TRUE,
    updated_at = NOW()
  WHERE id = ticket_id_param
  AND EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role IN ('council', 'admin')
  );
END;
$$;

-- Engineer soft delete
CREATE OR REPLACE FUNCTION soft_delete_ticket_for_engineer(ticket_id_param UUID)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  UPDATE tickets
  SET 
    deleted_by_engineer = TRUE,
    updated_at = NOW()
  WHERE id = ticket_id_param
  AND EXISTS (
    SELECT 1 FROM profiles
    WHERE profiles.id = auth.uid()
    AND profiles.role = 'engineer'
  );
END;
$$;

-- Grant permissions
GRANT EXECUTE ON FUNCTION soft_delete_ticket_for_citizen(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION soft_delete_ticket_for_council(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION soft_delete_ticket_for_engineer(UUID) TO authenticated;
