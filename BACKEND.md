Tech Stack
Platform & Core
Android: Java, Gradle Kotlin DSL, compileSdk 36, minSdk 25
Backend: Supabase (PostgreSQL + Auth + Storage + Realtime)
Push Notifications: Firebase Cloud Messaging (FCM)
Networking & APIs
HTTP Client: Retrofit 2.9.0, OkHttp 4.12.0
JSON Parsing: Gson 2.10.1
AI Services: OpenAI GPT-4 Vision API, Google Gemini API, OpenAI ChatGPT API
Android Libraries
Image Loading: Glide 4.16.0
Location: Google Play Services Location 21.0.1
UI Components: RecyclerView 1.3.2, CardView 1.0.0, Material Design Components
Architecture
Pattern: Repository pattern for data access
Authentication: Session-based with SharedPreferences
Storage: Supabase Storage for ticket photos
Backend Work Summary
Person 1: Engineer Dashboard Backend
Creates: DashboardRepository.java, DashboardStats.java
Responsibilities: Fetch dashboard statistics (new today, this week, high priority, avg response), load tickets by status (pending/accepted/rejected/spam), implement search/filter functionality, handle ticket actions (accept/reject/spam) with Supabase API integration.

Person 2: Citizen Dashboard Backend
Creates: CitizenRepository.java, CitizenStats.java, FCMService.java
Responsibilities: Fetch citizen's own tickets from Supabase, calculate personal statistics (total reports, pending, accepted, rejected), filter tickets by status, implement Firebase Cloud Messaging for real-time push notifications when ticket status changes.

Person 3: Authentication Backend
Creates: AuthRepository.java, AuthResult.java
Responsibilities: Implement Supabase Auth integration (sign up, login, logout), manage session tokens with SharedPreferences, handle role-based authentication (citizen/engineer), provide user ID and role retrieval methods for other modules.

Person 4: Report Issue Form + AI Backend
Creates: AIService.java, AIAnalysisResult.java, TicketRepository.java
Responsibilities: Integrate OpenAI GPT-4 Vision and Google Gemini APIs for image analysis, implement AI consensus logic (type/severity detection), handle photo upload to Supabase Storage, create tickets in database with GPS coordinates and AI confidence scores.

Person 5: Ticket Components + Location Service
Creates: LocationService.java
Updates: Ticket.java (add backend fields), TicketAdapter.java (Glide integration)
Responsibilities: Update Ticket model with backend-compatible fields (userId, photoUrl, locationLat/Lng, aiConfidence, createdAt, updatedAt), replace local image loading with Glide for remote URLs, implement GPS location service with FusedLocationProviderClient, handle location permissions.

Person 6: Core Infrastructure + Chatbot + Dependencies
Creates: NetworkModule.java, ChatbotService.java, ChatMessage.java
Updates: build.gradle.kts, local.properties
Responsibilities: Set up Supabase client and Retrofit with authentication interceptor, implement OpenAI ChatGPT chatbot service with conversation history, configure all project dependencies (Retrofit, Glide, Firebase, Location Services), create Supabase database schema (users, tickets, ticket_actions tables with RLS policies), set up Supabase Storage bucket for photos, manage API keys configuration.

## **DETAILED BACKEND WORK ALLOCATION (All in One, Separate Files)**

---

### **Person 1: Engineer Dashboard Backend**

**Owns:** EngineerDashboardActivity.java, activity_engineer_dashboard.xml

#### **Backend File to Create: `DashboardRepository.java`**
**Location:** `app/src/main/java/com/example/infrastructureproject/DashboardRepository.java`

**Detailed Tasks:**

1. **Create `DashboardRepository.java` class:**
```java
public class DashboardRepository {
    private SupabaseClient supabaseClient;
    
    public DashboardRepository(SupabaseClient client) {
        this.supabaseClient = client;
    }
    
    // Method 1: Get statistics for stat cards
    public DashboardStats getStatistics() {
        // Query Supabase tickets table:
        // - Count tickets with created_at = today → "New Today"
        // - Count tickets with created_at in last 7 days → "This Week"
        // - Count tickets where severity = "High" AND status = "PENDING" → "High Priority"
        // - Calculate avg time between created_at and first engineer action → "Avg Response"
        // Return DashboardStats object with these 4 values
    }
    
    // Method 2: Fetch tickets by status (for tabs)
    public List<Ticket> getTicketsByStatus(String status) {
        // Query: SELECT * FROM tickets WHERE status = ? ORDER BY created_at DESC
        // Convert JSON response to List<Ticket>
        // Return list
    }
    
    // Method 3: Search and filter tickets
    public List<Ticket> searchTickets(String query, String typeFilter, String severityFilter) {
        // Build dynamic query:
        // SELECT * FROM tickets WHERE 
        //   (description ILIKE '%query%' OR location ILIKE '%query%')
        //   AND (type = typeFilter OR typeFilter = 'All Types')
        //   AND (severity = severityFilter OR severityFilter = 'All Severities')
        // Return filtered list
    }
    
    // Method 4: Accept ticket
    public boolean acceptTicket(String ticketId, String engineerId) {
        // UPDATE tickets SET status = 'ACCEPTED', updated_at = NOW() WHERE id = ?
        // INSERT INTO ticket_actions (ticket_id, engineer_id, action_type, timestamp)
        //   VALUES (?, ?, 'ACCEPT', NOW())
        // Return true if successful
    }
    
    // Method 5: Reject ticket
    public boolean rejectTicket(String ticketId, String engineerId) {
        // UPDATE tickets SET status = 'REJECTED', updated_at = NOW() WHERE id = ?
        // Log action to ticket_actions table
        // Return success boolean
    }
    
    // Method 6: Mark as spam
    public boolean markAsSpam(String ticketId, String engineerId) {
        // UPDATE tickets SET status = 'SPAM', updated_at = NOW() WHERE id = ?
        // Log action to ticket_actions table
        // Return success boolean
    }
}
```

