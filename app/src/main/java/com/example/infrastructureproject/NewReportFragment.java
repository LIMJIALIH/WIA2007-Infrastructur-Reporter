package com.example.infrastructureproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NewReportFragment extends Fragment {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private com.google.android.material.card.MaterialCardView cardTakePhoto;
    private com.google.android.material.card.MaterialCardView cardUploadPhoto;
    private TextView locationText;
    private EditText descriptionInput;
    private Button submitButton;
    private Bitmap selectedImage = null;
    private String currentLocation = "Default Location (Kuala Lumpur)";
    private String imageName = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.report_issue, container, false);

        initializeViews(view);
        setupListeners();
        checkSubmitButtonState();

        return view;
    }

    private void initializeViews(View view) {
        cardTakePhoto = view.findViewById(R.id.card_take_photo);
        cardUploadPhoto = view.findViewById(R.id.card_upload_photo);
        locationText = view.findViewById(R.id.location_text);
        descriptionInput = view.findViewById(R.id.description_input);
        submitButton = view.findViewById(R.id.submit_button);
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
        TextView enterManually = getView().findViewById(R.id.enter_manually);
        if (enterManually != null) {
            enterManually.setOnClickListener(v -> {
                showLocationInputDialog();
            });
        }

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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void showLocationInputDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Enter Location");

        final EditText input = new EditText(requireContext());
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

        submitButton.setEnabled(hasImage && hasDescription);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (data != null && data.getExtras() != null) {
                    selectedImage = (Bitmap) data.getExtras().get("data");
                    if (selectedImage != null) {
                        imageName = "photo_" + System.currentTimeMillis() + ".jpg";
                        Toast.makeText(getContext(), "Photo taken successfully!", Toast.LENGTH_SHORT).show();
                        checkSubmitButtonState();
                    }
                }
            } else if (requestCode == REQUEST_IMAGE_PICK) {
                if (data != null) {
                    Uri imageUri = data.getData();
                    try {
                        selectedImage = MediaStore.Images.Media.getBitmap(
                                requireActivity().getContentResolver(), imageUri);
                        imageName = "upload_" + System.currentTimeMillis() + ".jpg";
                        Toast.makeText(getContext(), "Photo uploaded successfully!", Toast.LENGTH_SHORT).show();
                        checkSubmitButtonState();
                    } catch (IOException e) {
                        Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void submitReport() {
        String description = descriptionInput.getText().toString().trim();
        String location = locationText.getText().toString();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
        String currentTime = sdf.format(new Date());

        // Generate category and severity
        String category = "Road"; // Default
        String severity = "Medium"; // Default

        if (imageName.isEmpty()) {
            imageName = "report_" + System.currentTimeMillis() + ".jpg";
        }

        // Create and add ticket
        Ticket newTicket = new Ticket(
                "",
                category,
                severity,
                location,
                description,
                currentTime,
                imageName
        );

        // Set PENDING status
        newTicket.setStatus(Ticket.TicketStatus.PENDING);

        // Add to TicketManager
        TicketManager.getInstance().addTicket(newTicket);

        Toast.makeText(getContext(), "Report submitted successfully! ID: " + newTicket.getId(),
                Toast.LENGTH_LONG).show();

        // Notify MainActivity to refresh and switch to My Reports
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onNewReportSubmitted(newTicket);
        }

        // Clear form
        resetForm();
    }

    private void resetForm() {
        selectedImage = null;
        imageName = "";
        descriptionInput.setText("");
        currentLocation = "Default Location (Kuala Lumpur)";
        locationText.setText(currentLocation);
        checkSubmitButtonState();
    }
}