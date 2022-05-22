package com.indooratlas.android.sdk.examples;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;

import com.alan.alansdk.AlanCallback;
import com.alan.alansdk.AlanConfig;
import com.alan.alansdk.button.AlanButton;
import com.alan.alansdk.events.EventCommand;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IAOrientationListener;
import com.indooratlas.android.sdk.IAOrientationRequest;
import com.indooratlas.android.sdk.IAPOI;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.IARoute;
import com.indooratlas.android.sdk.IAWayfindingListener;
import com.indooratlas.android.sdk.IAWayfindingRequest;
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IALatLng;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;
import com.indooratlas.android.sdk.resources.IAVenue;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SdkExample(description = R.string.example_wayfinding_description)
public class WayfindingOverlayActivity extends FragmentActivity
        implements GoogleMap.OnMapClickListener, OnMapReadyCallback {
Button currentLocation,goToNearbyLandmark;
    FirebaseDatabase database;
    DatabaseReference myRef;
    private static final String TAG = "IndoorAtlasExample";
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    public static  double currentLatitude;
    public static  double currentLongitude;
    boolean loading=true;
    /* used to decide when bitmap should be downscaled */
    private static final int MAX_DIMENSION = 2048;
TextView db;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    TextToSpeech textToSpeech;
    public DrawerLayout dl;
    public ActionBarDrawerToggle abdt;

    private Circle mCircle;
    private IARegion mOverlayFloorPlan = null;
    private GroundOverlay mGroundOverlay = null;
    private IALocationManager mIALocationManager;
    private Target mLoadTarget;
    private boolean mCameraPositionNeedsUpdating = true; // update on first location
    private Marker mDestinationMarker;
    private Marker mHeadingMarker;
    private IAVenue mVenue;
    private List<Marker> mPoIMarkers = new ArrayList<>();
    private List<Polyline> mPolylines = new ArrayList<>();
    private IARoute mCurrentRoute;
    public Marker mMarker;
    ArrayList<WrngMsg> WMList = new ArrayList<WrngMsg>();
    ArrayList<Landmarks> LMList = new ArrayList<Landmarks>();
    String LandmarkType ;
    WrngMsg wm = new WrngMsg();
    boolean location_Selected = false;
    /// Adding AlanButton variable
    private AlanButton alanButton;

    private IAWayfindingRequest mWayfindingDestination;
    private IAWayfindingListener mWayfindingListener = new IAWayfindingListener() {
        @Override
        public void onWayfindingUpdate(IARoute route) {
            mCurrentRoute = route;
            if (hasArrivedToDestination(route)) {
                // stop wayfinding
                showInfo("You're there!");
                mCurrentRoute = null;
                mWayfindingDestination = null;
                mIALocationManager.removeWayfindingUpdates();
            }


            updateRouteVisualization();
        }
    };

    private IAOrientationListener mOrientationListener = new IAOrientationListener() {
        @Override
        public void onHeadingChanged(long timestamp, double heading) {
            updateHeading(heading);
        }

        @Override
        public void onOrientationChange(long timestamp, double[] quaternion) {
            // we do not need full device orientation in this example, just the heading
        }
    };


    private int mFloor;
    private LatLng Ul;
    private LatLng User_Destination ;

    private void SetUDes(LatLng point) {
        User_Destination = point;
    }

    private LatLng getUD(){
        return User_Destination;
    }

    private LatLng getUl(){
        return Ul;
    }

    private void showLocationCircle(LatLng center, double accuracyRadius) {
        if (mCircle == null) {
            // location can received before map is initialized, ignoring those updates
            if (mMap != null) {
                mCircle = mMap.addCircle(new CircleOptions()
                        .center(center)
                        .radius(accuracyRadius)
                        .fillColor(0x201681FB)
                        .strokeColor(0x500A78DD)
                        .zIndex(1.0f)
                        .visible(true)
                        .strokeWidth(5.0f));
                mHeadingMarker = mMap.addMarker(new MarkerOptions()
                        .position(center)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_blue_dot))
                        .anchor(0.5f, 0.5f)
                        .flat(true));
                User_Destination = mCircle.getCenter();
            }
        } else {
            // move existing markers position to received location
            mCircle.setCenter(center);
            mHeadingMarker.setPosition(center);
            mCircle.setRadius(accuracyRadius);
        }
    }

    private void updateHeading(double heading) {
        if (mHeadingMarker != null) {
            mHeadingMarker.setRotation((float)heading);
        }
    }

    /**
     * Listener that handles location change events.
     */
    private IALocationListener mListener = new IALocationListenerSupport() {

        /**
         * Location changed, move marker and camera position.
         */
        public void onLocationChanged(IALocation location) {

            Log.d(TAG, "new location received with coordinates: " + location.getLatitude()
                    + "," + location.getLongitude());

            if (mMap == null) {
                // location received before map is initialized, ignoring update here
                return;
            }

            final LatLng center = new LatLng(location.getLatitude(), location.getLongitude());
            if(location_Selected == true)
            SetDisplayBox(getUD());
            ShowWrngMsg();



            final int newFloor = location.getFloorLevel();
            if (mFloor != newFloor) {
                updateRouteVisualization();
            }
            mFloor = newFloor;

            showLocationCircle(center, location.getAccuracy());
            setUl(center);


            // our camera position needs updating if location has significantly changed
            if (mCameraPositionNeedsUpdating) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 17.5f));
                mCameraPositionNeedsUpdating = false;
            }
        }
    };

    private void setUl(LatLng location) {
        Ul = location;
    }

    /**
     * Listener that changes overlay if needed
     */
    private IARegion.Listener mRegionListener = new IARegion.Listener() {
        @Override
        public void onEnterRegion(final IARegion region) {
            if (region.getType() == IARegion.TYPE_FLOOR_PLAN) {
                Log.d(TAG, "enter floor plan " + region.getId());
                mCameraPositionNeedsUpdating = true; // entering new fp, need to move camera
                if (mGroundOverlay != null) {
                    mGroundOverlay.remove();
                    mGroundOverlay = null;
                }
                mOverlayFloorPlan = region; // overlay will be this (unless error in loading)
                fetchFloorPlanBitmap(region.getFloorPlan());
                setupPoIs(mVenue.getPOIs(), region.getFloorPlan().getFloorLevel());
            } else if (region.getType() == IARegion.TYPE_VENUE) {
                mVenue = region.getVenue();
            }
        }

        @Override
        public void onExitRegion(IARegion region) {
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
// create an object textToSpeech and adding features into it
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {

                // if No error is found then only it will run
                if(i!=TextToSpeech.ERROR){
                    // To Choose language of speech
                    textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }
        });

        dl=(DrawerLayout)findViewById(R.id.dl);
        abdt=new ActionBarDrawerToggle(this,dl,R.string.Open,R.string.Close);
        abdt.setDrawerIndicatorEnabled(true);

        dl.addDrawerListener(abdt);
        abdt.syncState();

//        getSupportActionBar.setDisplayHomeAsUpEnabled(true);

        final NavigationView nav_view=(NavigationView)findViewById(R.id.nav_view);
        nav_view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int id= menuItem.getItemId();
                if(id == R.id.coffee){
                    LandmarkType = "Coffee Shops";
                    LandmarkSort(LandmarkType);
                    Toast.makeText(WayfindingOverlayActivity.this, "All Coffee area is displayed on map", Toast.LENGTH_SHORT).show();
                }
                if(id == R.id.wc){
                    LandmarkType = "WC";
                    LandmarkSort(LandmarkType);
                    Toast.makeText(WayfindingOverlayActivity.this, "All WC places is displayed on map", Toast.LENGTH_SHORT).show();
                }
                if(id == R.id.classroom){
                    LandmarkType = "Class Rooms";
                    LandmarkSort(LandmarkType);
                    Toast.makeText(WayfindingOverlayActivity.this, "All Classrooms is displayed on map", Toast.LENGTH_SHORT).show();
                }
                if(id == R.id.EXIT){
                    LandmarkType = "Exist doors";
                    LandmarkSort(LandmarkType);
                    Toast.makeText(WayfindingOverlayActivity.this, "EXIT doors places is displayed on map", Toast.LENGTH_SHORT).show();
                }
                if(id == R.id.stairs){
                    LandmarkType = "Stairs";
                    LandmarkSort(LandmarkType);
                    Toast.makeText(WayfindingOverlayActivity.this, "All Stairs places is displayed on map", Toast.LENGTH_SHORT).show();
                }
                if(id == R.id.NearbyLandmark){
                    for (int counter = 0; counter < LMList.size(); counter++){
                        LatLng point =  LMList.get(counter).getLatLng();
                        Marker m = LMList.get(counter).getMarker();
                        if (calcdis(point)<=15){
                            m.setVisible(true);
                        }else m.setVisible(false);
                    }
                    Toast.makeText(WayfindingOverlayActivity.this, "The nearby landmarks is displayed on map", Toast.LENGTH_SHORT).show();
                }
                if(id == R.id.AllLandmarkd){
                    loading=false;

                    for (int counter = 0; counter < LMList.size(); counter++){
                        LatLng point =  LMList.get(counter).getLatLng();
                        Marker m = LMList.get(counter).getMarker();
                        m.setVisible(true);
                    }
                    Toast.makeText(WayfindingOverlayActivity.this, "All landmarks is displayed on map", Toast.LENGTH_SHORT).show();
                }
                if(id == R.id.others){
                    LandmarkType = "Others";
                    LandmarkSort(LandmarkType);
                    Toast.makeText(WayfindingOverlayActivity.this, "Others landmarks is displayed on map", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });


        // prevent the screen going to sleep while app is on foreground
        findViewById(android.R.id.content).setKeepScreenOn(true);
        db= findViewById(R.id.distanceBox);
        // instantiate IALocationManager
        mIALocationManager = IALocationManager.create(this);

//        startListeningPlatformLocations();

        // Try to obtain the map from the SupportMapFragment.
        ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map))
                .getMapAsync(this);
        //to retrieve all markers from database to map