2. **Create `DashboardStats.java` model:**
**Location:** `app/src/main/java/com/example/infrastructureproject/DashboardStats.java`
```java
public class DashboardStats {
    private int newToday;
    private int thisWeek;
    private String avgResponse;  // e.g., "< 2 hours"
    private int highPriority;
    
    // Constructor, getters, setters
}
```

3. **Integrate into EngineerDashboardActivity.java:**
   - In `loadDashboardData()` method:
     - Replace `updateStatisticsFromTickets()` with:
       ```java
       DashboardRepository repo = new DashboardRepository(NetworkModule.getSupabaseClient());
       DashboardStats stats = repo.getStatistics();
       tvStatNewTodayValue.setText(String.valueOf(stats.getNewToday()));
       tvStatThisWeekValue.setText(String.valueOf(stats.getThisWeek()));
       tvStatAvgResponseValue.setText(stats.getAvgResponse());
       tvStatHighPriorityValue.setText(String.valueOf(stats.getHighPriority()));
       ```
   - In `selectTab()` method:
     - Replace `pendingTickets`, `acceptedTickets`, etc. with:
       ```java
       List<Ticket> tickets = repo.getTicketsByStatus("PENDING"); // or "ACCEPTED", "REJECTED", "SPAM"
       ticketAdapter.setTickets(tickets);
       ```
   - In `filterTickets()` method:
     - Call:
       ```java
       List<Ticket> filtered = repo.searchTickets(searchQuery, selectedType, selectedSeverity);
       ticketAdapter.setTickets(filtered);
       ```
   - In `onAccept()` callback:
     - Call:
       ```java
       boolean success = repo.acceptTicket(ticket.getId(), getCurrentEngineerId());
       if (success) {
           Toast.makeText(this, "Ticket accepted", Toast.LENGTH_SHORT).show();
           refreshDashboard();
       }
       ```
   - Same for `onReject()` and `onSpam()`

4. **Add loading spinners:**
   - Show `ProgressBar` while API calls are in progress
   - Hide when data arrives or error occurs

**Files Modified:**
- ✅ NEW: `DashboardRepository.java`
- ✅ NEW: `DashboardStats.java`
- ✅ EDIT: EngineerDashboardActivity.java (your existing file)

---

### **Person 2: Citizen Dashboard Backend**

**Owns:** MainActivity.java, activity_main.xml

#### **Backend Files to Create:**

**1. `CitizenRepository.java`**
**Location:** `app/src/main/java/com/example/infrastructureproject/CitizenRepository.java`

```java
public class CitizenRepository {
    private SupabaseClient supabaseClient;
    
    public CitizenRepository(SupabaseClient client) {
        this.supabaseClient = client;
    }
    
    // Method 1: Get citizen's own tickets
    public List<Ticket> getMyTickets(String userId) {
        // Query: SELECT * FROM tickets WHERE user_id = ? ORDER BY created_at DESC
        // Convert response to List<Ticket>
        // Return list
    }
    
    // Method 2: Get statistics for summary cards
    public CitizenStats getMyStatistics(String userId) {
        // Query tickets WHERE user_id = ?
        // Count total tickets
        // Count where status = 'PENDING'
        // Count where status = 'ACCEPTED'
        // Count where status = 'REJECTED'
        // Return CitizenStats object
    }
    
    // Method 3: Filter tickets by status (local filtering)
    public List<Ticket> filterTicketsByStatus(List<Ticket> allTickets, String status) {
        // Filter in-memory: allTickets.stream().filter(t -> t.getStatus().equals(status))
        // Return filtered list
    }
}
```

**2. `CitizenStats.java` model:**
**Location:** `app/src/main/java/com/example/infrastructureproject/CitizenStats.java`
```java
public class CitizenStats {
    private int totalReports;
    private int pending;
    private int accepted;
    private int rejected;
    
    // Constructor, getters, setters
}
```

**3. `FCMService.java` (Firebase Cloud Messaging):**
**Location:** `app/src/main/java/com/example/infrastructureproject/FCMService.java`
```java
public class FCMService extends FirebaseMessagingService {
    
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Receive push notification when ticket status changes
        // Extract: ticketId, newStatus, engineerComment from remoteMessage.getData()
        // Show notification:
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "ticket_updates")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Ticket Status Updated")
            .setContentText("Your ticket #" + ticketId + " is now " + newStatus)
            .setPriority(NotificationCompat.PRIORITY_HIGH);
        
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(ticketId.hashCode(), builder.build());
        
        // If app is open, refresh MainActivity
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("TICKET_UPDATED"));
    }
    
    @Override
    public void onNewToken(String token) {
        // Send FCM token to Supabase to link with user account
        // Store token in users table: UPDATE users SET fcm_token = ? WHERE id = ?
    }
}
```

4. **Integrate into MainActivity.java:**
   - In `onCreate()`:
     ```java
     CitizenRepository repo = new CitizenRepository(NetworkModule.getSupabaseClient());
     String userId = AuthRepository.getCurrentUserId();
     
     // Fetch tickets
     List<Ticket> myTickets = repo.getMyTickets(userId);
     // Display in RecyclerView (you'll need to add RecyclerView to layout)
     
     // Fetch stats
     CitizenStats stats = repo.getMyStatistics(userId);
     totalReportsNumber.setText(String.valueOf(stats.getTotalReports()));
     card2Number.setText(String.valueOf(stats.getPending()));
     card3Number.setText(String.valueOf(stats.getAccepted()));
     card4Number.setText(String.valueOf(stats.getRejected()));
     ```
   - Add click listeners to summary cards:
     ```java
     cardTotalReports.setOnClickListener(v -> {
         // Show all tickets
         displayTickets(myTickets);
     });
     
     card2.setOnClickListener(v -> {
         // Show only pending tickets
         List<Ticket> pending = repo.filterTicketsByStatus(myTickets, "PENDING");
         displayTickets(pending);
     });
     // Same for other cards
     ```
   - Register broadcast receiver for real-time updates:
     ```java
     LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             // Refresh tickets when push notification received
             refreshData();
         }
     }, new IntentFilter("TICKET_UPDATED"));
     ```

