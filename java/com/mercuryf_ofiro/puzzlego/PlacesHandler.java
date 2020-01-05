package com.mercuryf_ofiro.puzzlego;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class PlacesHandler {

    public class PlaceJSONParser {

        /** Receives a JSONObject and returns a list */
        public Place[] parse(JSONObject jObject){

            JSONArray jPlaces = null;
            try {
                /** Retrieves all the elements in the 'places' array */
                jPlaces = jObject.getJSONArray("results");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            /** Invoking getPlaces with the array of json object
             * where each json object represent a place
             */
            return getPlaces(jPlaces);
        }

        private Place[] getPlaces(JSONArray jPlaces){
            int placesCount = jPlaces.length();
            Place[] places = new Place[placesCount];

            /** Taking each place, parses and adds to list object */
            for(int i=0; i<placesCount;i++){
                try {
                    /** Call getPlace with place JSON object to parse the place */
                    places[i] = getPlace((JSONObject)jPlaces.get(i));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return places;
        }

        /** Parsing the Place JSON object */
        private Place getPlace(JSONObject jPlace){

            Place place = new Place();

            try {
                // Extracting Place name, if available
                if(!jPlace.isNull("name")){
                    place.mPlaceName = jPlace.getString("name");
                }

                // Extracting Place Vicinity, if available
                if(!jPlace.isNull("vicinity")){
                    place.mVicinity = jPlace.getString("vicinity");
                }

                if(!jPlace.isNull("photos")){
                    JSONArray photos = jPlace.getJSONArray("photos");
                    place.mPhotos = new Photo[photos.length()];
                    for(int i=0;i<photos.length();i++){
                        place.mPhotos[i] = new Photo();
                        place.mPhotos[i].mWidth = ((JSONObject)photos.get(i)).getInt("width");
                        place.mPhotos[i].mHeight = ((JSONObject)photos.get(i)).getInt("height");
                        place.mPhotos[i].mPhotoReference = ((JSONObject)photos.get(i)).getString("photo_reference");
                        JSONArray attributions = ((JSONObject)photos.get(i)).getJSONArray("html_attributions");
                        place.mPhotos[i].mAttributions = new Attribution[attributions.length()];
                        for(int j=0;j<attributions.length();j++){
                            place.mPhotos[i].mAttributions[j] = new Attribution();
                            place.mPhotos[i].mAttributions[j].mHtmlAttribution = attributions.getString(j);
                        }
                    }
                }

                place.mLat = jPlace.getJSONObject("geometry").getJSONObject("location").getString("lat");
                place.mLng = jPlace.getJSONObject("geometry").getJSONObject("location").getString("lng");

            } catch (JSONException e) {
                e.printStackTrace();
                Log.d("EXCEPTION", e.toString());
            }
            return place;
        }
    }

    // Defining DialogFragment class to show the place details with photo
    public class PlaceDialogFragment extends DialogFragment {

        TextView mTVPhotosCount = null;
        TextView mTVVicinity = null;
        ViewFlipper mFlipper = null;
        Place mPlace = null;
        DisplayMetrics mMetrics = null;

        public PlaceDialogFragment(){
            super();
        }

        public PlaceDialogFragment(Place place, DisplayMetrics dm){
            super();
            this.mPlace = place;
            this.mMetrics = dm;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {

            // For retaining the fragment on screen rotation
            setRetainInstance(true);
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
//            View v = inflater.inflate(R.layout.dialog_layout, null);
//
//            // Getting reference to ViewFlipper
//            mFlipper = (ViewFlipper) v.findViewById(R.id.flipper);
//
//            // Getting reference to TextView to display photo count
//            mTVPhotosCount = (TextView) v.findViewById(R.id.tv_photos_count);
//
//            // Getting reference to TextView to display place vicinity
//            mTVVicinity = (TextView) v.findViewById(R.id.tv_vicinity);

            if(mPlace!=null){

                // Setting the title for the Dialog Fragment
                getDialog().setTitle(mPlace.mPlaceName);

                // Array of references of the photos
                Photo[] photos = mPlace.mPhotos;

                // Setting Photos count
                mTVPhotosCount.setText("Photos available : " + photos.length);

                // Setting the vicinity of the place
                mTVVicinity.setText(mPlace.mVicinity);

                // Creating an array of ImageDownloadTask to download photos
                ImageDownloadTask[] imageDownloadTask = new ImageDownloadTask[photos.length];

                int width = (int)(mMetrics.widthPixels*3)/4;
                int height = (int)(mMetrics.heightPixels*1)/2;

                String url = "https://maps.googleapis.com/maps/api/place/photo?";
                String key = "key=YOUR_BROWSER_KEY";
                String sensor = "sensor=true";
                String maxWidth="maxwidth=" + width;
                String maxHeight = "maxheight=" + height;
                url = url + "&" + key + "&" + sensor + "&" + maxWidth + "&" + maxHeight;

                // Traversing through all the photoreferences
                for(int i=0;i<photos.length;i++){
                    // Creating a task to download i-th photo
                    imageDownloadTask[i] = new ImageDownloadTask();

                    String photoReference = "photoreference="+photos[i].mPhotoReference;

                    // URL for downloading the photo from Google Services
                    url = url + "&" + photoReference;

                    // Downloading i-th photo from the above url
                    imageDownloadTask[i].execute(url);
                }
            }
            return v;
        }

        @Override
        public void onDestroyView() {
            if (getDialog() != null && getRetainInstance())
                getDialog().setDismissMessage(null);
            super.onDestroyView();
        }

        private Bitmap downloadImage(String strUrl) throws IOException {
            Bitmap bitmap=null;
            InputStream iStream = null;
            try{
                URL url = new URL(strUrl);

                /** Creating an http connection to communcate with url */
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                /** Connecting to url */
                urlConnection.connect();

                /** Reading data from url */
                iStream = urlConnection.getInputStream();

                /** Creating a bitmap from the stream returned from the url */
                bitmap = BitmapFactory.decodeStream(iStream);

            }catch(Exception e){
                Log.d("Exception while url", e.toString());
            }finally{
                iStream.close();
            }
            return bitmap;
        }

        private class ImageDownloadTask extends AsyncTask<String, Integer, Bitmap> {
            Bitmap bitmap = null;
            @Override
            protected Bitmap doInBackground(String... url) {
                try{
                    // Starting image download
                    bitmap = downloadImage(url[0]);
                }catch(Exception e){
                    Log.d("Background Task",e.toString());
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                // Creating an instance of ImageView to display the downloaded image
                ImageView iView = new ImageView(getActivity().getBaseContext());

                // Setting the downloaded image in ImageView
                iView.setImageBitmap(result);

                // Adding the ImageView to ViewFlipper
                mFlipper.addView(iView);

                // Showing download completion message
                Toast.makeText(getActivity().getBaseContext(), "Image downloaded successfully", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public class MainActivity extends FragmentActivity {

        // GoogleMap
        GoogleMap mGoogleMap;

        // Spinner in which the location types are stored
        Spinner mSprPlaceType;

        // A button to find the near by places
        Button mBtnFind=null;

        // Stores near by places
        Place[] mPlaces = null;

        // A String array containing place types sent to Google Place service
        String[] mPlaceType=null;

        // A String array containing place types displayed to user
        String[] mPlaceTypeName=null;

        // The location at which user touches the Google Map
        LatLng mLocation=null;

        // Links marker id and place object
        HashMap<String, Place> mHMReference = new HashMap<String, Place>();

        // Specifies the drawMarker() to draw the marker with default color
        private static final float UNDEFINED_COLOR = -1;

        @Override
        protected void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);
//            setContentView(R.layout.activity_main);
//
//            // Array of place types
//            mPlaceType = getResources().getStringArray(R.array.place_type);
//
//            // Array of place type names
//            mPlaceTypeName = getResources().getStringArray(R.array.place_type_name);

            // Creating an array adapter with an array of Place types
            // to populate the spinner
//            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
//                    android.R.layout.simple_spinner_dropdown_item,
//                    mPlaceTypeName);
//
//            // Getting reference to the Spinner
//            mSprPlaceType = (Spinner) findViewById(R.id.spr_place_type);
//
//            // Setting adapter on Spinner to set place types
//            mSprPlaceType.setAdapter(adapter);
//
//            // Getting reference to Find Button
//            mBtnFind = ( Button ) findViewById(R.id.btn_find);

            // Getting Google Play availability status
            int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

            if(status!= ConnectionResult.SUCCESS){ // Google Play Services are not available

                int requestCode = 10;
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
                dialog.show();

            }else { // Google Play Services are available

                // Getting reference to the SupportMapFragment
//                SupportMapFragment fragment = ( SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
//
//                // Getting Google MapmGoogleMap.setMyLocationEnabled(true);
//                mGoogleMap = fragment.getMap();

                // Enabling MyLocation in Google Map


                // Handling screen rotation
                if(savedInstanceState !=null) {

                    // Removes all the existing links from marker id to place object
                    mHMReference.clear();

                    //If near by places are already saved
                    if(savedInstanceState.containsKey("places")){

                        // Retrieving the array of place objects
                        mPlaces = (Place[]) savedInstanceState.getParcelableArray("places");

                        // Traversing through each near by place object
                        for(int i=0;i<mPlaces.length;i++){

                            // Getting latitude and longitude of the i-th place
                            LatLng point = new LatLng(Double.parseDouble(mPlaces[i].mLat),
                                    Double.parseDouble(mPlaces[i].mLng));

                            // Drawing the marker corresponding to the i-th place
                            Marker m = drawMarker(point,UNDEFINED_COLOR);

                            // Linkng i-th place and its marker id
                            mHMReference.put(m.getId(), mPlaces[i]);
                        }
                    }

                    // If a touched location is already saved
                    if(savedInstanceState.containsKey("location")){

                        // Retrieving the touched location and setting in member variable
                        mLocation = (LatLng) savedInstanceState.getParcelable("location");

                        // Drawing a marker at the touched location
                        drawMarker(mLocation, BitmapDescriptorFactory.HUE_GREEN);
                    }
                }

                // Setting click event lister for the find button
                mBtnFind.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        int selectedPosition = mSprPlaceType.getSelectedItemPosition();
                        String type = mPlaceType[selectedPosition];

                        mGoogleMap.clear();

                        if(mLocation==null){
                            Toast.makeText(getBaseContext(), "Please mark a location", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        drawMarker(mLocation, BitmapDescriptorFactory.HUE_GREEN);

                        StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
                        sb.append("location="+mLocation.latitude+","+mLocation.longitude);
                        sb.append("&radius=5000");
                        sb.append("&types="+type);
                        sb.append("&sensor=true");
                        sb.append("&key=YOUR_BROWSER_KEY");

                        // Creating a new non-ui thread task to download Google place json data
                        PlacesTask placesTask = new PlacesTask();

                        // Invokes the "doInBackground()" method of the class PlaceTask
                        placesTask.execute(sb.toString());
                    }
                });

                // Map Click listener
                mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

                    @Override
                    public void onMapClick(LatLng point) {

                        // Clears all the existing markers
                        mGoogleMap.clear();

                        // Setting the touched location in member variable
                        mLocation = point;

                        // Drawing a marker at the touched location
                        drawMarker(mLocation,BitmapDescriptorFactory.HUE_GREEN);
                    }
                });

                // Marker click listener
                mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

                    @Override
                    public boolean onMarkerClick(Marker marker) {

                        // If touched at User input location
                        if(!mHMReference.containsKey(marker.getId()))
                            return false;

                        // Getting place object corresponding to the currently clicked Marker
                        Place place = mHMReference.get(marker.getId());

                        // Creating an instance of DisplayMetrics
                        DisplayMetrics dm = new DisplayMetrics();

                        // Getting the screen display metrics
                        getWindowManager().getDefaultDisplay().getMetrics(dm);

                        // Creating a dialog fragment to display the photo
                        PlaceDialogFragment dialogFragment = new PlaceDialogFragment(place,dm);

                        // Getting a reference to Fragment Manager
                        FragmentManager fm = getSupportFragmentManager();

                        // Starting Fragment Transaction
                        FragmentTransaction ft = fm.beginTransaction();

                        // Adding the dialog fragment to the transaction
                        ft.add(dialogFragment, "TAG");

                        // Committing the fragment transaction
                        ft.commit();

                        return false;
                    }
                });
            }
        }

        /**
         * A callback function, executed on screen rotation
         */
        @Override
        protected void onSaveInstanceState(Bundle outState) {

            // Saving all the near by places objects
            if(mPlaces!=null)
                outState.putParcelableArray("places", mPlaces);

            // Saving the touched location
            if(mLocation!=null)
                outState.putParcelable("location", mLocation);

            super.onSaveInstanceState(outState);
        }

        /** A method to download json data from argument url */
        private String downloadUrl(String strUrl) throws IOException{
            String data = "";
            InputStream iStream = null;
            HttpURLConnection urlConnection = null;
            try{
                URL url = new URL(strUrl);

                // Creating an http connection to communicate with url
                urlConnection = (HttpURLConnection) url.openConnection();

                // Connecting to url
                urlConnection.connect();

                // Reading data from url
                iStream = urlConnection.getInputStream();

                BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

                StringBuffer sb = new StringBuffer();

                String line = "";
                while( ( line = br.readLine()) != null){
                    sb.append(line);
                }

                data = sb.toString();

                br.close();

            }catch(Exception e){
                Log.d("Exception while url", e.toString());
            }finally{
                iStream.close();
                urlConnection.disconnect();
            }
            return data;
        }
        /** A class, to download Google Places */
        private class PlacesTask extends AsyncTask<String, Integer, String>{

            String data = null;

            // Invoked by execute() method of this object
            @Override
            protected String doInBackground(String... url) {
                try{
                    data = downloadUrl(url[0]);
                }catch(Exception e){
                    Log.d("Background Task",e.toString());
                }
                return data;
            }

            // Executed after the complete execution of doInBackground() method
            @Override
            protected void onPostExecute(String result){
                ParserTask parserTask = new ParserTask();

                // Start parsing the Google places in JSON format
                // Invokes the "doInBackground()" method of ParserTask
                parserTask.execute(result);
            }
        }

        /** A class to parse the Google Places in JSON format */
        private class ParserTask extends AsyncTask<String, Integer, Place[]>{

            JSONObject jObject;

            // Invoked by execute() method of this object
            @Override
            protected Place[] doInBackground(String... jsonData) {

                Place[] places = null;
                PlaceJSONParser placeJsonParser = new PlaceJSONParser();

                try{
                    jObject = new JSONObject(jsonData[0]);
                    /** Getting the parsed data as a List construct */
                    places = placeJsonParser.parse(jObject);

                }catch(Exception e){
                    Log.d("Exception",e.toString());
                }
                return places;
            }

            // Executed after the complete execution of doInBackground() method
            @Override
            protected void onPostExecute(Place[] places){

                mPlaces = places;

                for(int i=0;i< places.length ;i++){
                    Place place = places[i];

                    // Getting latitude of the place
                    double lat = Double.parseDouble(place.mLat);

                    // Getting longitude of the place
                    double lng = Double.parseDouble(place.mLng);

                    LatLng latLng = new LatLng(lat, lng);

                    Marker m = drawMarker(latLng,UNDEFINED_COLOR);

                    // Adding place reference to HashMap with marker id as HashMap key
                    // to get its reference in infowindow click event listener
                    mHMReference.put(m.getId(), place);
                }
            }
        }

        /**
         * Drawing marker at latLng with color
         */
        private Marker drawMarker(LatLng latLng, float color){
            // Creating a marker
            MarkerOptions markerOptions = new MarkerOptions();

            // Setting the position for the marker
            markerOptions.position(latLng);

            if(color != UNDEFINED_COLOR)
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(color));

            // Placing a marker on the touched position
            Marker m = mGoogleMap.addMarker(markerOptions);

            return m;
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }
    }
}