//        getData();
        //to retrieve all warning msgs from database to map
        getData1();

        getClasses();
        getCoffeeShops();
        getExits();
        getWC();
        getStairs();
        getOthers();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return abdt.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    private void getCoffeeShops(){
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Map/Landmark/coffee");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
//                 This method is called once with the initial value and again
//                 whenever data at this location is updated.
//                Landmarks value = dataSnapshot.getValue(Landmarks.class);
//                Log.d(TAG, "Value is: " + value);


                for (DataSnapshot ds : dataSnapshot.getChildren()) {
//

                    String lat = ds.child("latLng/latitude").getValue().toString();
                    String lng = ds.child("latLng/longitude").getValue().toString();
                    String msg = ds.child("Title").getValue(String.class);

                    double latitude = Double.parseDouble(lat);
                    double longitude = Double.parseDouble(lng);
                    LatLng loc = new LatLng(latitude, longitude);


                    mMarker = mMap.addMarker(new MarkerOptions().position(loc).title(msg).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.cofe)));
                    mMarker.setVisible(false);
//                    mMap.addMarker(new MarkerOptions().position(loc).title(msg).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.classroom)));
                    LMList.add(new Landmarks(loc,msg,"Coffee Shops",mMarker));

                }
            }


            @Override
            public void onCancelled(DatabaseError error) {
//                 Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }
    private void getClasses(){
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Map/Landmark/class");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
//                 This method is called once with the initial value and again
//                 whenever data at this location is updated.
//                Landmarks value = dataSnapshot.getValue(Landmarks.class);
//                Log.d(TAG, "Value is: " + value);


                for (DataSnapshot ds : dataSnapshot.getChildren()) {
//

                    String lat = ds.child("latLng/latitude").getValue().toString();
                    String lng = ds.child("latLng/longitude").getValue().toString();
                    String msg = ds.child("Title").getValue(String.class);

                    double latitude = Double.parseDouble(lat);
                    double longitude = Double.parseDouble(lng);
                    LatLng loc = new LatLng(latitude, longitude);


                    mMarker = mMap.addMarker(new MarkerOptions().position(loc).title(msg).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.classroom)));
                    mMarker.setVisible(false);
                    LMList.add(new Landmarks(loc,msg,"Class Rooms",mMarker));
//                    mMap.addMarker(new MarkerOptions().position(loc).title(msg).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.classroom)));

                }
            }


            @Override
            public void onCancelled(DatabaseError error) {
//                 Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }
    private void getStairs(){
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Map/Landmark/stairs");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
//                 This method is called once with the initial value and again
////                 whenever data at this location is updated.
//                Landmarks value = dataSnapshot.getValue(Landmarks.class);
//                Log.d(TAG, "Value is: " + value);


                for (DataSnapshot ds : dataSnapshot.getChildren()) {
//

                    String lat = ds.child("latLng/latitude").getValue().toString();
                    String lng = ds.child("latLng/longitude").getValue().toString();
                    String msg = ds.child("Title").getValue(String.class);

                    double latitude = Double.parseDouble(lat);
                    double longitude = Double.parseDouble(lng);
                    LatLng loc = new LatLng(latitude, longitude);


                   mMarker= mMap.addMarker(new MarkerOptions().position(loc).title(msg).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.stair)));
                    mMarker.setVisible(false);
                   LMList.add(new Landmarks(loc,msg,"Stairs",mMarker));
                }
            }


            @Override
            public void onCancelled(DatabaseError error) {
//                 Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }
    private void getOthers(){
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Map/Landmark/others");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
//                 This method is called once with the initial value and again
//                 whenever data at this location is updated.
//                Landmarks value = dataSnapshot.getValue(Landmarks.class);
//                Log.d(TAG, "Value is: " + value);


                for (DataSnapshot ds : dataSnapshot.getChildren()) {
//

                    String lat = ds.child("latLng/latitude").getValue().toString();
                    String lng = ds.child("latLng/longitude").getValue().toString();
                    String msg = ds.child("Title").getValue(String.class);

                    double latitude = Double.parseDouble(lat);
                    double longitude = Double.parseDouble(lng);
                    LatLng loc = new LatLng(latitude, longitude);
                    mMarker =mMap.addMarker(new MarkerOptions().position(loc).title(msg).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.other)));
                    mMarker.setVisible(false);
                    LMList.add(new Landmarks(loc,msg,"Others",mMarker));

                }
            }


            @Override
            public void onCancelled(DatabaseError error) {
//                 Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }
    private void getExits(){
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Map/Landmark/EXIT");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
//                 This method is called once with the initial value and again
//                 whenever data at this location is updated.
//                Landmarks value = dataSnapshot.getValue(Landmarks.class);
//                Log.d(TAG, "Value is: " + value);


                for (DataSnapshot ds : dataSnapshot.getChildren()) {
//

                    String lat = ds.child("latLng/latitude").getValue().toString();
                    String lng = ds.child("latLng/longitude").getValue().toString();
                    String msg = ds.child("Title").getValue(String.class);

                    double latitude = Double.parseDouble(lat);
                    double longitude = Double.parseDouble(lng);
                    LatLng loc = new LatLng(latitude, longitude);
                    mMarker = mMap.addMarker(new MarkerOptions().position(loc).title(msg).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.exit)));
                    mMarker.setVisible(false);
                    LMList.add(new Landmarks(loc,msg,"Exist doors",mMarker));

                }
            }


            @Override
            public void onCancelled(DatabaseError error) {
//                 Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }
    private void getWC(){
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Map/Landmark/WC");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
//                 This method is called once with the initial value and again
//                 whenever data at this location is updated.
//                Landmarks value = dataSnapshot.getValue(Landmarks.class);
//                Log.d(TAG, "Value is: " + value);


                for (DataSnapshot ds : dataSnapshot.getChildren()) {
//

                    String lat = ds.child("latLng/latitude").getValue().toString();
                    String lng = ds.child("latLng/longitude").getValue().toString();
                    String msg = ds.child("Title").getValue(String.class);

                    double latitude = Double.parseDouble(lat);
                    double longitude = Double.parseDouble(lng);
                    LatLng loc = new LatLng(latitude, longitude);

                    mMarker = mMap.addMarker(new MarkerOptions().position(loc).title(msg).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.wc)));
                    mMarker.setVisible(false);
                    LMList.add(new Landmarks(loc,msg,"WC",mMarker));
//                  mMap.addMarker(new MarkerOptions().position(loc).title(msg).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.classroom)));

                }
            }


            @Override
            public void onCancelled(DatabaseError error) {
//                 Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }
    private void getData1(){



        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        double radius=1;
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(WayfindingOverlayActivity.this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLatitude=locationResult.getLastLocation().getLatitude()+radius;
                currentLongitude=locationResult.getLastLocation().getLongitude()+radius;
            }
        }, Looper.getMainLooper());
        Location crntLocation=new Location("crntlocation");
        crntLocation.setLatitude(currentLatitude);
        crntLocation.setLongitude(currentLongitude);
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Map/WarningMsg/");
        myRef.addValueEventListener(new ValueEventListener() {
            Location wmloc;
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {


//                 This method is called once with the initial value and again
//                 whenever data at this location is updated.
//                Landmarks value = dataSnapshot.getValue(Landmarks.class);
//                Log.d(TAG, "Value is: " + value);
                //int i= 0;

                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    String lat = ds.child("latLng/latitude").getValue().toString();
                    String lng = ds.child("latLng/longitude").getValue().toString();
                    String msg = ds.child("Title").getValue(String.class);

                    double x,y;
//                    wm.setTitle(ds.child("Map/WarningMsg/Title").getValue(WrngMsg.class).getMsg());
//                    wm.setLat(ds.child("Map/WarningMsg/latLng/latitude").getValue(WrngMsg.class).getLat());
//                    wm.setLng(ds.child("Map/WarningMsg/latLng/Longitude").getValue(WrngMsg.class).getLng());

                    //convert to LatLng
//                    String la = ds.child("Map/WarningMsg/latLng/latitude").getValue().toString();
//                    String ln = ds.child("Map/WarningMsg/latLng/longitude").getValue().toString();
                    double latitude = Double.parseDouble(lat);
                    double longitude = Double.parseDouble(lng);
                    LatLng loc = new LatLng(latitude,longitude);

                    //display info
                    Log.d(TAG,"MEEEESSSSSGGG" + wm.getMsg());
                    Log.d(TAG,"LOCCCCCCCCCCC" + wm.getLoc());

                    //Saving into arraylist
                    WMList.add(new WrngMsg(msg,loc,false));
                    // WMList.add(""+wm.getLat());
                    //WMList.add(""+wm.getLng());


//
//                    wmloc=new Location("WM_location");
//                    wmloc.setLatitude(latitude);
//                    wmloc.setLongitude(longitude);
//
//                    LatLng l = new LatLng(latitude, longitude);


                      //  wm[i] = new WrngMsg(msg,loc);
                   // i++;
                  mMap.addMarker(new MarkerOptions().position(loc).title(msg).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.warningmsg)));
                    //if user location 5 meter of the object display box
//                    double a =calcdis(loc);
//                    if (a <= 5){
//                        // Display the dialog.
//                        AlertDialog.Builder builder = new AlertDialog.Builder(WayfindingOverlayActivity.this);
//                        builder.setTitle("Warning Message!!")
//                                .setMessage(msg)
//                                .setNeutralButton("OK", null);
//                        AlertDialog dialog = builder.create();
//                        dialog.show();
//                    }


                }
            }


            @Override
            public void onCancelled(DatabaseError error) {
//                 Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    public void ShowWrngMsg(){
        for (int counter = 0; counter < WMList.size(); counter++) {
           LatLng point =  WMList.get(counter).getLoc();
           String msg = WMList.get(counter).getMsg();
           boolean shown = WMList.get(counter).getShown();
           if (calcdis(point)<=5 && shown==false){
               WMList.get(counter).setShown(true);
               AlertDialog.Builder builder = new AlertDialog.Builder(WayfindingOverlayActivity.this);
               builder.setTitle("Warning Message!!")
                       .setMessage(msg)
                       .setNeutralButton("OK", null);
               AlertDialog dialog = builder.create();
               dialog.show();
               textToSpeech.speak(msg,TextToSpeech.QUEUE_FLUSH,null);
           }
        }
    }

    public  void WC_BTN_SHOW(View view){
        LandmarkType = "WC";
        LandmarkSort(LandmarkType);
    }
    public  void CS_BTN_SHOW(View view){
        LandmarkType = "Coffee Shops";
        LandmarkSort(LandmarkType);
    }
    public  void CR_BTN_SHOW(View view){
        LandmarkType = "Class Rooms";
        LandmarkSort(LandmarkType);
    }
    public  void Stairs_BTN_SHOW(View view){
        LandmarkType = "Stairs";
        LandmarkSort(LandmarkType);
    }
    public  void ED_BTN_SHOW(View view){
        LandmarkType = "Exist doors";
        LandmarkSort(LandmarkType);
    }
    public  void Others_BTN_SHOW(View view){
        LandmarkType = "Others";
        LandmarkSort(LandmarkType);
    }
    public void ShowNearbyLandmark(View view){
        for (int counter = 0; counter < LMList.size(); counter++){
            LatLng point =  LMList.get(counter).getLatLng();
            Marker m = LMList.get(counter).getMarker();
            if (calcdis(point)<=15){
                m.setVisible(true);
            }else m.setVisible(false);
        }
    }
    public void ShowAllLandmark(View view){
        for (int counter = 0; counter < LMList.size(); counter++){
            LatLng point =  LMList.get(counter).getLatLng();
            Marker m = LMList.get(counter).getMarker();
           m.setVisible(true);
        }
    }

    public void LandmarkSort(String t){
        for (int counter = 0; counter < LMList.size(); counter++) {
            String Type = LMList.get(counter).getType();
            LatLng point =  LMList.get(counter).getLatLng();
            String Title = LMList.get(counter).getTitle1();
            Marker m = LMList.get(counter).getMarker();
            switch (Type){
                case "Coffee Shops":
                  if(t.equalsIgnoreCase("Coffee Shops")){
                      m.setVisible(true);
                  }else m.setVisible(false);
                    break;
                case "Class Rooms":
                    if(t.equalsIgnoreCase("Class Rooms")){
                        m.setVisible(true);
                    }else m.setVisible(false);
                    break;
                case "Stairs" :
                    if(t.equalsIgnoreCase("Stairs")){
                        m.setVisible(true);
                    }else m.setVisible(false);
                    break;
                case "Others":
                    if(t.equalsIgnoreCase("Others")){
                        m.setVisible(true);
                    }else m.setVisible(false);
                    break;
                case "Exist doors":
                    if(t.equalsIgnoreCase("Exist doors")){
                        m.setVisible(true);
                    }else m.setVisible(false);
                    break;
                case "WC":
                    if(t.equalsIgnoreCase("WC")){
                        m.setVisible(true);
                    }else m.setVisible(false);
                    break;
                default:
                    m.setVisible(false);
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // remember to clean up after ourselves
        mIALocationManager.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // start receiving location updates & monitor region changes
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mListener);
        mIALocationManager.registerRegionListener(mRegionListener);
        mIALocationManager.registerOrientationListener(
                // update if heading changes by 1 degrees or more
                new IAOrientationRequest(1, 0),
                mOrientationListener);

        if (mWayfindingDestination != null) {
            mIALocationManager.requestWayfindingUpdates(mWayfindingDestination, mWayfindingListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister location & region changes
        mIALocationManager.removeLocationUpdates(mListener);
        mIALocationManager.unregisterRegionListener(mRegionListener);
        mIALocationManager.unregisterOrientationListener(mOrientationListener);

        if (mWayfindingDestination != null) {
            mIALocationManager.removeWayfindingUpdates();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        if (!ListExamplesActivity.checkLocationPermissions(this)) {
            finish(); // Handle permission asking in ListExamplesActivity
            return;
        }

        // do not show Google's outdoor location
        mMap.setMyLocationEnabled(false);
        mMap.setOnMapClickListener(this);

        // disable various Google maps UI elements that do not work indoors
        mMap.getUiSettings().setMapToolbarEnabled(false);

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                // ignore clicks to artificial wayfinding target markers
                if (marker == mDestinationMarker) return false;

                setWayfindingTarget(marker.getPosition(), false);
                // do not consume the event so that the popup with marker name is displayed
                return false;
            }
        });
    }

    private void setupPoIs(List<IAPOI> pois, int currentFloorLevel) {
        Log.d(TAG, pois.size() + " PoI(s)");
        // remove any existing markers
        for (Marker m : mPoIMarkers) {
            m.remove();
        }
        mPoIMarkers.clear();
        for (IAPOI poi : pois) {
            if (poi.getFloor() == currentFloorLevel) {
                mPoIMarkers.add(mMap.addMarker(new MarkerOptions()
                        .title(poi.getName())
                        .position(new LatLng(poi.getLocation().latitude, poi.getLocation().longitude))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));
            }
        }
    }

    /**
     * Sets bitmap of floor plan as ground overlay on Google Maps
     */
    private void setupGroundOverlay(IAFloorPlan floorPlan, Bitmap bitmap) {

        if (mGroundOverlay != null) {
            mGroundOverlay.remove();
        }

        if (mMap != null) {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
            IALatLng iaLatLng = floorPlan.getCenter();
            LatLng center = new LatLng(iaLatLng.latitude, iaLatLng.longitude);
            GroundOverlayOptions fpOverlay = new GroundOverlayOptions()
                    .image(bitmapDescriptor)
                    .zIndex(0.0f)
                    .position(center, floorPlan.getWidthMeters(), floorPlan.getHeightMeters())
                    .bearing(floorPlan.getBearing());

            mGroundOverlay = mMap.addGroundOverlay(fpOverlay);
        }
    }

    /**
     * Download floor plan using Picasso library.
     */
    private void fetchFloorPlanBitmap(final IAFloorPlan floorPlan) {

        if (floorPlan == null) {
            Log.e(TAG, "null floor plan in fetchFloorPlanBitmap");
            return;
        }

        final String url = floorPlan.getUrl();
        Log.d(TAG, "loading floor plan bitmap from "+url);

        mLoadTarget = new Target() {

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                loading=false;
                Log.d(TAG, "onBitmap loaded with dimensions: " + bitmap.getWidth() + "x"
                        + bitmap.getHeight());
                if (mOverlayFloorPlan != null && floorPlan.getId().equals(mOverlayFloorPlan.getId())) {
                    Log.d(TAG, "showing overlay");
                    setupGroundOverlay(floorPlan, bitmap);
                }
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                // N/A
            }

            @Override
            public void onBitmapFailed(Drawable placeHolderDrawable) {
                showInfo("Failed to load bitmap");
                mOverlayFloorPlan = null;
            }
        };

        RequestCreator request = Picasso.with(this).load(url);

        final int bitmapWidth = floorPlan.getBitmapWidth();
        final int bitmapHeight = floorPlan.getBitmapHeight();

        if (bitmapHeight > MAX_DIMENSION) {
            request.resize(0, MAX_DIMENSION);
        } else if (bitmapWidth > MAX_DIMENSION) {
            request.resize(MAX_DIMENSION, 0);
        }

        request.into(mLoadTarget);
    }

    private void showInfo(String text) {
        final Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), text,
                Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.button_close, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }

    @Override
    public void onMapClick(LatLng point) {
        if (mPoIMarkers.isEmpty()) {
            // if PoIs exist, only allow wayfinding to PoI markers
            setWayfindingTarget(point, true);
            SetUDes(point);
//            double m=measure(Ul,point);
//            String FinalText =""+m + " meters away";
//            db.setText(FinalText);
            location_Selected = true;
            SetDisplayBox(point);
        }
    }

    private void SetDisplayBox(LatLng a){
        if(mCircle != null){
        double m=measure(a,mCircle.getCenter());
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(2);
            String FinalText =""+df.format(m) + " meters away";
        db.setText(FinalText);}
    }




    public double measure( LatLng p, LatLng C ){
        // generally used geo measurement function
        //variables
        double lat2 , lat1, lon2, lon1;
        //convert single lanlat to 2 variables
        lat2 = p.latitude ;
        lon2 =  p.longitude;
        lat1 = C.latitude;
        lon1 = C.longitude;
        double R = 6378.137; // Radius of earth in KM
        double dLat = lat2 * Math.PI / 180 - lat1 * Math.PI / 180;
        double dLon = lon2 * Math.PI / 180 - lon1 * Math.PI / 180;
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;

        return d*1000 ; // meters

    }
    private double calcdis(LatLng a){
        if(mCircle != null){
            double m=measure(a,mCircle.getCenter());

            return m;
            //this will return distance between user and a point
        }
        return 999;
    }

    private void setWayfindingTarget(LatLng point, boolean addMarker) {
        if (mMap == null) {
            Log.w(TAG, "map not loaded yet");
            return;
        }

        mWayfindingDestination = new IAWayfindingRequest.Builder()
                .withFloor(mFloor)
                .withLatitude(point.latitude)
                .withLongitude(point.longitude)
                .build();

        mIALocationManager.requestWayfindingUpdates(mWayfindingDestination, mWayfindingListener);

        if (mDestinationMarker != null) {
            mDestinationMarker.remove();
            mDestinationMarker = null;
        }

        if (addMarker) {
            mDestinationMarker = mMap.addMarker(new MarkerOptions()
                    .position(point)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }
        Log.d(TAG, "Set destination: (" + mWayfindingDestination.getLatitude() + ", " +
                mWayfindingDestination.getLongitude() + "), floor=" +
                mWayfindingDestination.getFloor());

    }

    private boolean hasArrivedToDestination(IARoute route) {
        // empty routes are only returned when there is a problem, for example,
        // missing or disconnected routing graph
        if (route.getLegs().size() == 0) {
            return false;
        }

        final double FINISH_THRESHOLD_METERS = 8.0;
        double routeLength = 0;
        for (IARoute.Leg leg : route.getLegs()) routeLength += leg.getLength();
        return routeLength < FINISH_THRESHOLD_METERS;
    }

    /**
     * Clear the visualizations for the wayfinding paths
     */
    private void clearRouteVisualization() {
        for (Polyline pl : mPolylines) {
            pl.remove();
        }
        mPolylines.clear();
    }

    /**
     * Visualize the IndoorAtlas Wayfinding route on top of the Google Maps.
     */
    private void updateRouteVisualization() {

        clearRouteVisualization();

        if (mCurrentRoute == null) {
            return;
        }

        for (IARoute.Leg leg : mCurrentRoute.getLegs()) {

            if (leg.getEdgeIndex() == null) {
                // Legs without an edge index are, in practice, the last and first legs of the
                // route. They connect the destination or current location to the routing graph.
                // All other legs travel along the edges of the routing graph.

                // Omitting these "artificial edges" in visualization can improve the aesthetics
                // of the route. Alternatively, they could be visualized with dashed lines.
                continue;
            }

            PolylineOptions opt = new PolylineOptions();
            opt.add(new LatLng(leg.getBegin().getLatitude(), leg.getBegin().getLongitude()));
            opt.add(new LatLng(leg.getEnd().getLatitude(), leg.getEnd().getLongitude()));

            // Here wayfinding path in different floor than current location is visualized in
            // a semi-transparent color
            if (leg.getBegin().getFloor() == mFloor && leg.getEnd().getFloor() == mFloor) {
                opt.color(0xFF0000FF);
            } else {
                opt.color(0x300000FF);
            }

            mPolylines.add(mMap.addPolyline(opt));
        }
    }
}