5. **Add Firebase setup:**
   - Download `google-services.json` from Firebase Console
   - Place in app directory
   - Add Firebase dependencies (Person 6 will add to build.gradle.kts)

**Files Modified:**
- ✅ NEW: `CitizenRepository.java`
- ✅ NEW: `CitizenStats.java`
- ✅ NEW: `FCMService.java`
- ✅ EDIT: MainActivity.java (your existing file)
- ✅ EDIT: AndroidManifest.xml (add FCMService declaration)

---

### **Person 3: Authentication Backend**

**Owns:** LoginActivity.java, LoginMainActivity.java, activity_login.xml, activity_sign_up.xml

#### **Backend File to Create: `AuthRepository.java`**
**Location:** `app/src/main/java/com/example/infrastructureproject/AuthRepository.java`

```java
public class AuthRepository {
    private SupabaseClient supabaseClient;
    private SharedPreferences prefs;
    
    public AuthRepository(Context context, SupabaseClient client) {
        this.supabaseClient = client;
        this.prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
    }
    
    // Method 1: Sign up new user
    public AuthResult signUp(String fullName, String email, String password, String role) {
        try {
            // Call Supabase Auth API:
            // POST to /auth/v1/signup
            // Body: { email: email, password: password, 
            //         data: { full_name: fullName, role: role } }
            
            // On success:
            // - Extract user ID and session token from response
            // - Insert into users table: INSERT INTO users (id, email, full_name, role) VALUES (...)
            // - Save session locally
            
            AuthResult result = new AuthResult();
            result.setSuccess(true);
            result.setUserId(userId);
            result.setRole(role);
            result.setToken(sessionToken);
            
            saveSession(userId, role, sessionToken);
            return result;
            
        } catch (Exception e) {
            // Return error result
            AuthResult result = new AuthResult();
            result.setSuccess(false);
            result.setError(e.getMessage());
            return result;
        }
    }
    
    // Method 2: Login existing user
    public AuthResult login(String email, String password) {
        try {
            // Call Supabase Auth API:
            // POST to /auth/v1/token?grant_type=password
            // Body: { email: email, password: password }
            
            // On success:
            // - Extract user ID, session token
            // - Query users table to get role: SELECT role FROM users WHERE id = ?
            
            AuthResult result = new AuthResult();
            result.setSuccess(true);
            result.setUserId(userId);
            result.setRole(role);
            result.setToken(sessionToken);
            
            saveSession(userId, role, sessionToken);
            return result;
            
        } catch (Exception e) {
            AuthResult result = new AuthResult();
            result.setSuccess(false);
            result.setError(e.getMessage());
            return result;
        }
    }
    
    // Method 3: Logout
    public void logout() {
        // Clear SharedPreferences
        prefs.edit().clear().apply();
        
        // Call Supabase: POST to /auth/v1/logout (optional, depends on token strategy)
    }
    
    // Method 4: Check if user is logged in
    public boolean isLoggedIn() {
        return prefs.contains("session_token") && prefs.contains("user_id");
    }
    
    // Method 5: Get current user info
    public static String getCurrentUserId() {
        return prefs.getString("user_id", null);
    }
    
    public static String getCurrentUserRole() {
        return prefs.getString("role", null);
    }
    
    // Helper: Save session to SharedPreferences
    private void saveSession(String userId, String role, String token) {
        prefs.edit()
            .putString("user_id", userId)
            .putString("role", role)
            .putString("session_token", token)
            .apply();
    }
}
```

**Create `AuthResult.java` model:**
**Location:** `app/src/main/java/com/example/infrastructureproject/AuthResult.java`
```java
public class AuthResult {
    private boolean success;
    private String userId;
    private String role;
    private String token;
    private String error;
    
    // Constructor, getters, setters
}
```

**Integrate into LoginActivity.java:**
```java
private void handleLogin() {
    String email = emailInput.getText().toString().trim();
    String password = passwordInput.getText().toString().trim();
    
    if (email.isEmpty() || password.isEmpty()) {
        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
        return;
    }
    
    // Show loading spinner
    ProgressBar loading = findViewById(R.id.loading);
    loading.setVisibility(View.VISIBLE);
    
    // Call AuthRepository
    AuthRepository authRepo = new AuthRepository(this, NetworkModule.getSupabaseClient());
    
    // Run in background thread (use AsyncTask or Coroutines)
    new Thread(() -> {
        AuthResult result = authRepo.login(email, password);
        
        runOnUiThread(() -> {
            loading.setVisibility(View.GONE);
            
            if (result.isSuccess()) {
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                
                // Navigate based on role
                Intent intent;
                if (result.getRole().equals("engineer")) {
                    intent = new Intent(this, EngineerDashboardActivity.class);
                } else {
                    intent = new Intent(this, MainActivity.class);
                }
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Login failed: " + result.getError(), Toast.LENGTH_SHORT).show();
            }
        });
    }).start();
}
```

**Integrate into LoginMainActivity.java (sign-up):**
```java
private void handleSignUp() {
    String fullName = fullNameInput.getText().toString().trim();
    String email = emailInput.getText().toString().trim();
    String password = passwordInput.getText().toString().trim();
    
    // Validation...
    
    AuthRepository authRepo = new AuthRepository(this, NetworkModule.getSupabaseClient());
    
    new Thread(() -> {
        AuthResult result = authRepo.signUp(fullName, email, password, selectedRole);
        
        runOnUiThread(() -> {
            if (result.isSuccess()) {
                Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                
                // Auto-login and navigate
                Intent intent;
                if (result.getRole().equals("engineer")) {
                    intent = new Intent(this, EngineerDashboardActivity.class);
                } else {
                    intent = new Intent(this, MainActivity.class);
                }
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Sign up failed: " + result.getError(), Toast.LENGTH_SHORT).show();
            }
        });
    }).start();
}
```

