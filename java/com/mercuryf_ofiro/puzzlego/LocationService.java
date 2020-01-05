package com.mercuryf_ofiro.puzzlego;

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
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class LocationService extends Service {

    private boolean isRunning;
    private static final String TAG_GPS = "Service_GPS";
    private static final String TAG_SERVICE = "Service";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 10f;
    private String textTitle = "Current_location";
    private String textContent = "";

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
        isRunning = true;
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while(isRunning)
                {
                    if (mLocationListeners[0] == null) {
                        textContent = "Current Location long: " + mLocationListeners[1].mLastLocation.getLongitude() + " lng: " + mLocationListeners[1].mLastLocation.getLatitude();
                    }
                    else{
                        textContent = "Current Location long: " + mLocationListeners[0].mLastLocation.getLongitude() + " lng: " + mLocationListeners[0].mLastLocation.getLatitude();
                    }
                    //Log.e(TAG_SERVICE, textContent);
                    //showNotification(textTitle, textContent);
                }
            }
        }).start();
        //START_STICKY will attempt to restart service if it stops.
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.e(TAG_SERVICE, "onCreate");
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


}
