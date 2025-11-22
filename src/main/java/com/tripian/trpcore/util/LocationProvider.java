package com.tripian.trpcore.util;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.Task;

public class LocationProvider implements LifecycleObserver {

    // Constants
    public enum PermissionDenied {
        FIRST,
        AGAIN,
        FOREVER
    }

    private final Activity activity;
    private final LifecycleOwner lifecycleOwner;
    private final String locationPermission;
    private final LocationObserver locationListener;

    // Location classes
    private boolean showPermissionRationaleFirstTime = true;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest requestParams;

    private final LocationCallback locationCallback = new LocationCallback() {
        /**
         * This is the callback that is triggered when the
         * FusedLocationClient updates your location.
         * @param locationResult The result containing the device location.
         */
        @Override
        public void onLocationResult(LocationResult locationResult) {
            // If tracking is turned on, reverse geocode into an address
            locationListener.onLocation(locationResult.getLastLocation());
        }
    };

    public interface LocationObserver {
        void onLocation(Location location);

        void onPermissionDenied(PermissionDenied type);

        void onPermissionOk();

        void onLocationDisabled();
    }

    private LocationProvider(
            Activity activity,
            LifecycleOwner lifecycleOwner,
            String locationPermission,
            int accuracy,
            int intervalMs,
            LocationObserver locationObserver
    ) {
        this.activity = activity;
        this.lifecycleOwner = lifecycleOwner;
        this.locationPermission = locationPermission;
        this.locationListener = locationObserver;

        /**
         * Location request parametreleri
         */
        requestParams = new LocationRequest();
        requestParams.setInterval(intervalMs);
        requestParams.setFastestInterval(intervalMs / 2);
        requestParams.setPriority(accuracy);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);

        lifecycleOwner.getLifecycle().removeObserver(this);
        lifecycleOwner.getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        stopTrackingLocation();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        lifecycleOwner.getLifecycle().removeObserver(this);
    }

    public void startTrackingLocation() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(requestParams);

        Task<LocationSettingsResponse> result =
                LocationServices.getSettingsClient(activity).checkLocationSettings(builder.build());

        result.addOnCompleteListener(task -> {
            try {
                if (activity != null) {
                    if (ActivityCompat.checkSelfPermission(activity, locationPermission) == PackageManager.PERMISSION_GRANTED) {
                        LocationSettingsResponse response = task.getResult(ApiException.class);

                        locationListener.onPermissionOk();

                        if (fusedLocationClient.getLastLocation() != null) {
                            fusedLocationClient.getLastLocation().addOnSuccessListener(locationListener::onLocation);
                        }

                        fusedLocationClient.requestLocationUpdates(
                                requestParams,
                                locationCallback,
                                null /* Looper */
                        );
                    } else {
                        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, RequestCodes.PERMISSION_LOCATION);
                    }
                } else {
                    // TODO: fragment activity null gelebilir?
                }
            } catch (ApiException exception) {
                switch (exception.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the
                        // user a dialog.
                        try {
                            // Cast to a resolvable exception.
                            ResolvableApiException resolvable = (ResolvableApiException) exception;
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                    activity,
                                    RequestCodes.REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        } catch (ClassCastException e) {
                            // Ignore, should be an impossible error.
                        }
                        break;
                }

            }
        });
    }

    public void stopTrackingLocation() {
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    /**
     * You must call this in your Activity's onRequestPermissionsResults
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCodes.PERMISSION_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startTrackingLocation();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, locationPermission)) {
                        if (showPermissionRationaleFirstTime) {
                            showPermissionRationaleFirstTime = false;
                            locationListener.onPermissionDenied(PermissionDenied.FIRST);
                        } else {
                            locationListener.onPermissionDenied(PermissionDenied.AGAIN);
                        }
                    } else {
                        locationListener.onPermissionDenied(PermissionDenied.FOREVER);
                    }
                }
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RequestCodes.REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                startTrackingLocation();
            } else {
                locationListener.onLocationDisabled();
            }
        }
    }

    public static class Builder {
        private Activity activity;
        private LifecycleOwner lifecycleOwner;
        private int accuracy = LocationRequest.PRIORITY_HIGH_ACCURACY;
        private int intervalMs = 1000;
        private LocationObserver locationObserver;

        public Builder(Activity activity) {
            this.activity = activity;

            if (activity != null) {
                lifecycleOwner = (LifecycleOwner) activity;
            }
        }

        public Builder accuracy(int accuracy) {
            this.accuracy = accuracy;
            return this;
        }

        public Builder intervalMs(int intervalMs) {
            this.intervalMs = intervalMs;
            return this;
        }

        public Builder locationListener(LocationObserver locationObserver) {
            this.locationObserver = locationObserver;
            return this;
        }

        public LocationProvider build() {
            if (lifecycleOwner == null)
                throw new IllegalArgumentException("lifecycleOwner can't be null");
            if (locationObserver == null)
                throw new IllegalArgumentException("locationListener can't be null");

            return new LocationProvider(
                    activity,
                    lifecycleOwner,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    accuracy,
                    intervalMs,
                    locationObserver
            );
        }
    }
}