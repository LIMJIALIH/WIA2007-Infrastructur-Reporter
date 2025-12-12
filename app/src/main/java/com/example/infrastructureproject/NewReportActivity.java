package com.example.infrastructureproject;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NewReportActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    // UI Elements
    private com.google.android.material.card.MaterialCardView cardTakePhoto;
    private com.google.android.material.card.MaterialCardView cardUploadPhoto;
    private TextView locationText;
    private EditText descriptionInput;
    private Button submitButton;
    private ImageView previewImageView;

    // Data
    private Bitmap selectedImage = null;
    private String currentLocation = "Default Location (Kuala Lumpur)";
    private String imageName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_issue);

        initializeViews();
        setupListeners();
        checkSubmitButtonState();
    }

    private void initializeViews() {
        cardTakePhoto = findViewById(R.id.card_take_photo);
        cardUploadPhoto = findViewById(R.id.card_upload_photo);
        locationText = findViewById(R.id.location_text);
        descriptionInput = findViewById(R.id.description_input);
        submitButton = findViewById(R.id.submit_button);

        // Add ImageView for preview (if not in XML, create dynamically)
        previewImageView = new ImageView(this);
        previewImageView.setId(View.generateViewId());
        previewImageView.setVisibility(View.GONE);
        previewImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Add to layout (you might need to adjust your layout to include this)
        // For simplicity, we'll handle this differently
    }

    private void setupListeners() {
        // Take Photo button
        cardTakePhoto.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                takePhoto();
            }
        });

        // Upload Photo button
        cardUploadPhoto.setOnClickListener(v -> {
            pickImageFromGallery();
        });

        // Enter Manually location
        TextView enterManually = findViewById(R.id.enter_manually);
        enterManually.setOnClickListener(v -> {
            showLocationInputDialog();
        });

        // Description text change listener
        descriptionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkSubmitButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Submit button
        submitButton.setOnClickListener(v -> {
            submitReport();
        });
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void showLocationInputDialog() {
        // Create a simple dialog for location input
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Enter Location");

        final EditText input = new EditText(this);
        input.setText(currentLocation);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            currentLocation = input.getText().toString();
            locationText.setText(currentLocation);
            checkSubmitButtonState();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void checkSubmitButtonState() {
        boolean hasImage = selectedImage != null;
        boolean hasDescription = descriptionInput.getText().toString().trim().length() > 0;
        boolean hasLocation = !currentLocation.isEmpty() &&
                !currentLocation.equals("Default Location (Kuala Lumpur)");

        // Enable button only if image is selected AND description is entered
        submitButton.setEnabled(hasImage || hasDescription);

        // Optional: Change button color based on state
//        if (hasImage && hasDescription) {
//            submitButton.setBackgroundResource(R.drawable.btn_submit_enabled);
//        } else {
//            submitButton.setBackgroundResource(R.drawable.btn_submit_disabled);
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Handle camera result
                if (data != null && data.getExtras() != null) {
                    selectedImage = (Bitmap) data.getExtras().get("data");
                    if (selectedImage != null) {
                        imageName = "photo_" + System.currentTimeMillis() + ".jpg";
                        Toast.makeText(this, "Photo taken successfully!", Toast.LENGTH_SHORT).show();
                        checkSubmitButtonState();

                        // Show preview in the card
                        showImagePreview();
                    }
                }
            } else if (requestCode == REQUEST_IMAGE_PICK) {
                // Handle gallery pick result
                if (data != null) {
                    Uri imageUri = data.getData();
                    try {
                        selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        imageName = "upload_" + System.currentTimeMillis() + ".jpg";
                        Toast.makeText(this, "Photo uploaded successfully!", Toast.LENGTH_SHORT).show();
                        checkSubmitButtonState();

                        // Show preview in the card
                        showImagePreview();
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void showImagePreview() {
        // Hide the "Take Photo" and "Upload Photo" cards
        cardTakePhoto.setVisibility(View.GONE);
        cardUploadPhoto.setVisibility(View.GONE);

        // Show the image preview in card_upload_photo's position
        // You might need to adjust this based on your layout
        if (selectedImage != null) {
            ImageView preview = findViewById(R.id.ic_upload_photo);
            if (preview != null) {
                preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                preview.setImageBitmap(selectedImage);
            }
        }
    }

    private void submitReport() {
        // Get form data
        String description = descriptionInput.getText().toString().trim();
        String location = locationText.getText().toString();

        // Generate timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
        String currentTime = sdf.format(new Date());

        // Determine category and severity (in real app, this could come from user selection or AI)
        String category = determineCategory(description);
        String severity = determineSeverity(description);

        // Generate unique image name if not already set
        if (imageName.isEmpty()) {
            imageName = "report_" + System.currentTimeMillis() + ".jpg";
        }

        // Create new ticket
        Ticket newTicket = new Ticket(
                "", // Will be auto-generated by TicketManager
                category,
                severity,
                location,
                description,
                currentTime,
                imageName
        );

        // Save the image to internal storage (optional)
        if (selectedImage != null) {
            saveImageToInternalStorage(imageName, selectedImage);
        }

        // Add to TicketManager
        TicketManager.getInstance().addTicket(newTicket);

        // Show success message
        Toast.makeText(this, "Report submitted successfully! ID: " + newTicket.getId(),
                Toast.LENGTH_LONG).show();

        // Return to main activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("new_report_submitted", true);
        setResult(Activity.RESULT_OK, resultIntent);

        // Optional: Clear form
        resetForm();

        // Finish activity
        finish();
    }

    private String determineCategory(String description) {
        // Simple logic to determine category based on keywords
        description = description.toLowerCase();

        if (description.contains("road") || description.contains("pothole") ||
                description.contains("traffic") || description.contains("street")) {
            return "Road";
        } else if (description.contains("water") || description.contains("pipe") ||
                description.contains("flood") || description.contains("drain")) {
            return "Utilities";
        } else if (description.contains("park") || description.contains("bench") ||
                description.contains("light") || description.contains("facility")) {
            return "Facilities";
        } else if (description.contains("tree") || description.contains("environment") ||
                description.contains("park") || description.contains("green")) {
            return "Environment";
        } else {
            return "Other";
        }
    }

    private String determineSeverity(String description) {
        // Simple logic to determine severity based on keywords
        description = description.toLowerCase();

        if (description.contains("emergency") || description.contains("dangerous") ||
                description.contains("immediate") || description.contains("urgent") ||
                description.contains("accident") || description.contains("hazard")) {
            return "High";
        } else if (description.contains("serious") || description.contains("important") ||
                description.contains("major") || description.contains("broken")) {
            return "Medium";
        } else {
            return "Low";
        }
    }

    private void saveImageToInternalStorage(String filename, Bitmap bitmap) {
        // Save image to app's internal storage
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);

            // In a real app, you would save to internal storage or upload to server
            // For now, we'll just keep it in memory
            // To save to internal storage, you would use:
            // FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
            // fos.write(bytes.toByteArray());
            // fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetForm() {
        selectedImage = null;
        imageName = "";
        descriptionInput.setText("");
        currentLocation = "Default Location (Kuala Lumpur)";
        locationText.setText(currentLocation);

        // Show photo options again
        cardTakePhoto.setVisibility(View.VISIBLE);
        cardUploadPhoto.setVisibility(View.VISIBLE);

        // Reset image preview
        ImageView preview = findViewById(R.id.ic_upload_photo);
        if (preview != null) {
            preview.setImageResource(android.R.drawable.ic_menu_upload);
            preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }

        checkSubmitButtonState();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
            }
        }
    }
}