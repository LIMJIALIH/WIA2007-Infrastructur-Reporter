package com.example.infrastructureproject.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

public class LocationHelper {
    private static final String TAG = "LocationHelper";
    private final Activity activity;
    private final FusedLocationProviderClient fusedLocationClient;

    public interface LocationCallback {
        void onLocationReceived(double latitude, double longitude, String address);
        void onLocationError(String error);
    }

    public LocationHelper(Activity activity) {
        this.activity = activity;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
    }

    public void getCurrentLocation(LocationCallback callback) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            callback.onLocationError("Location permission not granted");
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(activity, location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        
                        // Format address (you can enhance this with Geocoder for real address)
                        String address = String.format("%.6f, %.6f", lat, lon);
                        
                        Log.d(TAG, "Location: " + lat + ", " + lon);
                        callback.onLocationReceived(lat, lon, address);
                    } else {
                        callback.onLocationError("Unable to get current location");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Location error: " + e.getMessage(), e);
                    callback.onLocationError("Failed to get location: " + e.getMessage());
                });
    }

    public static boolean hasLocationPermission(Activity activity) {
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestLocationPermission(Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(activity, 
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
            requestCode);
    }
}
