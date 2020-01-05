package com.mercuryf_ofiro.puzzlego;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import java.util.Collections;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_CONTACTS;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnPoiClickListener {

    private static final String TAG_ACTIVIY = "Activty";
    private static final String TAG_SERVICE = "Service";
    private static final String TAG_DEBUG = "Debug";
    private static final String TAG_GPS = "GPS";
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    boolean mLocationPermissionGranted = false, mContactsPermissionGranted = false;
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private final int PERMISSIONS_REQUEST_READ_CONTACTS = 1;
    private final float DEFAULT_ZOOM = 18f;
    private final String sp_name = "Service_Mode";
    private Location mLastKnownLocation;
    private LatLng mDefaultLocation;
    private LocationManager locationManager;
    private int Service_state = 1;
    private boolean Service_mode = false;
    private SharedPreferences.Editor edit;
    Handler hand = new Handler();
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        btn = findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                followUser();
            }
        });

        //Sets default location in case no service is accessible.
        mDefaultLocation = new LatLng(-33.852, 151.211);

        //Checks if the user gave permission to use gps prior to this boot.
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mLocationPermissionGranted = true;
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
            mContactsPermissionGranted = true;

        getLocationPermission();

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        //Starts the request location updater to follow up user locations.
        locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,
                100,
                10, locationListener);

        if(checkService()){
            setService_state(true);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private boolean checkService(){
        SharedPreferences sp = getSharedPreferences(sp_name, MODE_PRIVATE);
        edit = getSharedPreferences(sp_name, MODE_PRIVATE).edit();
        if(sp != null) {
            Service_state = sp.getInt(sp_name, -1);
            if (Service_state == -1) {
                Log.d(TAG_SERVICE, "first boot");
                edit.putInt(sp_name, Service_state);
                edit.apply();
                return true;
            } else {
                Log.d(TAG_SERVICE, "not first boot");
                if (Service_state == 0) {
                    Log.d(TAG_SERVICE, "service off");
                    return false;
                }
                else{
                    Log.d(TAG_SERVICE, "service on");
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        hand.postDelayed(runnable, 0);
    }

    private void setService_state(boolean mode){
        if(mode){
            //Starts the service
            Intent LocationServiceIntent = new Intent(this, LocationService.class);
            startService(LocationServiceIntent);
            Service_mode = true;
        }
        else{
            //Stops the service
            Intent LocationServiceIntent = new Intent(this, LocationService.class);
            stopService(LocationServiceIntent);
            Service_mode = false;
        }
    }

    @Override
    public void onPoiClick(PointOfInterest pointOfInterest) {

    }

    //Checks the users permissions.
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            mContactsPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{READ_CONTACTS},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    //Updates the map UI based on user permissions.
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // Turn on the My Location layer and the related control on the map.
            updateLocationUI();

            // Get the current location of the device and set the position of the map.
            getDeviceLocation();
        }
    };

    private void followUser() {
        Intent i = new Intent(this, WhatsAppContacts.class);
        startActivity(i);
    }

    //Checks for location changes and behaves accordingly.
    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            getDeviceLocation();
            if(mLastKnownLocation != null){
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom((new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude())), DEFAULT_ZOOM), 5000, null);
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    protected void onResume(){
        super.onResume();
        //isLocationEnabled();
    }

    protected void onPause(){
        super.onPause();
    }

    protected void onDestory(){
        super.onDestroy();
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = (Location)task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        } else {
                            Log.d(TAG_GPS, "Current location is null. Using defaults.");
                            Log.e(TAG_GPS, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    //Sets maps to light or day mode.
    private void Set_map(boolean mode){
        try {
            boolean success = false;
            if(mode){
                success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.day_map));
            }
            else{
                success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.night_map));
            }

            if (!success) {
                Log.e(TAG_DEBUG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG_DEBUG, "Can't find style. Error: ", e);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            new AlertDialog.Builder(this).setTitle("About PuzzleGO").setMessage("This is the game Puzzle 15. \n By Mercury Finnegan & Ofir Ozeri.").setIcon(android.R.drawable.ic_dialog_alert).show();
            return true;
        }

        if(id == R.id.action_exit){
            new AlertDialog.Builder(this).setTitle("Exit Game").setMessage("Are you sure you want to exit?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            System.exit(1);
                        }
                    }).setNegativeButton(android.R.string.no, null).setIcon(android.R.drawable.ic_dialog_alert).show();
            return true;
        }

        if(id == R.id.action_serviceoff){
            new AlertDialog.Builder(this).setTitle("Service mode").setMessage("Are you sure you want to turn off location awareness?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.d(TAG_SERVICE, "turned off service");
                            Service_state = 0;
                            edit.putInt(sp_name,Service_state);
                            edit.apply();
                            setService_state(false);
                        }
                    }).setNegativeButton(android.R.string.no, null).setIcon(android.R.drawable.ic_dialog_alert).show();
            return true;
        }

        if(id == R.id.action_serviceon){
            new AlertDialog.Builder(this).setTitle("Service mode").setMessage("Are you sure you want to turn on location awareness?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Service_state = 1;
                            Log.d(TAG_SERVICE, "turned on service");
                            edit.putInt(sp_name,Service_state);
                            edit.apply();
                            setService_state(true);
                        }
                    }).setNegativeButton(android.R.string.no, null).setIcon(android.R.drawable.ic_dialog_alert).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
