# Engineer Dashboard Statistics Implementation

## Overview
This implementation adds real-time statistics to the Engineer Dashboard that display:
1. **New Today** - Total tickets assigned to the engineer today
2. **This Week** - Total tickets assigned in the last 7 days  
3. **Avg Response** - Average response time from ticket creation to acceptance
4. **High Priority** - Count of pending high-severity tickets

## Changes Made

### 1. TicketRepository.java
**Location:** `app/src/main/java/com/example/infrastructureproject/TicketRepository.java`

#### Added New Callback Interface:
```java
public interface EngineerStatsCallback {
    void onSuccess(List<Ticket> allTickets, int newToday, int thisWeek, 
                   int highPriority, String avgResponse);
    void onError(String message);
}
```

#### Added New Method:
```java
public static void getEngineerTicketsWithStats(String engineerId, 
                                               EngineerStatsCallback callback)
```

**What it does:**
- Fetches all tickets assigned to the engineer from Supabase
- Calculates statistics in real-time:
  - **New Today:** Counts tickets created today
  - **This Week:** Counts tickets from last 7 days
  - **High Priority:** Counts pending tickets with "High" severity
  - **Avg Response:** Calculates average time between ticket creation and first acceptance (from ticket_actions table)
- Returns both the ticket list and calculated statistics
- Fetches reporter names and image URLs for each ticket

### 2. EngineerDashboardActivity.java
**Location:** `app/src/main/java/com/example/infrastructureproject/EngineerDashboardActivity.java`

#### Modified Methods:

**`initializeDataLists()`**
- Replaced mock data with real Supabase data loading
- Calls `TicketRepository.getEngineerTicketsWithStats()` 
- Categorizes tickets by status (Pending, Accepted, Rejected, Spam)
- Updates statistics cards with real data from callback
- Handles errors gracefully

**`loadDashboardData()`**
- Removed call to `updateStatisticsFromTickets()` 
- Statistics now updated directly from Supabase callback

**`updateStatisticsFromTickets()`**
- Kept for backward compatibility but does nothing
- Statistics are now calculated in `getEngineerTicketsWithStats()`

**`refreshDashboard()`**
- Already calls `initializeDataLists()` which now fetches fresh data from Supabase

### 3. EngineerDashboardActivity2.java
**Location:** `app/src/main/java/com/example/infrastructureproject/EngineerDashboardActivity2.java`

- Applied same changes as EngineerDashboardActivity.java for consistency
- This is the backup file maintained in sync

## Supabase Database Setup

### SQL File Created: ENGINEER_DASHBOARD_STATISTICS.sql
**Location:** `ENGINEER_DASHBOARD_STATISTICS.sql`

This file provides:

#### 1. **View: engineer_ticket_stats**
- Provides quick overview of all engineers and their ticket statistics
- Can be queried directly from the app for optimization

#### 2. **Function: get_engineer_avg_response_time(engineer_user_id UUID)**
- Calculates average response time for an engineer
- Returns formatted string (e.g., "< 2 hours")

#### 3. **Function: get_engineer_dashboard_stats(engineer_user_id UUID)**
- Returns all statistics in a single database call
- Alternative to calculating in Java (for optimization)
- Returns: total_tickets, new_today, this_week, pending_tickets, accepted_tickets, rejected_tickets, spam_tickets, high_priority_pending, avg_response_time

#### 4. **Performance Indexes**
- `idx_tickets_assigned_engineer` - Fast lookup by engineer
- `idx_tickets_status` - Filter by status
- `idx_tickets_severity` - Filter by severity  
- `idx_tickets_created_at` - Date-based queries
- `idx_tickets_engineer_status` - Composite index for common queries
- `idx_ticket_actions_ticket_id` - Fast action lookups

#### 5. **Sample Queries**
- Example queries for testing the statistics
- Queries for different time ranges and filters

## How It Works