**Add logout buttons:**
- In MainActivity.java and EngineerDashboardActivity.java:
```java
logoutButton.setOnClickListener(v -> {
    AuthRepository authRepo = new AuthRepository(this, NetworkModule.getSupabaseClient());
    authRepo.logout();
    
    Intent intent = new Intent(this, LoginMainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
});
```

**Files Modified:**
- ✅ NEW: `AuthRepository.java`
- ✅ NEW: `AuthResult.java`
- ✅ EDIT: LoginActivity.java (your existing file)
- ✅ EDIT: LoginMainActivity.java (your existing file)

---

### **Person 4: Report Issue Form + AI Backend**

**Owns:** ReportIssueActivity.java, report_issue.xml

#### **Backend Files to Create:**

**1. `AIService.java`**
**Location:** `app/src/main/java/com/example/infrastructureproject/AIService.java`

```java
public class AIService {
    private String openAIApiKey;
    private String geminiApiKey;
    
    public AIService() {
        // Read API keys from local.properties or BuildConfig
        this.openAIApiKey = BuildConfig.OPENAI_API_KEY;
        this.geminiApiKey = BuildConfig.GEMINI_API_KEY;
    }
    
    // Method 1: Analyze image with OpenAI GPT-4 Vision
    public AIAnalysisResult analyzeWithOpenAI(Bitmap photo) {
        try {
            // Convert Bitmap to Base64 string
            String base64Image = bitmapToBase64(photo);
            
            // Build request to OpenAI API:
            // POST to https://api.openai.com/v1/chat/completions
            // Headers: { "Authorization": "Bearer " + openAIApiKey, "Content-Type": "application/json" }
            // Body: {
            //   "model": "gpt-4-vision-preview",
            //   "messages": [{
            //     "role": "user",
            //     "content": [
            //       { "type": "text", "text": "Analyze this infrastructure image. Return JSON: {\"isValid\": true/false, \"type\": \"Road\"|\"Utilities\"|\"Facilities\"|\"Environment\"|\"Other\", \"severity\": \"Low\"|\"Medium\"|\"High\", \"confidence\": 0-100, \"reason\": \"brief explanation\"}" },
            //       { "type": "image_url", "image_url": { "url": "data:image/jpeg;base64," + base64Image } }
            //     ]
            //   }],
            //   "max_tokens": 300
            // }
            
            // Parse response JSON:
            // Extract isValid, type, severity, confidence, reason
            
            AIAnalysisResult result = new AIAnalysisResult();
            result.setValid(isValid);
            result.setType(type);
            result.setSeverity(severity);
            result.setConfidence(confidence);
            result.setReason(reason);
            result.setSource("OpenAI");
            
            return result;
            
        } catch (Exception e) {
            AIAnalysisResult error = new AIAnalysisResult();
            error.setValid(false);
            error.setError(e.getMessage());
            return error;
        }
    }
    
    // Method 2: Verify with Google Gemini
    public AIAnalysisResult analyzeWithGemini(Bitmap photo) {
        try {
            // Similar to OpenAI but call Gemini API:
            // POST to https://generativelanguage.googleapis.com/v1/models/gemini-pro-vision:generateContent?key={apiKey}
            // Body: {
            //   "contents": [{
            //     "parts": [
            //       { "text": "Analyze this infrastructure image. Return JSON: {...}" },
            //       { "inline_data": { "mime_type": "image/jpeg", "data": base64Image } }
            //     ]
            //   }]
            // }
            
            // Parse and return result
            
        } catch (Exception e) {
            // Return error result
        }
    }
    
    // Method 3: Get consensus from both AIs
    public AIAnalysisResult getConsensus(AIAnalysisResult openAI, AIAnalysisResult gemini) {
        // If both say valid, use the one with higher confidence
        // If one says invalid, mark as low confidence
        // Average confidence scores
        // Prefer matching type/severity; if different, show both as options
        
        AIAnalysisResult consensus = new AIAnalysisResult();
        consensus.setValid(openAI.isValid() && gemini.isValid());
        
        if (openAI.getType().equals(gemini.getType())) {
            consensus.setType(openAI.getType());
        } else {
            consensus.setType(openAI.getType() + " or " + gemini.getType());
        }
        
        consensus.setConfidence((openAI.getConfidence() + gemini.getConfidence()) / 2);
        // ... more logic
        
        return consensus;
    }
    
    // Helper: Convert Bitmap to Base64
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteStream);
        byte[] bytes = byteStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
}
```

**Create `AIAnalysisResult.java` model:**
**Location:** `app/src/main/java/com/example/infrastructureproject/AIAnalysisResult.java`
```java
public class AIAnalysisResult {
    private boolean valid;
    private String type;        // "Road", "Utilities", etc.
    private String severity;    // "Low", "Medium", "High"
    private int confidence;     // 0-100
    private String reason;
    private String source;      // "OpenAI" or "Gemini"
    private String error;
    
    // Constructor, getters, setters
}
```

**2. `TicketRepository.java`**
**Location:** `app/src/main/java/com/example/infrastructureproject/TicketRepository.java`

