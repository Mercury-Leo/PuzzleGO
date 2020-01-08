package com.mercuryf_ofiro.puzzlego;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.google.android.libraries.places.api.Places.createClient;


public class LocationService extends Service {


    private boolean isRunning;
    private static final String TAG_GPS = "Service_GPS";
    private static final String TAG_SERVICE = "Service";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 10f;
    private String textTitle = "Puzzle location";
    private String textContent = "There is a puzzle near you! Open PuzzleGO to solve it.";
    public PlacesClient placesClient;

    private static final int M_MAX_ENTRIES = 5;
    private String[] mLikelyPlaceNames;
    private String[] mLikelyPlaceAddresses;
    private String[] mLikelyPlaceAttributions;
    private String[] getmLikelyPlaceIDs;
    private Double[] getmLikelyPlaceNum;
    private LatLng[] mLikelyPlaceLatLngs;
    private long time_interval = 300000/5;

    private long curr_time;
    private long base_time;

    private Double close_threshold = 0.0001;

    //LocationListener class will listen for location
    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            Log.e(TAG_GPS, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        //If location has changed, update.
        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG_GPS, "onLocationChanged: " + location);
            mLastLocation.set(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG_GPS, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG_GPS, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG_GPS, "onStatusChanged: " + provider);
        }
    }

    //Creates a locationListener, one of GPS and one of Network.
    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG_GPS, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        curr_time = Calendar.getInstance().getTimeInMillis();
        base_time = Calendar.getInstance().getTimeInMillis();
        isRunning = true;
        new Thread(() -> {
            while (isRunning) {
                if(curr_time - base_time > time_interval){
                    curr_time = Calendar.getInstance().getTimeInMillis();
                    base_time = Calendar.getInstance().getTimeInMillis();
                    find_liklyplace();
                    //Log.e(TAG_SERVICE, textContent);
                    //showNotification(textTitle, textContent);
                }
                curr_time = Calendar.getInstance().getTimeInMillis();
            }
        }).start();
        //START_STICKY will attempt to restart service if it stops.
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.e(TAG_SERVICE, "onCreate");
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key), Locale.US);
        }
        placesClient = createClient(this);
        //Creates Location manager.
        initializeLocationManager();
        //Tries to create a listener on the network provider.
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG_SERVICE, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG_SERVICE, "network provider does not exist, " + ex.getMessage());
        }
        //Tries to create a listener on the GPS provider.
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG_SERVICE, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG_SERVICE, "gps provider does not exist " + ex.getMessage());
        }

    }

    //Shows a notification with a given title and message.
    void showNotification(String title, String message) {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //If running on Oreo build, need to create our own channel.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("YOUR_CHANNEL_ID",
                    "YOUR_CHANNEL_NAME",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("YOUR_NOTIFICATION_CHANNEL_DISCRIPTION");
            mNotificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "YOUR_CHANNEL_ID")
                .setSmallIcon(R.mipmap.ic_launcher) // notification icon
                .setContentTitle(title) // title for notification
                .setContentText(message)// message for notification
                .setAutoCancel(true); // clear notification after click
        Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pi);
        mNotificationManager.notify(0, mBuilder.build());
    }

    @Override
    public void onDestroy() {
        Log.e(TAG_SERVICE, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG_SERVICE, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG_GPS, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }


    private void getPlaceList() {
        // Use fields to define the data types to return.
        List<Place.Field> placeFields = Collections.singletonList(Place.Field.NAME);
        // Use the builder to create a FindCurrentPlaceRequest.
        FindCurrentPlaceRequest request =
                FindCurrentPlaceRequest.newInstance(placeFields);

        // Call findCurrentPlace and handle the response (first check that the user has granted permission).
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(request);
            placeResponse.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FindCurrentPlaceResponse response = task.getResult();
                    int count;
                    if (response.getPlaceLikelihoods().size() < M_MAX_ENTRIES) {
                        count = response.getPlaceLikelihoods().size();
                    } else {
                        count = M_MAX_ENTRIES;
                    }

                    int i = 0;
                    mLikelyPlaceNames = new String[count];
                    mLikelyPlaceAddresses = new String[count];
                    mLikelyPlaceAttributions = new String[count];
                    mLikelyPlaceLatLngs = new LatLng[count];
                    getmLikelyPlaceIDs = new String[count];
                    getmLikelyPlaceNum = new Double[count];

                    for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
                        Place currPlace = placeLikelihood.getPlace();
                        mLikelyPlaceNames[i] = currPlace.getName();
                        mLikelyPlaceAddresses[i] = currPlace.getAddress();
                        mLikelyPlaceAttributions[i] = (currPlace.getAttributions() == null) ?
                                null : TextUtils.join(" ", currPlace.getAttributions());
                        mLikelyPlaceLatLngs[i] = currPlace.getLatLng();
                        getmLikelyPlaceNum[i] = placeLikelihood.getLikelihood();
                        getmLikelyPlaceIDs[i] = currPlace.getId();

                        String currLatLng = (mLikelyPlaceLatLngs[i] == null) ?
                                "" : mLikelyPlaceLatLngs[i].toString();

                        Log.i("debug", String.format("Place " + currPlace.getName()
                                + " has likelihood: " + placeLikelihood.getLikelihood()
                                + " at " + currLatLng));

                        i++;
                        if (i > (count - 1)) {
                            break;
                        }
                    }
                } else {
                    Exception exception = task.getException();
                    if (exception instanceof ApiException) {
                        ApiException apiException = (ApiException) exception;
                        Log.e("debug", "Place not found: " + apiException.getStatusCode());
                    }
                }
            });
        } else {
            // A local method to request required permissions;
            // See https://developer.android.com/training/permissions/requesting
        }
    }

    private void find_liklyplace(){
        getPlaceList();
        if (getmLikelyPlaceNum != null){
            for(Double num : getmLikelyPlaceNum){
                if( num!= null && num > close_threshold){
                    showNotification(textTitle,textContent);
                }
            }
        }


    }

