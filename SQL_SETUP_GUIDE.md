# WIA2007 Infrastructure Reporter - Supabase SQL Setup Guide

## Overview

This guide covers the complete database setup for the Infrastructure Reporter application using Supabase. The system supports three user roles: **Citizens**, **Council**, and **Engineers**, each with different permissions and views.

## Quick Start

Run the SQL files in the `supabase/` folder in numerical order:

1. **01_initial_setup.sql** - Creates tables, columns, and constraints
2. **02_ticket_actions.sql** - Sets up action logging for auditing
3. **03_rls_policies.sql** - Configures row-level security for tickets
4. **04_profiles_rls.sql** - Configures security for user profiles
5. **05_soft_delete_functions.sql** - Enables soft delete per role
6. **06_spam_handling.sql** - Manages spam ticket workflow
7. **07_statistics_views.sql** - Creates dashboard statistics views
8. **08_diagnostic_queries.sql** - Diagnostic queries for troubleshooting

## Core Features

### 1. Role-Based Access Control

Each role has specific permissions:

**Citizens:**
- View their own tickets (not soft-deleted by them)
- Create new tickets
- Soft-delete their own tickets (hides from their view only)

**Council:**
- View all tickets (not soft-deleted by them)
- Assign tickets to engineers
- Mark tickets as spam
- Soft-delete any ticket (hides from council view only)

**Engineers:**
- View assigned tickets (not soft-deleted, not spam)
- Accept, reject, or mark tickets as spam
- Soft-delete assigned tickets (hides from engineer view only)

### 2. Soft Delete System

Tickets are never permanently deleted from the database. Instead, each role can hide tickets from their own view:

- `deleted_by_citizen` - Hides ticket from citizen's view
- `deleted_by_council` - Hides ticket from council's view
- `deleted_by_engineer` - Hides ticket from engineer's view

**Example:** A citizen deletes a ticket → it disappears from the citizen's app, but council and engineers can still see it.

### 3. Ticket Status Workflow

**Valid statuses:**
- `Pending` - New ticket, awaiting council assignment
- `UNDER_REVIEW` - Assigned to engineer, awaiting action
- `Accepted` - Engineer has completed work
- `Rejected` - Engineer rejected the ticket
- `SPAM` - Marked as spam by engineer or council

### 4. Spam Handling

Two ways to mark spam:

**Engineer marks as SPAM:**
- Status becomes `SPAM`
- `is_spam` flag set to true
- Automatically adds "Marked As Spam" to engineer notes
- Ticket hidden from engineer's view
- Citizens see status as "Rejected" with reason

**Council marks as SPAM:**
- Status becomes `Rejected`
- `council_notes` set to "Marked as Spam by the Council"
- `is_spam` flag set to true
- Appears in council's Spam tab

### 5. Action Logging

All engineer actions (accept/reject/spam) are logged in the `ticket_actions` table with:
- Ticket ID
- Action type
- Reason/notes
- Timestamp
- Engineer ID

This enables:
- Audit trail
- Average response time calculations
- Performance metrics

### 6. Statistics & Analytics

**Council Dashboard:**
- Total reports
- Pending tickets
- Under review tickets
- Completed tickets
- Spam tickets
- High priority pending
- Average response time

**Engineer Dashboard:**
- Total assigned tickets
- New today
- This week
- Pending/Accepted/Rejected/Spam counts
- High priority pending
- Average response time

## Database Schema

### Tickets Table (Key Columns)

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `ticket_id` | TEXT | Human-readable ID |
| `reporter_id` | UUID | References citizen's profile |
| `status` | TEXT | Current status (see workflow) |
| `severity` | TEXT | High/Medium/Low |
| `deleted_by_citizen` | BOOLEAN | Soft delete flag for citizen |
| `deleted_by_council` | BOOLEAN | Soft delete flag for council |
| `deleted_by_engineer` | BOOLEAN | Soft delete flag for engineer |
| `is_spam` | BOOLEAN | Spam indicator |
| `assigned_engineer_id` | UUID | Assigned engineer reference |
| `assigned_engineer_name` | TEXT | Engineer name (denormalized) |
| `assigned_at` | TIMESTAMPTZ | When assigned |
| `engineer_notes` | TEXT | Engineer's accept/reject reason |
| `council_notes` | TEXT | Council's instructions |
| `created_at` | TIMESTAMPTZ | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | Last update timestamp |

### Ticket Actions Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `ticket_id` | UUID | References ticket |
| `created_by` | UUID | Engineer who performed action |
| `action_type` | TEXT | ACCEPTED/REJECTED/SPAM |
| `reason` | TEXT | Optional reason/notes |
| `created_at` | TIMESTAMPTZ | Action timestamp |

### Profiles Table (Key Columns)

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `user_id` | UUID | References auth.users |
| `role` | TEXT | citizen/engineer/council/admin |
| `full_name` | TEXT | User's full name |
| `email` | TEXT | User's email |