```java
public class TicketRepository {
    private SupabaseClient supabaseClient;
    
    public TicketRepository(SupabaseClient client) {
        this.supabaseClient = client;
    }
    
    // Method 1: Upload photo to Supabase Storage
    public String uploadPhoto(Bitmap photo, String ticketId) {
        try {
            // Compress image to < 2MB
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            int quality = 90;
            photo.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            while (stream.size() > 2 * 1024 * 1024 && quality > 20) {
                stream.reset();
                quality -= 10;
                photo.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            }
            byte[] imageBytes = stream.toByteArray();
            
            // Upload to Supabase Storage:
            // POST to /storage/v1/object/ticket-photos/{ticketId}.jpg
            // Headers: { "Authorization": "Bearer " + sessionToken, "Content-Type": "image/jpeg" }
            // Body: imageBytes
            
            // Get public URL:
            String publicUrl = supabaseClient.storage()
                .from("ticket-photos")
                .getPublicUrl(ticketId + ".jpg");
            
            return publicUrl;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    // Method 2: Create new ticket
    public boolean createTicket(Ticket ticket) {
        try {
            // Insert into tickets table:
            // POST to /rest/v1/tickets
            // Body: {
            //   user_id: ticket.getUserId(),
            //   title: ticket.getTitle(),
            //   description: ticket.getDescription(),
            //   type: ticket.getType(),
            //   severity: ticket.getSeverity(),
            //   location_lat: ticket.getLocationLat(),
            //   location_lng: ticket.getLocationLng(),
            //   photo_url: ticket.getPhotoUrl(),
            //   status: "PENDING",
            //   ai_confidence: ticket.getAiConfidence(),
            //   created_at: NOW()
            // }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
}
```

**3. Integrate into ReportIssueActivity.java:**

```java
private Bitmap capturedPhoto;
private AIAnalysisResult aiResult;

// Step 1: Handle camera capture (you need to add camera permissions)
findViewById(R.id.card_take_photo).setOnClickListener(v -> {
    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
});

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
        capturedPhoto = (Bitmap) data.getExtras().get("data");
        
        // Show captured image in preview (add ImageView to layout)
        ImageView preview = findViewById(R.id.photo_preview);
        preview.setImageBitmap(capturedPhoto);
        preview.setVisibility(View.VISIBLE);
        
        // Analyze with AI
        analyzePhoto();
    }
}

// Step 2: Analyze photo with AI
private void analyzePhoto() {
    ProgressBar loading = findViewById(R.id.ai_loading);
    loading.setVisibility(View.VISIBLE);
    
    AIService aiService = new AIService();
    
    new Thread(() -> {
        // Call both AIs
        AIAnalysisResult openAI = aiService.analyzeWithOpenAI(capturedPhoto);
        AIAnalysisResult gemini = aiService.analyzeWithGemini(capturedPhoto);
        
        // Get consensus
        aiResult = aiService.getConsensus(openAI, gemini);
        
        runOnUiThread(() -> {
            loading.setVisibility(View.GONE);
            
            if (aiResult.isValid()) {
                // Pre-fill form fields
                EditText titleInput = findViewById(R.id.title_input);
                titleInput.setText("Report: " + aiResult.getType() + " issue");
                
                // Set type spinner
                Spinner typeSpinner = findViewById(R.id.type_spinner);
                // ... set selected item to aiResult.getType()
                
                // Set severity
                TextView severityBadge = findViewById(R.id.severity_badge);
                severityBadge.setText(aiResult.getSeverity());
                
                // Show AI confidence
                TextView aiConfidence = findViewById(R.id.ai_confidence);
                aiConfidence.setText("AI Confidence: " + aiResult.getConfidence() + "%");
                
                // Enable submit button
                Button submitButton = findViewById(R.id.submit_button);
                submitButton.setEnabled(true);
                
            } else {
                Toast.makeText(this, "AI could not validate this image. Please try another photo.", Toast.LENGTH_LONG).show();
            }
        });
    }).start();
}

// Step 3: Submit ticket
findViewById(R.id.submit_button).setOnClickListener(v -> {
    // Get GPS location (Person 5 will provide LocationService)
    LocationService locationService = new LocationService(this);
    locationService.getCurrentLocation(new LocationCallback() {
        @Override
        public void onLocationReceived(double lat, double lng) {
            submitTicket(lat, lng);
        }
        
        @Override
        public void onError(String error) {
            // Use default location or ask user to enter manually
            submitTicket(3.1390, 101.6869); // Default: Kuala Lumpur
        }
    });
});

private void submitTicket(double lat, double lng) {
    ProgressBar loading = findViewById(R.id.submit_loading);
    loading.setVisibility(View.VISIBLE);
    
    new Thread(() -> {
        // Step 1: Upload photo
        TicketRepository ticketRepo = new TicketRepository(NetworkModule.getSupabaseClient());
        String ticketId = UUID.randomUUID().toString();
        String photoUrl = ticketRepo.uploadPhoto(capturedPhoto, ticketId);
        
        if (photoUrl == null) {
            runOnUiThread(() -> {
                loading.setVisibility(View.GONE);
                Toast.makeText(this, "Photo upload failed", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        
        // Step 2: Create ticket object
        Ticket ticket = new Ticket(
            ticketId,
            aiResult.getType(),
            aiResult.getSeverity(),
            locationTextView.getText().toString(),
            descriptionInput.getText().toString(),
            new Date().toString(),
            photoUrl
        );
        ticket.setUserId(AuthRepository.getCurrentUserId());
        ticket.setLocationLat(lat);
        ticket.setLocationLng(lng);
        ticket.setAiConfidence(aiResult.getConfidence());
        
        // Step 3: Submit to backend
        boolean success = ticketRepo.createTicket(ticket);
        
        runOnUiThread(() -> {
            loading.setVisibility(View.GONE);
            
            if (success) {
                Toast.makeText(this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();
                
                // Navigate back to MainActivity
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Submission failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }).start();
}
```

**Add API keys to `local.properties`:**
```properties
openai.api.key=sk-your-openai-key-here
gemini.api.key=your-gemini-key-here
```

**Add to build.gradle.kts (Person 6 will do this):**
```kotlin
android {
    defaultConfig {
        buildConfigField("String", "OPENAI_API_KEY", "\"${project.findProperty("openai.api.key")}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${project.findProperty("gemini.api.key")}\"")
    }
}
```