### Current Implementation (Java-based calculation):
1. Engineer opens dashboard
2. App calls `TicketRepository.getEngineerTicketsWithStats(currentUserId, callback)`
3. Repository fetches all tickets assigned to engineer
4. Statistics are calculated in Java:
   - Parses dates to count today/this week
   - Filters by severity for high priority
   - Queries ticket_actions table for response times
5. Results returned via callback
6. UI updates with real statistics

### Alternative Implementation (SQL-based - Optional):
You can optimize by using the SQL functions:
```java
// Instead of getEngineerTicketsWithStats, call the RPC function:
String url = BuildConfig.SUPABASE_URL + "/rest/v1/rpc/get_engineer_dashboard_stats";
JSONObject body = new JSONObject();
body.put("engineer_user_id", currentUserId);
// Make POST request and parse response
```

## Database Requirements

### Existing Tables Used:
- `tickets` - Main ticket table
  - Must have: `assigned_engineer_id` column (UUID, references profiles.id)
  - Columns used: ticket_id, issue_type, severity, location, description, created_at, status, reporter_id
  
- `ticket_actions` - Action history table
  - Columns used: ticket_id, action_type, created_at
  - Used to calculate response time

- `profiles` - User profiles
  - Columns used: id, full_name, role
  - Used to get reporter names

- `ticket_images` - Image storage
  - Used to fetch ticket images

### Required Setup:
1. Run `COUNCIL_ENGINEER_ASSIGNMENT_SETUP.sql` if not already done
   - This adds the `assigned_engineer_id` column to tickets table
   
2. Optionally run `ENGINEER_DASHBOARD_STATISTICS.sql` 
   - Creates views, functions, and indexes for better performance
   - Not required for current implementation but recommended

## Testing

### To test the implementation:

1. **Assign tickets to an engineer:**
   ```sql
   UPDATE tickets 
   SET assigned_engineer_id = '<engineer-user-id>' 
   WHERE id IN (SELECT id FROM tickets LIMIT 5);
   ```

2. **Create test tickets for today:**
   ```sql
   INSERT INTO tickets (ticket_id, reporter_id, issue_type, severity, status, 
                       location, description, assigned_engineer_id, created_at)
   VALUES ('TKT' || extract(epoch from now())::text, 
           '<reporter-id>', 'Pothole', 'High', 'Pending', 
           'Test Location', 'Test Description', 
           '<engineer-user-id>', NOW());
   ```

3. **Test response time calculation:**
   ```sql
   -- Accept a ticket
   UPDATE tickets SET status = 'Accepted' WHERE id = '<ticket-id>';
   
   -- Add action record
   INSERT INTO ticket_actions (ticket_id, action_type, created_at)
   VALUES ('<ticket-db-id>', 'ACCEPTED', NOW());
   ```

4. **Login as engineer and verify:**
   - New Today count shows tickets created today
   - This Week shows last 7 days
   - High Priority shows pending high-severity tickets
   - Avg Response shows calculated average

## Benefits

1. **Real-time Data:** Statistics always reflect current database state
2. **Accurate Calculations:** Dates and times calculated correctly using timestamps
3. **Scalable:** Works with any number of tickets
4. **Efficient:** Single query fetches all needed data
5. **Maintainable:** Clear separation of concerns between repository and UI
6. **Future-proof:** Can easily switch to SQL-based calculation for better performance

## Notes

- Statistics update every time dashboard is opened or refreshed
- Response time only counts accepted tickets
- If no accepted tickets exist, shows default "< 2 hours"
- High priority only counts pending tickets (not accepted/rejected)
- All timestamps use device's current date for "today" calculation
- Week calculation uses last 7 days from current date

## Future Enhancements

1. **Cache statistics** - Store in SharedPreferences for offline access
2. **Real-time updates** - Use Supabase Realtime subscriptions
3. **Historical trends** - Show graphs of statistics over time
4. **Comparison** - Compare engineer performance with averages
5. **Notifications** - Alert when high-priority tickets are assigned
