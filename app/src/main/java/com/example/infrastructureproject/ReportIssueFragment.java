package com.example.infrastructureproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.infrastructurereporter.R;
import com.example.infrastructureproject.models.Ticket;
import com.example.infrastructureproject.repository.TicketRepository;
import com.example.infrastructureproject.utils.LocationHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class ReportIssueFragment extends Fragment {

    private Spinner typeSpinner;
    private Spinner severitySpinner;
    private EditText descriptionEditText;
    private TextView locationTextView;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private Uri selectedImageUri;
    private Uri capturedImageUri;
    private ImageView previewImageView;
    private ProgressBar loadingIndicator;
    private TextView loadingText;
    private Button submitButton;
    private Client client;
    private String reportIssuePrompt = "Analyze this infrastructure image and identify: 1) Issue type (Pothole, Broken Streetlight, Damaged Pipe, or Other), 2) Severity level (Low, Medium, or High), 3) Detailed description of the problem, 4) Estimated location or landmark visible in the image. Provide accurate infrastructure assessment.";
    
    // Location tracking
    private Double currentLatitude;
    private Double currentLongitude;
    private boolean usingManualLocation = false;
    private LocationHelper locationHelper;
    private ActivityResultLauncher<String> locationPermissionLauncher;
    
    // Supabase
    private TicketRepository ticketRepository;
    private String userToken = "YOUR_USER_TOKEN_HERE"; // TODO: Get from auth system
    private String userId = "YOUR_USER_ID_HERE"; // TODO: Get from auth system
    
    private View rootView;

    public ReportIssueFragment() {
        // Required empty public constructor
    }

    public static ReportIssueFragment newInstance() {
        return new ReportIssueFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Gemini client
        String apiKey = BuildConfig.GEMINI_API_KEY;
        this.client = Client.builder()
                .apiKey(apiKey)
                .build();
        
        // Initialize repository and location helper
        this.ticketRepository = new TicketRepository();
        this.locationHelper = new LocationHelper(requireActivity());
        
        // Location permission launcher
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        fetchCurrentLocation();
                    } else {
                        Toast.makeText(requireContext(), "Location permission denied. Using default location.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.report_issue, container, false);
        
        previewImageView = rootView.findViewById(R.id.photo_preview);
        descriptionEditText = rootView.findViewById(R.id.description_input);
        locationTextView = rootView.findViewById(R.id.location_text);
        loadingIndicator = rootView.findViewById(R.id.ai_loading_indicator);
        loadingText = rootView.findViewById(R.id.ai_loading_text);
        submitButton = rootView.findViewById(R.id.submit_button);
        
        // Setup dropdown spinners
        setupSpinner();
        
        // Setup image handlers for the upload and take photo cards to work
        setupImageHandlers();
        // Setup submit button behavior
        setupSubmitButton();
        // Setup location handlers
        setupLocationHandlers();
        
        return rootView;
    }

    private void setupSubmitButton(){
        // Spinner selection changes affect submit button state
        if(typeSpinner != null){
            typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updateSubmitButtonState();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    updateSubmitButtonState();
                }
            });
        }

        if(severitySpinner != null){
            severitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updateSubmitButtonState();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    updateSubmitButtonState();
                }
            });
        }

        // Initial state
        updateSubmitButtonState();

        if(submitButton != null){
            submitButton.setOnClickListener(v -> {
                // Gather form data
                String issueType = (typeSpinner != null) ? typeSpinner.getSelectedItem().toString() : "";
                String severity = (severitySpinner != null) ? severitySpinner.getSelectedItem().toString() : "";
                String description = (descriptionEditText != null) ? descriptionEditText.getText().toString() : "";
                String location = (locationTextView != null) ? locationTextView.getText().toString() : "";

                // Simple validation (defensive)
                if(selectedImageUri == null || (typeSpinner != null && typeSpinner.getSelectedItemPosition() == 0) || (severitySpinner != null && severitySpinner.getSelectedItemPosition() == 0)){
                    Toast.makeText(requireContext(), "Please provide a photo, issue type and severity.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Submit to Supabase
                submitTicketToSupabase(issueType, severity, description, location);
            });
        }
    }

    private void updateSubmitButtonState(){
        boolean hasPhoto = selectedImageUri != null;
        boolean typeSelected = typeSpinner != null && typeSpinner.getSelectedItemPosition() > 0;
        boolean severitySelected = severitySpinner != null && severitySpinner.getSelectedItemPosition() > 0;

        boolean enabled = hasPhoto && typeSelected && severitySelected;
        if(submitButton != null){
            submitButton.setEnabled(enabled);
        }
    }

    private void setupSpinner() {
        typeSpinner = rootView.findViewById(R.id.type_spinner);
        severitySpinner = rootView.findViewById(R.id.severity_spinner);

//        dynamically shows the list of dropdown items
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.issue_types,
                R.layout.spinner_item
        );
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        ArrayAdapter<CharSequence> severityAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.severity_levels,
                R.layout.spinner_item
        );
        severityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        severitySpinner.setAdapter(severityAdapter);
    }

    private void setupImageHandlers() {
        View uploadCard = rootView.findViewById(R.id.card_upload_photo);
        View takePhotoCard = rootView.findViewById(R.id.card_take_photo);
        
        // Gallery permission launcher
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if(isGranted){
                        launchGalleryPicker();
                    }
                    else{
                        Toast.makeText(requireContext(), R.string.permission_required_message, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Camera permission launcher
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if(isGranted){
                        launchCamera();
                    }
                    else{
                        Toast.makeText(requireContext(), R.string.camera_permission_required_message, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Gallery picker launcher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null){
                        Uri uri = result.getData().getData();
                        if(uri != null){
                            selectedImageUri = uri;
                            showPreview(uri);
                            updateSubmitButtonState();
                            callGeminiAPI(selectedImageUri, reportIssuePrompt);
                            // Automatically fetch current location after selecting photo
                            autoFetchLocationAfterPhoto();
                        }
                    }
                }
        );

        // Camera capture launcher
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if(success && capturedImageUri != null){
                        selectedImageUri = capturedImageUri;
                        showPreview(capturedImageUri);
                        updateSubmitButtonState();
                        callGeminiAPI(capturedImageUri, reportIssuePrompt);
                        // Automatically fetch current location after taking photo
                        autoFetchLocationAfterPhoto();
                    }
                }
        );

        // Upload photo button
        uploadCard.setOnClickListener(
                v -> {
                    if(hasReadPermission()){
                        launchGalleryPicker();
                    }
                    else{
                        permissionLauncher.launch(getReadPermission());
                    }
                }
        );

        // Take photo button
        takePhotoCard.setOnClickListener(
                v -> {
                    if(hasCameraPermission()){
                        launchCamera();
                    }
                    else{
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                    }
                }
        );
    }

    private void launchGalleryPicker(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void showPreview(Uri uri){
        if (previewImageView != null) {
            previewImageView.setVisibility(View.VISIBLE);
            previewImageView.setImageURI(uri);
        }
    }

    private boolean hasReadPermission(){
        return ContextCompat.checkSelfPermission(requireContext(), getReadPermission()) == PackageManager.PERMISSION_GRANTED;
    }

    private String getReadPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private boolean hasCameraPermission(){
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void launchCamera(){
        try {
            capturedImageUri = createImageUri();
            if(capturedImageUri != null){
                takePictureLauncher.launch(capturedImageUri);
            }
        } catch (Exception e) {
            Log.e("Camera", "Error launching camera: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Failed to open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri createImageUri(){
        try {
            java.io.File imageFile = new java.io.File(requireContext().getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
            String authority = requireContext().getPackageName() + ".fileprovider";
            return FileProvider.getUriForFile(requireContext(), authority, imageFile);
        } catch (Exception e) {
            Log.e("Camera", "Error creating image URI: " + e.getMessage(), e);
            return null;
        }
    }

    private void callGeminiAPI(Uri imageUri, String prompt){
        // Show loading indicator
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if(loadingIndicator != null) loadingIndicator.setVisibility(View.VISIBLE);
                if(loadingText != null) loadingText.setVisibility(View.VISIBLE);
            });
        }

        new Thread(() ->{
           try{
               String modelId = "gemini-2.5-flash-lite";
               byte[] imgBytes = getBytesFromUri(imageUri);

               // json schema for structured output for parsing and auto fill in to the ui
               ImmutableMap<String, Object> schema = ImmutableMap.of(
                   "type", "object",
                   "properties", ImmutableMap.of(
                       "issue_type", ImmutableMap.of(
                           "type", "string",
                           "enum", ImmutableList.of("Pothole", "Broken Streetlight", "Damaged Pipe", "Other")
                       ),
                       "severity", ImmutableMap.of(
                           "type", "string",
                           "enum", ImmutableList.of("Low", "Medium", "High")
                       ),
                       "description", ImmutableMap.of(
                           "type", "string",
                           "description", "Detailed description of the infrastructure issue"
                       ),
                       "location", ImmutableMap.of(
                           "type", "string",
                           "description", "Location or landmark visible in the image"
                       )
                   ),
                   "required", ImmutableList.of("issue_type", "severity", "description", "location")
               );

               // response to use JSON schema
               GenerateContentConfig config = GenerateContentConfig.builder()
                       .responseMimeType("application/json")
                       .candidateCount(1)
                       .responseJsonSchema(schema)
                       .build();

               // Concat the prompt together with the image data in content object
               Content content = Content.fromParts(
                   Part.fromText(prompt),
                   Part.fromBytes(imgBytes, "image/jpeg")
               );
               
               GenerateContentResponse response = client.models.generateContent(
                   modelId,
                   content,
                   config
               );
               
               String jsonResponse = response.text();
               Log.d("GeminiAPI", "Response: " + jsonResponse);
               
               // Hide loading indicator
               if (getActivity() != null) {
                   getActivity().runOnUiThread(() -> {
                       if(loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                       if(loadingText != null) loadingText.setVisibility(View.GONE);
                   });
               }
               
               // Parse JSON and autofill form
               parseAndAutofillForm(jsonResponse);
           }
           catch (java.net.UnknownHostException | java.net.SocketTimeoutException e){
               Log.e("GeminiAPI", "Network error: " + e.getMessage(), e);
               if (getActivity() != null) {
                   getActivity().runOnUiThread(() -> {
                       if(loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                       if(loadingText != null) loadingText.setVisibility(View.GONE);
                       Toast.makeText(requireContext(), 
                           "Network error: Please check your internet connection", 
                           Toast.LENGTH_LONG).show();
                   });
               }
           }
           catch (Exception e){
               Log.e("GeminiAPI", "Error calling Gemini API: " + e.getMessage(), e);
               e.printStackTrace();
               if (getActivity() != null) {
                   getActivity().runOnUiThread(() -> {
                       if(loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                       if(loadingText != null) loadingText.setVisibility(View.GONE);
                       Toast.makeText(requireContext(), 
                           "AI analysis failed: " + e.getMessage(), 
                           Toast.LENGTH_LONG).show();
                   });
               }
           }
        }).start();
    }

    private void parseAndAutofillForm(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            
            String issueType = json.optString("issue_type", "");
            String severity = json.optString("severity", "");
            String description = json.optString("description", "");
            String location = json.optString("location", "");
            
            Log.d("GeminiAPI", "Parsed - Type: " + issueType + ", Severity: " + severity);
            
            // Update UI on main thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Set issue type spinner
                    if (!issueType.isEmpty()) {
                        int typePosition = getSpinnerPosition(typeSpinner, issueType);
                        if (typePosition >= 0) {
                            typeSpinner.setSelection(typePosition);
                        }
                    }
                    
                    // Set severity spinner
                    if (!severity.isEmpty()) {
                        int severityPosition = getSpinnerPosition(severitySpinner, severity);
                        if (severityPosition >= 0) {
                            severitySpinner.setSelection(severityPosition);
                        }
                    }
                    
                    // Set description
                    if (!description.isEmpty() && descriptionEditText != null) {
                        descriptionEditText.setText(description);
                    }
                    
                    // Set location
                    if (!location.isEmpty() && locationTextView != null) {
                        locationTextView.setText(location);
                    }
                    
                    Toast.makeText(requireContext(), 
                        "Form autofilled with AI analysis", 
                        Toast.LENGTH_SHORT).show();
                    // Update submit button state in case AI filled required fields
                    updateSubmitButtonState();
                });
            }
            
        } catch (JSONException e) {
            Log.e("GeminiAPI", "Error parsing JSON: " + e.getMessage(), e);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> 
                    Toast.makeText(requireContext(), 
                        "Failed to parse AI response", 
                        Toast.LENGTH_SHORT).show()
                );
            }
        }
    }
    
    private int getSpinnerPosition(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0; // Return 0 (placeholder position) if not found
    }

    // Method to get image data after user uploaded the image
    private byte[] getBytesFromUri(Uri uri) throws Exception{
        InputStream iStream = requireContext().getContentResolver().openInputStream(uri);
        try{
            assert iStream != null;
            return getBytes(iStream);
        }
        finally {
            try{
                assert iStream != null;
                iStream.close();
            }
            catch (Exception ignored){

            }
        }
    }

    private byte[] getBytes(InputStream iStream) throws IOException {
        byte[] bytesRes = null;
        try (ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream()){
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len ;
            while((len = iStream.read(buffer)) != -1){
                byteBuffer.write(buffer,0,len);
            }
            bytesRes = byteBuffer.toByteArray();
        }
        return bytesRes;
    }
    
    // ========== LOCATION HANDLING ==========
    
    private void setupLocationHandlers() {
        TextView enterManuallyBtn = rootView.findViewById(R.id.enter_manually);
        
        if (enterManuallyBtn != null) {
            enterManuallyBtn.setOnClickListener(v -> {
                // Show dialog to enter location manually
                showManualLocationDialog();
            });
        }
        
        // Don't auto-fetch location on load, wait for photo to be taken
        // Just show a placeholder message
        if (locationTextView != null) {
            locationTextView.setText("Location will be detected after taking a photo");
        }
    }
    
    private void fetchCurrentLocation() {
        locationHelper.getCurrentLocation(new LocationHelper.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude, String address) {
                currentLatitude = latitude;
                currentLongitude = longitude;
                usingManualLocation = false;
                
                if (locationTextView != null) {
                    locationTextView.setText(address);
                }
                Toast.makeText(requireContext(), "Location updated", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onLocationError(String error) {
                Log.e("ReportIssue", "Location error: " + error);
                // Fallback to default
                locationTextView.setText("Default Location (Kuala Lumpur)");
                currentLatitude = 3.1390;
                currentLongitude = 101.6869;
            }
        });
    }
    
    private void autoFetchLocationAfterPhoto() {
        if (LocationHelper.hasLocationPermission(requireActivity())) {
            // Permission already granted, fetch location
            locationHelper.getCurrentLocation(new LocationHelper.LocationCallback() {
                @Override
                public void onLocationReceived(double latitude, double longitude, String address) {
                    currentLatitude = latitude;
                    currentLongitude = longitude;
                    usingManualLocation = false;
                    
                    if (locationTextView != null) {
                        locationTextView.setText(address);
                    }
                }
                
                @Override
                public void onLocationError(String error) {
                    Log.e("ReportIssue", "Location error: " + error);
                    if (locationTextView != null) {
                        locationTextView.setText("Unable to get current location");
                    }
                }
            });
        } else {
            // Request permission
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }
    
    private void showManualLocationDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Enter Location");
        
        final EditText input = new EditText(requireContext());
        input.setHint("e.g., Jalan Gasing, Petaling Jaya");
        builder.setView(input);
        
        builder.setPositiveButton("OK", (dialog, which) -> {
            String manualLocation = input.getText().toString().trim();
            if (!manualLocation.isEmpty()) {
                locationTextView.setText(manualLocation);
                usingManualLocation = true;
                // Clear GPS coordinates when manual
                currentLatitude = null;
                currentLongitude = null;
                Toast.makeText(requireContext(), "Manual location set", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    
    // ========== SUPABASE SUBMISSION ==========
    
    private void submitTicketToSupabase(String issueType, String severity, String description, String location) {
        // Disable button during submission
        submitButton.setEnabled(false);
        
        // Show loading
        if (loadingIndicator != null) loadingIndicator.setVisibility(View.VISIBLE);
        if (loadingText != null) {
            loadingText.setText("Submitting report...");
            loadingText.setVisibility(View.VISIBLE);
        }
        
        // Run in background thread
        new Thread(() -> {
            try {
                // Create ticket object
                Ticket ticket = new Ticket(
                    userId,
                    issueType,
                    severity,
                    location,
                    currentLatitude,
                    currentLongitude,
                    description
                );
                
                // Insert ticket to database
                Ticket createdTicket = ticketRepository.createTicket(ticket, userToken);
                
                // Upload image
                String imagePath = ticketRepository.uploadImage(
                    selectedImageUri, 
                    createdTicket.getId(), 
                    userToken, 
                    requireContext()
                );
                
                // Save image metadata
                String filename = imagePath.substring(imagePath.lastIndexOf("/") + 1);
                ticketRepository.saveImageMetadata(
                    createdTicket.getId(),
                    imagePath,
                    filename,
                    userId,
                    userToken
                );
                
                // Success - update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                        if (loadingText != null) loadingText.setVisibility(View.GONE);
                        
                        Toast.makeText(requireContext(), 
                            "Report submitted successfully! Ticket: " + createdTicket.getTicketId(), 
                            Toast.LENGTH_LONG).show();
                        
                        // Clear form
                        clearForm();
                        submitButton.setEnabled(true);
                    });
                }
                
            } catch (Exception e) {
                Log.e("ReportIssue", "Submit failed: " + e.getMessage(), e);
                
                // Show error on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                        if (loadingText != null) loadingText.setVisibility(View.GONE);
                        
                        Toast.makeText(requireContext(), 
                            "Failed to submit: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                        
                        submitButton.setEnabled(true);
                    });
                }
            }
        }).start();
    }
    
    private void clearForm() {
        // Reset form after successful submission
        if (typeSpinner != null) typeSpinner.setSelection(0);
        if (severitySpinner != null) severitySpinner.setSelection(0);
        if (descriptionEditText != null) descriptionEditText.setText("");
        if (previewImageView != null) previewImageView.setVisibility(View.GONE);
        selectedImageUri = null;
        updateSubmitButtonState();
    }
}
