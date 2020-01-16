package com.mercuryf_ofiro.puzzlego;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    private Double[] getmLikelyPlaceNum;
    private long time_interval = 300000/5;//5 mins / 5
    private long curr_time;
    private long base_time;


    private Double close_threshold = 0.1;

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
                    getmLikelyPlaceNum = new Double[count];

                    for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
                        Place currPlace = placeLikelihood.getPlace();
                        mLikelyPlaceNames[i] = currPlace.getName();
                        getmLikelyPlaceNum[i] = placeLikelihood.getLikelihood();

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

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Intent intent = new Intent("com.android.ServiceStopped");

    }
}