//    private void getCurrentPlaceLikelihoods() {
//        // Use fields to define the data types to return.
//        List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS,
//                Place.Field.LAT_LNG);
//
//        // Get the likely places - that is, the businesses and other points of interest that
//        // are the best match for the device's current location.
//        @SuppressWarnings("MissingPermission") final FindCurrentPlaceRequest request =
//                FindCurrentPlaceRequest.builder(placeFields).build();
//        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    Activity#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for Activity#requestPermissions for more details.
//            return;
//        }
//        Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(request);
//        placeResponse.addOnCompleteListener((Executor) this,
//                task -> {
//                    if (task.isSuccessful()) {
//                        FindCurrentPlaceResponse response = task.getResult();
//                        // Set the count, handling cases where less than 5 entries are returned.
//                        int count;
//                        if (response.getPlaceLikelihoods().size() < M_MAX_ENTRIES) {
//                            count = response.getPlaceLikelihoods().size();
//                        } else {
//                            count = M_MAX_ENTRIES;
//                        }
//
//                        int i = 0;
//                        mLikelyPlaceNames = new String[count];
//                        mLikelyPlaceAddresses = new String[count];
//                        mLikelyPlaceAttributions = new String[count];
//                        mLikelyPlaceLatLngs = new LatLng[count];
//                        getmLikelyPlaceIDs = new String[count];
//                        getmLikelyPlaceNum = new Double[count];
//
//                        for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
//                            Place currPlace = placeLikelihood.getPlace();
//                            mLikelyPlaceNames[i] = currPlace.getName();
//                            mLikelyPlaceAddresses[i] = currPlace.getAddress();
//                            mLikelyPlaceAttributions[i] = (currPlace.getAttributions() == null) ?
//                                    null : TextUtils.join(" ", currPlace.getAttributions());
//                            mLikelyPlaceLatLngs[i] = currPlace.getLatLng();
//                            getmLikelyPlaceNum[i] = placeLikelihood.getLikelihood();
//                            getmLikelyPlaceIDs[i] = currPlace.getId();
//
//                            String currLatLng = (mLikelyPlaceLatLngs[i] == null) ?
//                                    "" : mLikelyPlaceLatLngs[i].toString();
//
//                            Log.i("debug", String.format("Place " + currPlace.getName()
//                                    + " has likelihood: " + placeLikelihood.getLikelihood()
//                                    + " at " + currLatLng));
//
//                            i++;
//                            if (i > (count - 1)) {
//                                break;
//                            }
//                        }
//
//
//                        // COMMENTED OUT UNTIL WE DEFINE THE METHOD
//                        // Populate the ListView
//                        // fillPlacesList();
//                    } else {
//                        Exception exception = task.getException();
//                        if (exception instanceof ApiException) {
//                            ApiException apiException = (ApiException) exception;
//                            Log.e("debug", "Place not found: " + apiException.getStatusCode());
//                        }
//                    }
//                });
//    }

}
