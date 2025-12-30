package com.example.infrastructureproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.infrastructurereporter.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
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
import java.util.List;
import java.util.Locale;

import com.example.infrastructurereporter.BuildConfig;




public class ReportIssueFragment extends Fragment {

    private Spinner typeSpinner;
    private Spinner severitySpinner;
    private EditText descriptionEditText;
    private TextView locationTextView;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private FusedLocationProviderClient fusedLocationClient;
    private Uri selectedImageUri;
    private Uri capturedImageUri;
    private ImageView previewImageView;
    private ProgressBar loadingIndicator;
    private TextView loadingText;
    private Client client;
    private String reportIssuePrompt = "Analyze this infrastructure image and identify: 1) Issue type (Pothole, Broken Streetlight, Damaged Pipe, or Other), 2) Severity level (Low, Medium, or High), 3) Detailed description of the problem, 4) Estimated location or landmark visible in the image. Provide accurate infrastructure assessment.";
    
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

        
        // Setup dropdown spinners
        setupSpinner();
        
        // Setup image handlers for the upload and take photo cards to work
        setupImageHandlers();

        // initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        requestLocationPermission();
        View locationRow = rootView.findViewById(R.id.location_row);
        locationRow.setOnClickListener(v -> requestLocationPermission());

        
        return rootView;
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
                            callGeminiAPI(selectedImageUri, reportIssuePrompt);
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
                        callGeminiAPI(capturedImageUri, reportIssuePrompt);
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

//        register location permission launcher
        locationPermissionLauncher =registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result ->{
                    Boolean fineLocation = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocation = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if (fineLocation || coarseLocation){
                        getCurrentLocation();
                    }
                    else{
                        Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                    }

                }
        );
    }

    private void requestLocationPermission(){
        if(ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            getCurrentLocation();
        }
        else{
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressWarnings("MissingPermission")
    private void getCurrentLocation(){
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null){
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        getAddressFromLocation(latitude, longitude);
                    }
                    else{
                        Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e->{
                    Log.e("location","error getting location : "+ e.getMessage());
                    Toast.makeText(requireContext(),"failed to get location",Toast.LENGTH_SHORT).show();
                });
    }
//    method to convert latitude and longitude to a readable address
    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());

        try{
            List<Address> addresses = geocoder.getFromLocation(latitude,longitude,1);
            if (addresses != null  && !addresses.isEmpty()){
                Address address = addresses.get(0);

                StringBuilder addressText = new StringBuilder();
                for(int i = 0; i<= address.getMaxAddressLineIndex();i++){
                    addressText.append(address.getAddressLine(i));
                    if (i < address.getMaxAddressLineIndex()){
                        addressText.append(", ");
                    }
                }

                if(getActivity()!= null){
                    getActivity().runOnUiThread(()->{
                        locationTextView.setText(addressText.toString());
                    });
                }
            }
            else{
                String coords = String.format(Locale.getDefault(), "%.6f, %.6f", latitude, longitude);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> locationTextView.setText(coords));
                }
            }
        } catch (IOException e) {
            Log.e("geocode","error "+e.getMessage());
            String coords = String.format(Locale.getDefault(),"%.6f, %.6f",latitude,longitude);
            if(getActivity()!= null){
                getActivity().runOnUiThread(()-> locationTextView.setText(coords));
            }
        }
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
                       )
                   ),
                   "required", ImmutableList.of("issue_type", "severity", "description")
               );

               // response to use json schema
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
               
               // Parse json and autofill form
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

                    
                    Toast.makeText(requireContext(), 
                        "Form autofilled with AI analysis", 
                        Toast.LENGTH_SHORT).show();
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
}