**Files Modified:**
- ✅ NEW: `AIService.java`
- ✅ NEW: `AIAnalysisResult.java`
- ✅ NEW: `TicketRepository.java`
- ✅ EDIT: ReportIssueActivity.java (your existing file)
- ✅ EDIT: report_issue.xml (add photo preview ImageView, loading spinners)

---

### **Person 5: Ticket Components + Location Service**

**Owns:** TicketAdapter.java, Ticket.java, item_ticket.xml

#### **Backend Files to Create/Modify:**

**1. Update Ticket.java model:**
**Location:** Ticket.java

```java
public class Ticket implements Serializable {
    @SerializedName("id")
    private String id;
    
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("type")
    private String type;
    
    @SerializedName("severity")
    private String severity;
    
    @SerializedName("location")
    private String location;
    
    @SerializedName("location_lat")
    private double locationLat;
    
    @SerializedName("location_lng")
    private double locationLng;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("date_time")
    private String dateTime;
    
    @SerializedName("photo_url")  // CHANGED: was imageName, now remote URL
    private String photoUrl;
    
    @SerializedName("status")
    private TicketStatus status;
    
    @SerializedName("ai_confidence")
    private int aiConfidence;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("updated_at")
    private String updatedAt;
    
    // Keep existing enum
    public enum TicketStatus {
        PENDING,
        ACCEPTED,
        REJECTED,
        SPAM
    }
    
    // Add all getters/setters for new fields
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    
    public double getLocationLat() { return locationLat; }
    public void setLocationLat(double lat) { this.locationLat = lat; }
    
    public double getLocationLng() { return locationLng; }
    public void setLocationLng(double lng) { this.locationLng = lng; }
    
    public int getAiConfidence() { return aiConfidence; }
    public void setAiConfidence(int confidence) { this.aiConfidence = confidence; }
    
    // ... rest of existing getters/setters
}
```

**2. Update TicketAdapter.java:**
**Location:** TicketAdapter.java

```java
// In bind() method, replace local image loading:

// OLD CODE (remove this):
// int imageResource = context.getResources().getIdentifier(
//     ticket.getImageName(), "drawable", context.getPackageName());
// if (imageResource != 0) {
//     ivTicketImage.setImageResource(imageResource);
// }

// NEW CODE (add this):
Glide.with(context)
    .load(ticket.getPhotoUrl())
    .placeholder(R.drawable.ic_launcher_background)  // Shown while loading
    .error(R.drawable.ic_launcher_background)        // Shown if URL fails
    .centerCrop()
    .into(ivTicketImage);
```

**3. Create `LocationService.java`:**
**Location:** `app/src/main/java/com/example/infrastructureproject/LocationService.java`

```java
public class LocationService {
    private Context context;
    private FusedLocationProviderClient fusedLocationClient;
    
    public LocationService(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }
    
    // Interface for callback
    public interface LocationCallback {
        void onLocationReceived(double latitude, double longitude);
        void onError(String error);
    }
    
    // Method: Get current GPS location
    public void getCurrentLocation(LocationCallback callback) {
        // Check if permission granted
        if (ActivityCompat.checkSelfPermission(context, 
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            // Request permission (you need to handle this in Activity)
            callback.onError("Location permission not granted");
            return;
        }
        
        // Get last known location (fast)
        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(location -> {
                if (location != null) {
                    callback.onLocationReceived(location.getLatitude(), location.getLongitude());
                } else {
                    // Request fresh location
                    requestFreshLocation(callback);
                }
            })
            .addOnFailureListener(e -> {
                callback.onError(e.getMessage());
            });
    }
    
    // Helper: Request fresh location update
    private void requestFreshLocation(LocationCallback callback) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setNumUpdates(1);
        locationRequest.setInterval(0);
        
        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                    Location loc = locationResult.getLocations().get(0);
                    callback.onLocationReceived(loc.getLatitude(), loc.getLongitude());
                } else {
                    callback.onError("Could not get location");
                }
            }
        }, Looper.getMainLooper());
    }
    
    // Helper: Check if permission granted
    public boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    // Helper: Request permission (call from Activity)
    public static void requestLocationPermission(Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(activity, 
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
            requestCode);
    }
}
```

**Add to AndroidManifest.xml:**
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

**Person 4 uses this in `ReportIssueActivity`:**
```java
// When submit button clicked:
LocationService locationService = new LocationService(this);

if (!locationService.hasLocationPermission()) {
    LocationService.requestLocationPermission(this, LOCATION_REQUEST_CODE);
    return;
}

locationService.getCurrentLocation(new LocationService.LocationCallback() {
    @Override
    public void onLocationReceived(double lat, double lng) {
        // Person 4's code continues here...
        submitTicket(lat, lng);
    }
    
    @Override
    public void onError(String error) {
        Toast.makeText(ReportIssueActivity.this, 
            "Location error: " + error, Toast.LENGTH_SHORT).show();
        // Use default location or let user enter manually
    }
});
```

**Files Modified:**
- ✅ EDIT: Ticket.java (add backend fields)
- ✅ EDIT: TicketAdapter.java (use Glide for remote images)
- ✅ NEW: `LocationService.java`
- ✅ EDIT: AndroidManifest.xml (add location permissions)

---

### **Person 6: Core Infrastructure + Chatbot + Dependencies**

**No specific UI ownership — supports all**

#### **Backend Files to Create:**

**1. `NetworkModule.java` (CRITICAL — everyone uses this)**
**Location:** `app/src/main/java/com/example/infrastructureproject/NetworkModule.java`