## Security Implementation

### Row Level Security (RLS)

All tables have RLS enabled. Policies ensure:

1. **Citizens** only see their own tickets
2. **Council** sees all tickets (except soft-deleted)
3. **Engineers** only see assigned tickets (except spam/soft-deleted)

### Helper Functions

**`is_user_in_role(user_id, role_name)`**
- Checks if user has specific role
- Prevents recursion in RLS policies
- Used in council and engineer policies

**Soft Delete Functions:**
- `soft_delete_ticket_for_citizen(ticket_id)`
- `soft_delete_ticket_for_council(ticket_id)`
- `soft_delete_ticket_for_engineer(ticket_id)`

**Spam Function:**
- `mark_ticket_as_spam(ticket_id)` - Sets status to SPAM, nullifies reason

## Common Operations

### Assigning a Ticket to Engineer

```sql
UPDATE tickets
SET 
  status = 'UNDER_REVIEW',
  assigned_engineer_id = '<engineer_uuid>',
  assigned_engineer_name = 'Engineer Name',
  council_notes = 'Please check the electrical wiring',
  assigned_at = NOW()
WHERE id = '<ticket_uuid>';
```

### Engineer Accepting a Ticket

```sql
-- Update ticket status
UPDATE tickets
SET status = 'Accepted',
    engineer_notes = 'Work completed successfully'
WHERE id = '<ticket_uuid>';

-- Log action
INSERT INTO ticket_actions (ticket_id, created_by, action_type, reason)
VALUES ('<ticket_uuid>', auth.uid(), 'ACCEPTED', 'Work completed successfully');
```

### Citizen Soft-Deleting a Ticket

```sql
SELECT soft_delete_ticket_for_citizen('<ticket_uuid>');
```

### Viewing Council Statistics

```sql
SELECT * FROM council_dashboard_stats;
SELECT get_council_avg_response_time();
```

### Viewing Engineer Statistics

```sql
SELECT * FROM engineer_ticket_stats WHERE engineer_id = '<engineer_uuid>';
SELECT get_engineer_avg_response_time('<engineer_uuid>');
```

## Troubleshooting

### Common Issues

**1. HTTP 400/403 Errors When Updating Tickets**
- Check RLS policies are created correctly
- Verify user has correct role in profiles table
- Ensure soft-delete columns exist with default FALSE

**2. Infinite Recursion in Profiles**
- Use the `is_user_in_role()` function instead of direct profile queries in RLS
- Profiles policies should use `auth.uid()` directly without subqueries

**3. SPAM Tickets Not Hiding from Engineer View**
- Check that RLS policy filters `status != 'SPAM'`
- Verify trigger is installed to set `is_spam = true`

**4. Statistics Not Updating**
- Views are automatically updated (no caching)
- Check that `ticket_actions` table is being populated
- Verify `assigned_at` timestamps are set when assigning tickets

### Diagnostic Steps

1. Run queries from `08_diagnostic_queries.sql`
2. Check column existence and data types
3. Verify RLS policies exist and are correctly formatted
4. Test with actual user authentication tokens
5. Check Supabase logs for detailed error messages

## Migration from Existing Setup

If you already have a database, run each file carefully:

1. **Backup your data first**
2. Files are idempotent (safe to run multiple times)
3. Use `IF NOT EXISTS` and `IF EXISTS` clauses
4. Test with sample data before production deployment

## Performance Considerations

### Indexes Created

The setup creates indexes on:
- Soft delete flags
- Status column
- Assigned engineer ID
- Spam flag
- Ticket actions (ticket_id, action_type, created_at)

### Query Optimization Tips

- Use views for complex statistics (pre-computed)
- Soft-delete columns have partial indexes (WHERE column = false)
- Composite indexes for common query patterns

## Security Best Practices

1. **Never disable RLS** except temporarily for maintenance
2. **Use SECURITY DEFINER functions** for operations that need elevated privileges
3. **Validate user roles** before sensitive operations
4. **Log all important actions** in ticket_actions table
5. **Use prepared statements** in application code to prevent SQL injection

## Support and Maintenance

### Regular Maintenance

- Monitor table sizes and consider archiving old tickets
- Review RLS policies when adding new features
- Update statistics views if dashboard requirements change
- Backup database regularly

### Adding New Features

When adding features:
1. Update table schema first
2. Modify RLS policies if needed
3. Add corresponding application code
4. Test with all three user roles
5. Update statistics views if dashboard changes

## File Organization

All SQL files are organized in the `/supabase` folder with descriptive names and numbered for execution order. Each file contains:
- Clear, concise comments explaining what the code does
- Human-readable explanations
- Idempotent operations (safe to run multiple times)

---

**Last Updated:** January 8, 2026

For questions or issues, refer to the diagnostic queries in `08_diagnostic_queries.sql` or check application logs.
