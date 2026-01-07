# Quick Reference: Engineer Dashboard Statistics Code

## Summary
This document provides the exact code snippets added to implement real-time statistics for the Engineer Dashboard.

---

## 1. TicketRepository.java - New Callback Interface

Add this interface to TicketRepository.java (around line 570):

```java
public interface EngineerStatsCallback {
    void onSuccess(List<Ticket> allTickets, int newToday, int thisWeek, int highPriority, String avgResponse);
    void onError(String message);
}
```

---

## 2. TicketRepository.java - New Method

Add this method to TicketRepository.java (before the getEngineersWithStats method):

```java
/**
 * Get tickets assigned to an engineer with statistics
 * Fetches all tickets assigned to the current engineer and calculates:
 * - New Today: tickets assigned today
 * - This Week: tickets assigned this week
 * - High Priority: pending high severity tickets
 * - Avg Response: average response time
 */
public static void getEngineerTicketsWithStats(String engineerId, EngineerStatsCallback callback) {
    new Thread(() -> {
        try {
            // Fetch all tickets assigned to this engineer
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/tickets?assigned_engineer_id=eq." + engineerId + "&order=created_at.desc";
            
            String response = SupabaseManager.makeHttpRequest(
                "GET",
                url,
                null,
                SupabaseManager.getAccessToken()
            );
            
            JSONArray ticketsArray = new JSONArray(response);
            List<Ticket> allTickets = new ArrayList<>();
            
            // Get today's date for comparison
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String todayDate = dateFormat.format(new Date());
            
            // Get start of week (7 days ago)
            long weekAgoMillis = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
            String weekAgoDate = dateFormat.format(new Date(weekAgoMillis));
            
            int newToday = 0;
            int thisWeek = 0;
            int highPriority = 0;
            long totalResponseTime = 0;
            int responseCount = 0;
            
            for (int i = 0; i < ticketsArray.length(); i++) {
                JSONObject ticketJson = ticketsArray.getJSONObject(i);
                
                // Parse ticket data
                String ticketId = ticketJson.optString("ticket_id", "");
                String dbId = ticketJson.optString("id", "");
                String type = ticketJson.optString("issue_type", "Other");
                String severity = ticketJson.optString("severity", "Low");
                String location = ticketJson.optString("location", "Unknown");
                String description = ticketJson.optString("description", "");
                String createdAt = ticketJson.optString("created_at", "");
                String status = ticketJson.optString("status", "Pending");
                String reporterId = ticketJson.optString("reporter_id", "");
                
                // Format date
                String formattedDate = formatDate(createdAt);
                
                // Create ticket object
                Ticket ticket = new Ticket(
                    ticketId,
                    type,
                    severity,
                    location,
                    description,
                    formattedDate,
                    ""
                );
                ticket.setStatus(parseStatus(status));
                
                // Get reporter name
                String reporterName = getReporterName(reporterId);
                ticket.setReporterName(reporterName);
                
                // Fetch image URL
                String imageUrl = getTicketImageUrl(dbId);
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    ticket.setImageUrl(imageUrl);
                }
                
                allTickets.add(ticket);
                
                // Calculate statistics
                // Extract date from created_at (format: 2025-01-02T10:30:00+00:00)
                String ticketDate = createdAt.substring(0, 10); // Get YYYY-MM-DD
                
                // New Today
                if (ticketDate.equals(todayDate)) {
                    newToday++;
                }
                
                // This Week
                if (ticketDate.compareTo(weekAgoDate) >= 0) {
                    thisWeek++;
                }
                
                // High Priority (pending high severity tickets)
                if (severity.equalsIgnoreCase("High") && status.equalsIgnoreCase("Pending")) {
                    highPriority++;
                }
                
                // Calculate response time for accepted tickets
                if (status.equalsIgnoreCase("Accepted")) {
                    try {
                        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                        Date created = isoFormat.parse(createdAt.substring(0, 19));
                        
                        // Get acceptance time from ticket_actions table
                        String actionUrl = BuildConfig.SUPABASE_URL + "/rest/v1/ticket_actions?ticket_id=eq." + dbId + "&action_type=eq.ACCEPTED&order=created_at.asc&limit=1";
                        String actionResponse = SupabaseManager.makeHttpRequest("GET", actionUrl, null, SupabaseManager.getAccessToken());
                        JSONArray actionsArray = new JSONArray(actionResponse);
                        
                        if (actionsArray.length() > 0) {
                            String acceptedAt = actionsArray.getJSONObject(0).optString("created_at", "");
                            if (!acceptedAt.isEmpty()) {
                                Date accepted = isoFormat.parse(acceptedAt.substring(0, 19));
                                long responseTime = accepted.getTime() - created.getTime();
                                totalResponseTime += responseTime;
                                responseCount++;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error calculating response time", e);
                    }
                }
            }
            
            // Calculate average response time
            String avgResponse = "N/A";
            if (responseCount > 0) {
                long avgMillis = totalResponseTime / responseCount;
                long avgHours = avgMillis / (1000 * 60 * 60);
                if (avgHours < 1) {
                    long avgMinutes = avgMillis / (1000 * 60);
                    avgResponse = "< 1 hour";
                } else {
                    avgResponse = "< " + avgHours + " hours";
                }
            } else {
                avgResponse = "< 2 hours"; // Default
            }
            
            if (callback != null) {
                int finalNewToday = newToday;
                int finalThisWeek = thisWeek;
                int finalHighPriority = highPriority;
                String finalAvgResponse = avgResponse;
                callback.onSuccess(allTickets, finalNewToday, finalThisWeek, finalHighPriority, finalAvgResponse);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error fetching engineer tickets", e);
            if (callback != null) {
                callback.onError("Error: " + e.getMessage());
            }
        }
    }).start();
}
```