```java
public class NetworkModule {
    private static SupabaseClient supabaseClient;
    private static Retrofit retrofit;
    
    // Initialize Supabase client (call in Application.onCreate())
    public static void initialize(Context context) {
        String supabaseUrl = BuildConfig.SUPABASE_URL;
        String supabaseKey = BuildConfig.SUPABASE_ANON_KEY;
        
        supabaseClient = new SupabaseClient.Builder()
            .setUrl(supabaseUrl)
            .setKey(supabaseKey)
            .build();
        
        // Initialize Retrofit
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(new AuthInterceptor(context))  // Adds session token to headers
            .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build();
        
        retrofit = new Retrofit.Builder()
            .baseUrl(supabaseUrl + "/rest/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    }
    
    public static SupabaseClient getSupabaseClient() {
        return supabaseClient;
    }
    
    public static Retrofit getRetrofit() {
        return retrofit;
    }
}

// Auth Interceptor to add session token
class AuthInterceptor implements Interceptor {
    private Context context;
    
    public AuthInterceptor(Context context) {
        this.context = context;
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        
        // Get session token from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("session_token", null);
        
        if (token != null) {
            Request request = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .method(original.method(), original.body())
                .build();
            return chain.proceed(request);
        }
        
        return chain.proceed(original);
    }
}
```

**2. Create `ChatbotService.java`:**
**Location:** `app/src/main/java/com/example/infrastructureproject/ChatbotService.java`

```java
public class ChatbotService {
    private String openAIApiKey;
    private List<ChatMessage> conversationHistory;
    
    public ChatbotService() {
        this.openAIApiKey = BuildConfig.OPENAI_API_KEY;
        this.conversationHistory = new ArrayList<>();
        
        // Add system message
        conversationHistory.add(new ChatMessage(
            "system",
            "You are an AI assistant for an infrastructure reporting app in Malaysia. " +
            "Help users understand how to report broken roads, drains, water leaks, fallen trees, etc. " +
            "Answer questions about report statuses (Pending, Accepted, Rejected). " +
            "Be concise, helpful, and friendly. Respond in English or Malay as needed."
        ));
    }
    
    // Method: Send message and get response
    public ChatMessage sendMessage(String userMessage) {
        try {
            // Add user message to history
            conversationHistory.add(new ChatMessage("user", userMessage));
            
            // Build request to OpenAI ChatGPT API:
            // POST to https://api.openai.com/v1/chat/completions
            // Headers: { "Authorization": "Bearer " + openAIApiKey, "Content-Type": "application/json" }
            // Body: {
            //   "model": "gpt-4",
            //   "messages": conversationHistory,  // Array of {role: "user/assistant/system", content: "..."}
            //   "max_tokens": 150,
            //   "temperature": 0.7
            // }
            
            // Send HTTP request (use OkHttp or Retrofit)
            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                buildChatRequest()
            );
            
            Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + openAIApiKey)
                .post(body)
                .build();
            
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            
            // Parse JSON response
            JSONObject json = new JSONObject(responseBody);
            String botMessage = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
            
            // Add bot response to history
            ChatMessage botResponse = new ChatMessage("assistant", botMessage);
            conversationHistory.add(botResponse);
            
            return botResponse;
            
        } catch (Exception e) {
            return new ChatMessage("assistant", "Sorry, I encountered an error: " + e.getMessage());
        }
    }
    
    // Helper: Build JSON request body
    private String buildChatRequest() {
        JSONObject json = new JSONObject();
        json.put("model", "gpt-4");
        json.put("max_tokens", 150);
        json.put("temperature", 0.7);
        
        JSONArray messages = new JSONArray();
        for (ChatMessage msg : conversationHistory) {
            JSONObject msgObj = new JSONObject();
            msgObj.put("role", msg.getRole());
            msgObj.put("content", msg.getMessage());
            messages.put(msgObj);
        }
        json.put("messages", messages);
        
        return json.toString();
    }
    
    // Clear conversation history
    public void clearHistory() {
        conversationHistory.clear();
        // Re-add system message
        conversationHistory.add(new ChatMessage("system", "..."));
    }
}
```

**Create `ChatMessage.java` model:**
**Location:** `app/src/main/java/com/example/infrastructureproject/ChatMessage.java`

```java
public class ChatMessage {
    private String role;     // "user", "assistant", "system"
    private String message;
    private long timestamp;
    
    public ChatMessage(String role, String message) {
        this.role = role;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters, setters
}
```

**3. Person 1 & Person 2 integrate chatbot:**

**In MainActivity.java (Person 2) and EngineerDashboardActivity.java (Person 1):**

```java
// In popup_ai_assistant.xml, add these components:
// - chatContainer (LinearLayout to hold messages)
// - chatInput (EditText)
// - sendButton (ImageButton)

private ChatbotService chatbotService;
private LinearLayout chatContainer;
private EditText chatInput;

private void showAIAssistantPopup() {
    // ... existing popup code ...
    
    chatbotService = new ChatbotService();
    chatContainer = popupView.findViewById(R.id.chatContainer);
    chatInput = popupView.findViewById(R.id.chatInput);
    
    ImageButton sendButton = popupView.findViewById(R.id.sendButton);
    sendButton.setOnClickListener(v -> {
        String userMessage = chatInput.getText().toString().trim();
        if (userMessage.isEmpty()) return;
        
        // Add user message to chat UI
        addMessageToChat("user", userMessage);
        chatInput.setText("");
        
        // Get bot response
        new Thread(() -> {
            ChatMessage botResponse = chatbotService.sendMessage(userMessage);
            
            runOnUiThread(() -> {
                addMessageToChat("bot", botResponse.getMessage());
            });
        }).start();
    });
}

private void addMessageToChat(String sender, String message) {
    TextView messageView = new TextView(this);
    messageView.setText(message);
    messageView.setPadding(12, 12, 12, 12);
    messageView.setMaxWidth(400);
    
    if (sender.equals("user")) {
        messageView.setGravity(Gravity.END);
        messageView.setBackgroundColor(Color.parseColor("#E0E0E0"));
    } else {
        messageView.setGravity(Gravity.START);
        messageView.setBackgroundColor(Color.parseColor("#2196F3"));
        messageView.setTextColor(Color.WHITE);
    }
    
    chatContainer.addView(messageView);
    
    // Scroll to bottom
    ScrollView scrollView = findViewById(R.id.chatScrollView);
    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
}
```

