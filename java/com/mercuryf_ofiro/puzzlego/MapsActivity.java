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
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_CONTACTS;
import static com.google.android.libraries.places.api.Places.createClient;

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
    private static final int M_MAX_ENTRIES = 5;
    private String[] mLikelyPlaceNames;
    private String[] mLikelyPlaceAddresses;
    private String[] mLikelyPlaceAttributions;
    private String[] getmLikelyPlaceIDs;
    private Double[] getmLikelyPlaceNum;
    private LatLng[] mLikelyPlaceLatLngs;
    private PhotoMetadata[] getPhotoPlace;
    String serverKey = "AIzaSyD2MTsBcjEIYPT-bFBXPOKrpy56d3pdHOQ";
    public PlacesClient placesClient;
    ImageView img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key), Locale.US);
        }
        placesClient = createClient(this);
        btn = findViewById(R.id.btn);
        btn.setOnClickListener(view -> {
            //showCurrentPlace();
            //followUser();
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
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                100,
                10, locationListener);

        if (checkService()) {
            setService_state(true);
        }
        img = findViewById(R.id.imageView);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private boolean checkService() {
        SharedPreferences sp = getSharedPreferences(sp_name, MODE_PRIVATE);
        edit = getSharedPreferences(sp_name, MODE_PRIVATE).edit();
        if (sp != null) {
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
                } else {
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
        //mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.night_map));
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        hand.postDelayed(runnable, 0);
        mMap.setOnPoiClickListener(pointOfInterest -> {
            getPlaceList();
        });
    }

    private void get_place_photo() {
        getPlaceList();
    }

    private void setService_state(boolean mode) {
        if (mode) {
            //Starts the service
            Intent LocationServiceIntent = new Intent(this, LocationService.class);
            startService(LocationServiceIntent);
            Service_mode = true;
        } else {
            //Stops the service
            Intent LocationServiceIntent = new Intent(this, LocationService.class);
            stopService(LocationServiceIntent);
            Service_mode = false;
        }
    }


    @Override
    public void onPoiClick(PointOfInterest poi) {

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
        } catch (SecurityException e) {
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

    private void getPlaceList() {

        // Use fields to define the data types to return.
        List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS,
                Place.Field.LAT_LNG, Place.Field.ID, Place.Field.PHOTO_METADATAS);

        // Get the likely places - that is, the businesses and other points of interest that
        // are the best match for the device's current location.
        @SuppressWarnings("MissingPermission") final FindCurrentPlaceRequest request =
                FindCurrentPlaceRequest.builder(placeFields).build();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(request);
        placeResponse.addOnCompleteListener(this,
                task -> {
                    if (task.isSuccessful()) {
                        FindCurrentPlaceResponse response = task.getResult();
                        // Set the count, handling cases where less than 5 entries are returned.
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

                        //Find up to 5 places near the place and get their data.
                        for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
                            Place currPlace = placeLikelihood.getPlace();
                            mLikelyPlaceNames[i] = currPlace.getName();
                            mLikelyPlaceAddresses[i] = currPlace.getAddress();
                            mLikelyPlaceAttributions[i] = (currPlace.getAttributions() == null) ?
                                    null : TextUtils.join(" ", currPlace.getAttributions());
                            mLikelyPlaceLatLngs[i] = currPlace.getLatLng();
                            getmLikelyPlaceIDs[i] = currPlace.getId();
                            getmLikelyPlaceNum[i] = placeLikelihood.getLikelihood();
                            if(currPlace.getPhotoMetadatas() != null){
                                getPhotoPlace[i] = currPlace.getPhotoMetadatas().get(0);
                            }


                            String currLatLng = (mLikelyPlaceLatLngs[i] == null) ?
                                    "" : mLikelyPlaceLatLngs[i].toString();

                            Log.i("debug", "Place " + currPlace.getName()
                                    + " has likelihood: " + placeLikelihood.getLikelihood()
                                    + " at " + getmLikelyPlaceIDs[i]);

                            i++;
                            if (i > (count - 1)) {
                                break;
                            }

                            int pos = -1;
                            if(getmLikelyPlaceNum != null){
                                pos = 0;
                            //Finds the place with the highest likelyhood to user.
                            for(int j = 0; j<getmLikelyPlaceNum.length-1; j++){
                                if(getmLikelyPlaceNum[j+1] > getmLikelyPlaceNum[j])
                                    pos = j+1;
                                }
                            }

                            if(getPhotoPlace[pos] != null) {
                                FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(getPhotoPlace[pos])
                                        .setMaxWidth(1200) // Optional.
                                        .setMaxHeight(1200) // Optional.
                                        .build();
                                placesClient.fetchPhoto(photoRequest).addOnSuccessListener((fetchPhotoResponse) -> {
                                    Bitmap bitmap = fetchPhotoResponse.getBitmap();
                                    img.setImageBitmap(bitmap);
                                }).addOnFailureListener((exception) -> {
                                    if (exception instanceof ApiException) {
                                        ApiException apiException = (ApiException) exception;
                                        int statusCode = apiException.getStatusCode();
                                        // Handle error with given status code.
                                        Log.e("debug", "Photo not found: " + exception.getMessage());
                                    }
                                });

                            }


                        }


//                        String placeId = getmLikelyPlaceIDs[0];
//
//                        // Specify fields. Requests for photos must always have the PHOTO_METADATAS field.
//                        List<Place.Field> fields = Collections.singletonList(Place.Field.PHOTO_METADATAS);
//
//                        // Get a Place object (this example uses fetchPlace(), but you can also use findCurrentPlace())
//                        FetchPlaceRequest placeRequest = FetchPlaceRequest.newInstance(placeId, fields);
//
//                        placesClient.fetchPlace(placeRequest).addOnSuccessListener((responses) -> {
//                            Place place = responses.getPlace();
//
//                            if(place.getPhotoMetadatas() == null)
//                                return;
//                            // Get the photo metadata.
//                            PhotoMetadata photoMetadata = place.getPhotoMetadatas().get(0);
//
//                            // Get the attribution text.
//                            String attributions = photoMetadata.getAttributions();
//
//                            // Create a FetchPhotoRequest.
//                            FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata)
//                                    .setMaxWidth(1200) // Optional.
//                                    .setMaxHeight(1200) // Optional.
//                                    .build();
//                            placesClient.fetchPhoto(photoRequest).addOnSuccessListener((fetchPhotoResponse) -> {
//                                Bitmap bitmap = fetchPhotoResponse.getBitmap();
//                                img.setImageBitmap(bitmap);
//                            }).addOnFailureListener((exception) -> {
//                                if (exception instanceof ApiException) {
//                                    ApiException apiException = (ApiException) exception;
//                                    int statusCode = apiException.getStatusCode();
//                                    // Handle error with given status code.
//                                    Log.e("debug", "Place not found: " + exception.getMessage());
//                                }
//                            });
//                        });


                        //request_photo(getmLikelyPlaceIDs[0]);

                    } else {
                        Exception exception = task.getException();
                        if (exception instanceof ApiException) {
                            ApiException apiException = (ApiException) exception;
                            Log.e("debug", "Place not found: " + apiException.getStatusCode());
                        }
                    }
                });
//        // Use fields to define the data types to return.
//        List<Place.Field> placeFields = Collections.singletonList(Place.Field.ID);
//        // Use the builder to create a FindCurrentPlaceRequest.
//        FindCurrentPlaceRequest request =
//                FindCurrentPlaceRequest.newInstance(placeFields);
//
//        // Call findCurrentPlace and handle the response (first check that the user has granted permission).
//        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(request);
//            placeResponse.addOnCompleteListener(task -> {
//                if (task.isSuccessful()) {
//                    FindCurrentPlaceResponse response = task.getResult();
//                    int count;
//                    if (response.getPlaceLikelihoods().size() < M_MAX_ENTRIES) {
//                        count = response.getPlaceLikelihoods().size();
//                    } else {
//                        count = M_MAX_ENTRIES;
//                    }
//
//                    int i = 0;
//                    getmLikelyPlaceIDs = new String[count];
//                    getmLikelyPlaceNum = new Double[count];
//
//                    for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
//                        Place currPlace = placeLikelihood.getPlace();
//                        getmLikelyPlaceNum[i] = placeLikelihood.getLikelihood();
//                        getmLikelyPlaceIDs[i] = currPlace.getId();
//                        Log.d("debug", "ID is: " + getmLikelyPlaceIDs[i] );
//
//                        Log.i("debug", String.format(
//                                " has likelihood: " + placeLikelihood.getLikelihood()
//                                ));
//
//                        i++;
//                        if (i > (count - 1)) {
//                            break;
//                        }
//                    }


//                    int pos = -1;
//                    if(getmLikelyPlaceNum != null){
//                        pos = 0;
//                        //Finds the place with the highest likelyhood to user.
//                        for(int j = 0; j<getmLikelyPlaceNum.length-1; j++){
//                            if(getmLikelyPlaceNum[j+1] > getmLikelyPlaceNum[j])
//                                pos = j+1;
//                        }
//                    }
//                    if(pos > -1){
//                        Log.d("debug", "in pos");


                        //request_photo(getmLikelyPlaceIDs[pos]);
//                    }

//
//                } else {
//                    Exception exception = task.getException();
//                    if (exception instanceof ApiException) {
//                        ApiException apiException = (ApiException) exception;
//                        Log.e("debug", "Place not found: " + apiException.getStatusCode());
//                    }
//                }
//            });
//        }
    }

    private void request_photo(String ID){
        List<Place.Field> fields = Collections.singletonList(Place.Field.PHOTO_METADATAS);
        // Get a Place object (this example uses fetchPlace(), but you can also use findCurrentPlace())
        FetchPlaceRequest placeRequest = FetchPlaceRequest.newInstance(ID, fields);

        placesClient.fetchPlace(placeRequest).addOnSuccessListener((responses) -> {
            Place place = responses.getPlace();

            // Get the photo metadata.
            if(place.getPhotoMetadatas() == null){
                Log.d("debug", "failed to get photo");
                return;
            }
            PhotoMetadata photoMetadata = place.getPhotoMetadatas().get(0);

            // Get the attribution text.
            String attributions = photoMetadata.getAttributions();

            // Create a FetchPhotoRequest.
            FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata)
                    .setMaxWidth(1000)
                    .setMaxHeight(1000)
                    .build();
            placesClient.fetchPhoto(photoRequest).addOnSuccessListener((fetchPhotoResponse) -> {
                Bitmap bitmap = fetchPhotoResponse.getBitmap();
                //Send the photo to game logic.
                Log.d("debug", "test " + bitmap + "test");
                Intent photo_intent = new Intent(this, GameActivity.class);
                photo_intent.putExtra("BitmapImage", bitmap);
                //startActivity(photo_intent);
                img.setImageBitmap(bitmap);

            }).addOnFailureListener((exception) -> {
                if (exception instanceof ApiException) {
                    ApiException apiException = (ApiException) exception;
                    int statusCode = apiException.getStatusCode();
                    // Handle error with given status code.
                    Log.e("debug", "Place not found: " + exception.getMessage());
                }
            });
        });
    }

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
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) ->
                            System.exit(1)).setNegativeButton(android.R.string.no, null).setIcon(android.R.drawable.ic_dialog_alert).show();
            return true;
        }

        if(id == R.id.action_serviceoff){
            new AlertDialog.Builder(this).setTitle("Service mode").setMessage("Are you sure you want to turn off location awareness?")
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                        Log.d(TAG_SERVICE, "turned off service");
                        Service_state = 0;
                        edit.putInt(sp_name,Service_state);
                        edit.apply();
                        setService_state(false);
                    }).setNegativeButton(android.R.string.no, null).setIcon(android.R.drawable.ic_dialog_alert).show();
            return true;
        }

        if(id == R.id.action_serviceon){
            new AlertDialog.Builder(this).setTitle("Service mode").setMessage("Are you sure you want to turn on location awareness?")
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                        Service_state = 1;
                        Log.d(TAG_SERVICE, "turned on service");
                        edit.putInt(sp_name,Service_state);
                        edit.apply();
                        setService_state(true);
                    }).setNegativeButton(android.R.string.no, null).setIcon(android.R.drawable.ic_dialog_alert).show();
            return true;
        }

        if(id == R.id.action_prize){

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