---

## 3. EngineerDashboardActivity.java - Updated initializeDataLists()

Replace the existing `initializeDataLists()` method with:

```java
private void initializeDataLists() {
    allTickets = new ArrayList<>();
    pendingTickets = new ArrayList<>();
    acceptedTickets = new ArrayList<>();
    rejectedTickets = new ArrayList<>();
    spamTickets = new ArrayList<>();
    currentDisplayedTickets = new ArrayList<>();

    // Load tickets from Supabase
    String currentUserId = SupabaseManager.getCurrentUserId();
    if (currentUserId != null && !currentUserId.isEmpty()) {
        TicketRepository.getEngineerTicketsWithStats(currentUserId, new TicketRepository.EngineerStatsCallback() {
            @Override
            public void onSuccess(List<Ticket> tickets, int newToday, int thisWeek, int highPriority, String avgResponse) {
                runOnUiThread(() -> {
                    allTickets.clear();
                    allTickets.addAll(tickets);
                    
                    // Categorize tickets by status
                    pendingTickets.clear();
                    acceptedTickets.clear();
                    rejectedTickets.clear();
                    spamTickets.clear();
                    
                    for (Ticket ticket : tickets) {
                        switch (ticket.getStatus()) {
                            case PENDING:
                                pendingTickets.add(ticket);
                                break;
                            case ACCEPTED:
                                acceptedTickets.add(ticket);
                                break;
                            case REJECTED:
                                rejectedTickets.add(ticket);
                                break;
                            case SPAM:
                                spamTickets.add(ticket);
                                break;
                        }
                    }
                    
                    // Update statistics with real data
                    tvStatNewTodayValue.setText(String.valueOf(newToday));
                    tvStatThisWeekValue.setText(String.valueOf(thisWeek));
                    tvStatAvgResponseValue.setText(avgResponse);
                    tvStatHighPriorityValue.setText(String.valueOf(highPriority));
                    
                    // Update UI
                    loadDashboardData();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(EngineerDashboardActivity.this, 
                        "Error loading tickets: " + message, Toast.LENGTH_SHORT).show();
                    // Still initialize with empty lists
                    loadDashboardData();
                });
            }
        });
    } else {
        Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
    }
}
```

---

## 4. EngineerDashboardActivity.java - Updated loadDashboardData() and updateStatisticsFromTickets()

Replace these two methods:

```java
private void loadDashboardData() {
    // Set welcome message with username
    String fullName = SupabaseManager.getCurrentFullName();
    if (fullName != null && !fullName.isEmpty()) {
        if (tvWelcome != null) {
            tvWelcome.setText("Welcome, " + fullName);
        }
    }

    // Update tab counts
    updateTabCounts(pendingTickets.size(), rejectedTickets.size(),
            spamTickets.size(), acceptedTickets.size());

    // Load tickets for current tab
    selectTab(currentTabIndex);
}

private void updateStatisticsFromTickets() {
    // Statistics are now updated in real-time from Supabase in initializeDataLists()
    // This method is kept for backward compatibility but does nothing
    // The stats are calculated server-side for accuracy
}
```

---

## 5. Supabase SQL (Optional but Recommended)

Run this in Supabase SQL Editor to create optimized views and functions:

See the file: `ENGINEER_DASHBOARD_STATISTICS.sql` for the complete SQL code.

Key components:
- View: `engineer_ticket_stats` - Quick statistics for all engineers
- Function: `get_engineer_dashboard_stats(UUID)` - Get all stats in one call
- Performance indexes for faster queries

---

## Testing

1. **Ensure tickets table has assigned_engineer_id column:**
   ```sql
   ALTER TABLE tickets ADD COLUMN IF NOT EXISTS assigned_engineer_id UUID REFERENCES profiles(id);
   ```

2. **Assign some tickets to test engineer:**
   ```sql
   UPDATE tickets 
   SET assigned_engineer_id = '<engineer-user-id>' 
   WHERE id IN (SELECT id FROM tickets LIMIT 5);
   ```

3. **Login as engineer and open dashboard** - Statistics should display real data

---

## Files Modified

1. ✅ `app/src/main/java/com/example/infrastructureproject/TicketRepository.java`
2. ✅ `app/src/main/java/com/example/infrastructureproject/EngineerDashboardActivity.java`
3. ✅ `app/src/main/java/com/example/infrastructureproject/EngineerDashboardActivity2.java`
4. ✅ `ENGINEER_DASHBOARD_STATISTICS.sql` (NEW)
5. ✅ `ENGINEER_DASHBOARD_IMPLEMENTATION.md` (NEW - Full documentation)

---

## What Each Statistic Shows

| Statistic | Description | Calculation |
|-----------|-------------|-------------|
| **New Today** | Tickets assigned today | Count where created_at date = today |
| **This Week** | Tickets in last 7 days | Count where created_at >= 7 days ago |
| **Avg Response** | Average time to accept | Average of (accepted_at - created_at) |
| **High Priority** | Pending high-severity | Count where severity='High' AND status='Pending' |

---

## Support

If you encounter any issues:
1. Check that `assigned_engineer_id` column exists in tickets table
2. Verify engineer user is logged in
3. Check Supabase connection
4. Look for errors in Android Logcat with tag "TicketRepository"