**4. Update build.gradle.kts with ALL dependencies:**

**Location:** build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")  // For Firebase
}

android {
    namespace = "com.example.infrastructurereporter"
    compileSdk = 36
    
    defaultConfig {
        applicationId = "com.example.infrastructurereporter"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Add API keys from local.properties
        val properties = File(rootProject.rootDir, "local.properties")
            .inputStream().use { java.util.Properties().apply { load(it) } }
        
        buildConfigField("String", "SUPABASE_URL", "\"${properties.getProperty("supabase.url")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${properties.getProperty("supabase.anon.key")}\"")
        buildConfigField("String", "OPENAI_API_KEY", "\"${properties.getProperty("openai.api.key")}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${properties.getProperty("gemini.api.key")}\"")
    }
    
    buildFeatures {
        buildConfig = true
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Existing dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    
    // Supabase (use Java-compatible HTTP client instead)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Retrofit for REST APIs
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Image loading (Glide)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    
    // Location services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Firebase for push notifications
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    
    // RecyclerView (if not already added)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // CardView (if not already added)
    implementation("androidx.cardview:cardview:1.0.0")
}
```

**5. Create `.env` / `local.properties` template:**

**Location:** `local.properties` (add these lines, everyone fills in their keys)

```properties
# Supabase credentials (Person 6 provides after setup)
supabase.url=https://your-project.supabase.co
supabase.anon.key=your-anon-key-here

# OpenAI API key (Person 4 & Person 6 get from openai.com)
openai.api.key=sk-your-key-here

# Google Gemini API key (Person 4 gets from ai.google.dev)
gemini.api.key=your-gemini-key-here
```

**6. Set up Supabase database schema:**

**SQL to run in Supabase SQL Editor:**

```sql
-- Table: users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT UNIQUE NOT NULL,
    full_name TEXT,
    role TEXT CHECK (role IN ('citizen', 'engineer')),
    fcm_token TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: tickets
CREATE TABLE tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    title TEXT NOT NULL,
    description TEXT,
    type TEXT CHECK (type IN ('Road', 'Utilities', 'Facilities', 'Environment', 'Other')),
    severity TEXT CHECK (severity IN ('Low', 'Medium', 'High')),
    location TEXT,
    location_lat DOUBLE PRECISION,
    location_lng DOUBLE PRECISION,
    photo_url TEXT,
    status TEXT CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'SPAM')) DEFAULT 'PENDING',
    ai_confidence INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: ticket_actions (for audit trail)
CREATE TABLE ticket_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID REFERENCES tickets(id),
    engineer_id UUID REFERENCES users(id),
    action_type TEXT CHECK (action_type IN ('ACCEPT', 'REJECT', 'SPAM')),
    comment TEXT,
    timestamp TIMESTAMPTZ DEFAULT NOW()
);

-- Row Level Security (RLS) policies
ALTER TABLE tickets ENABLE ROW LEVEL SECURITY;

-- Policy: Citizens can only see their own tickets
CREATE POLICY "Citizens can view own tickets" ON tickets
    FOR SELECT USING (auth.uid() = user_id);

-- Policy: Engineers can see all tickets
CREATE POLICY "Engineers can view all tickets" ON tickets
    FOR SELECT USING (
        EXISTS (SELECT 1 FROM users WHERE id = auth.uid() AND role = 'engineer')
    );

-- Policy: Citizens can insert their own tickets
CREATE POLICY "Citizens can create tickets" ON tickets
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Policy: Engineers can update ticket status
CREATE POLICY "Engineers can update tickets" ON tickets
    FOR UPDATE USING (
        EXISTS (SELECT 1 FROM users WHERE id = auth.uid() AND role = 'engineer')
    );
```

**7. Create Supabase Storage bucket:**
- In Supabase dashboard → Storage → Create bucket: `ticket-photos`
- Set to **public** (read-only for everyone, write requires auth)

**Files Modified:**
- ✅ NEW: `NetworkModule.java`
- ✅ NEW: `ChatbotService.java`
- ✅ NEW: `ChatMessage.java`
- ✅ EDIT: build.gradle.kts (add all dependencies)
- ✅ EDIT: `local.properties` (add API keys template)
- ✅ NEW: Supabase SQL schema (run in Supabase console)

---

## **FINAL SUMMARY: WHO DOES WHAT**

| Person | New Files Created (No Conflicts) | Files Edited (Your Existing Code) | External Setup |
|--------|----------------------------------|-----------------------------------|----------------|
| **1** | `DashboardRepository.java`, `DashboardStats.java` | EngineerDashboardActivity.java | Supabase tickets table |
| **2** | `CitizenRepository.java`, `CitizenStats.java`, `FCMService.java` | MainActivity.java, AndroidManifest.xml | Firebase FCM |
| **3** | `AuthRepository.java`, `AuthResult.java` | LoginActivity.java, LoginMainActivity.java | Supabase Auth |
| **4** | `AIService.java`, `AIAnalysisResult.java`, `TicketRepository.java` | ReportIssueActivity.java, report_issue.xml | OpenAI API, Gemini API, Supabase Storage |
| **5** | `LocationService.java` | Ticket.java, TicketAdapter.java, AndroidManifest.xml | Google Play Services |
| **6** | `NetworkModule.java`, `ChatbotService.java`, `ChatMessage.java` | build.gradle.kts, `local.properties` | Supabase project setup, OpenAI ChatGPT |

**All work in SEPARATE FILES → minimal merge conflicts!**

---

Does this detailed breakdown work for your team? Everyone has clear tasks and files to create without stepping on each other's toes!
