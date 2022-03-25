package com.indooratlas.android.sdk.examples.geofence;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.indooratlas.android.sdk.IAGeofence;
import com.indooratlas.android.sdk.IAGeofenceEvent;
import com.indooratlas.android.sdk.IAGeofenceListener;
import com.indooratlas.android.sdk.IAGeofenceRequest;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.IARoute;
import com.indooratlas.android.sdk.IAWayfindingListener;
import com.indooratlas.android.sdk.IAWayfindingRequest;
import com.indooratlas.android.sdk.examples.ListExamplesActivity;
import com.indooratlas.android.sdk.examples.NearbyLandmarks;
import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.SdkExample;
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IALatLng;
import com.indooratlas.android.sdk.resources.IALatLngFloorCompatible;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GeofenceMapsOverlayActivity extends FragmentActivity implements LocationListener, OnMapReadyCallback {
Button goToNearbyLandmark;
    public static  double currentLatitude;
    public static  double currentLongitude;
    private static final String TAG = "IndoorAtlasExample";
    Button currentLocation;
    /* used to decide when bitmap should be downscaled */
    private static final int MAX_DIMENSION = 2048;
    FirebaseDatabase database;
    DatabaseReference myRef;
    private static final double GEOFENCE_RADIUS_METERS = 5.0;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Circle mCircle;
    private Marker mMarker;
    private IARegion mOverlayFloorPlan = null;
    private GroundOverlay mGroundOverlay = null;
    private IALocationManager mIALocationManager;
    private Target mLoadTarget;
    private boolean mCameraPositionNeedsUpdating = true; // update on first location
    private boolean mShowIndoorLocation = false;
    LocationRequest locationRequest;
    IALocation mLatestLocation = null;
    FusedLocationProviderClient fusedLocationProviderClient;


    private void showBlueDot(LatLng center, double accuracyRadius, double bearing) {
        if (mMap != null) {

            int dotColor = 0x31812016; // red-ish for outdoors
            if (mShowIndoorLocation) {
                dotColor = 0x201681FB;
            }

            if (mCircle == null) {
                // location can received before map is initialized, ignoring those updates

                mCircle = mMap.addCircle(new CircleOptions()
                        .center(center)
                        .radius(accuracyRadius)
                        .fillColor(dotColor)
                        .strokeColor(0x500A78DD)
                        .zIndex(1.0f)
                        .visible(true)
                        .strokeWidth(5.0f));
                mMarker = mMap.addMarker(new MarkerOptions()
                        .position(center)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_blue_dot))
                        .anchor(0.5f, 0.5f)
                        .rotation((float) bearing)
                        .flat(true));
            } else {
                // move existing markers position to received location
                mCircle.setCenter(center);
                mCircle.setRadius(accuracyRadius);
                mCircle.setFillColor(dotColor);
                mMarker.setPosition(center);
                mMarker.setRotation((float) bearing);
            }
        }
    }

    /**
     * Listener that handles location change events.
     */
    private IALocationListener mListener = new IALocationListenerSupport() {

        /**
         * Location changed, move marker and camera position.
         */
        @Override
        public void onLocationChanged(IALocation location) {

//            if (!mGuideShown) {
//                mGuideShown = true;
//                Toast.makeText(GeofenceMapsOverlayActivity.this,
//                        "Long-touch to add a geofence",
//                        Toast.LENGTH_LONG).show();
//            }

            mLatestLocation = location;

            Log.d(TAG, "new location received with coordinates: " + location.getLatitude()
                    + "," + location.getLongitude());

            if (mMap == null) {
                // location received before map is initialized, ignoring update here
                return;
            }

            final LatLng center = new LatLng(location.getLatitude(), location.getLongitude());

            if (mShowIndoorLocation) {
                showBlueDot(center, location.getAccuracy(), location.getBearing());
            }

            // our camera position needs updating if location has significantly changed
            if (mCameraPositionNeedsUpdating) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 17.5f));
                mCameraPositionNeedsUpdating = false;
            }
        }
    };

    /**
     * Listener that changes overlay if needed
     */
    private IARegion.Listener mRegionListener = new IARegion.Listener() {
        @Override
        public void onEnterRegion(IARegion region) {
            Log.d(TAG, "trace ID:" + mIALocationManager.getExtraInfo().traceId);
            if (region.getType() == IARegion.TYPE_FLOOR_PLAN) {


                if (mGroundOverlay == null || !region.equals(mOverlayFloorPlan)) {
                    mCameraPositionNeedsUpdating = true; // entering new fp, need to move camera
                    if (mGroundOverlay != null) {
                        mGroundOverlay.remove();
                        mGroundOverlay = null;
                    }
                    mOverlayFloorPlan = region; // overlay will be this (unless error in loading)
                    fetchFloorPlanBitmap(region.getFloorPlan());
                } else {
                    mGroundOverlay.setTransparency(0.0f);
                }

                mShowIndoorLocation = true;
//                showInfo("Showing IndoorAtlas SDK's location output");
            }

        }

        @Override
        public void onExitRegion(IARegion region) {
            if (mGroundOverlay != null) {
                // Indicate we left this floor plan but leave it there for reference
                // If we enter another floor plan, this one will be removed and another one loaded
                mGroundOverlay.setTransparency(0.5f);
            }

            mShowIndoorLocation = false;

        }

    };

    @Override
    public void onLocationChanged(@NonNull Location location) {
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Map/Landmark/");
        myRef.addValueEventListener(new ValueEventListener() {
            String landmark;
            double longitude;
            double latitude;
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
//                 This method is called once with the initial value and again
//                 whenever data at this location is updated.
                Landmarks value = dataSnapshot.getValue(Landmarks.class);
                Log.d(TAG, "Value is: " + value);
        for (DataSnapshot ds : dataSnapshot.getChildren()) {
//
            String lat = ds.child("latLng/latitude").getValue().toString();
            String lng = ds.child("latLng/longitude").getValue().toString();
             landmark = ds.child("Title").getValue(String.class);

             latitude = Double.parseDouble(lat);
             longitude = Double.parseDouble(lng);
        }
                double currentLatitude=location.getLatitude();
                double currentLongitude=location.getLongitude();

                double latitude1=latitude;
                double longitude1=longitude;
                float distance;
                Location crntLocation=new Location("crntlocation");
                crntLocation.setLatitude(currentLatitude);
                crntLocation.setLongitude(currentLongitude);

                Location newLocation=new Location("newlocation");
                newLocation.setLatitude(latitude1);
                newLocation.setLongitude(longitude1);
                distance =crntLocation.distanceTo(newLocation);
                Toast.makeText(GeofenceMapsOverlayActivity.this, "The distance from your current location to "+landmark+" is"+distance, Toast.LENGTH_SHORT).show();

                }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        if (!mShowIndoorLocation) {
            Log.d(TAG, "new LocationService location received with coordinates: " + location.getLatitude()
                    + "," + location.getLongitude());

            showBlueDot(
                    new LatLng(location.getLatitude(), location.getLongitude()),
                    location.getAccuracy(),
                    location.getBearing());


        }

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        currentLocation=(Button) findViewById(R.id.currentLocation);
        // prevent the screen going to sleep while app is on foreground
        findViewById(android.R.id.content).setKeepScreenOn(true);

        // instantiate IALocationManager
        mIALocationManager = IALocationManager.create(this);

        startListeningPlatformLocations();

        // Try to obtain the map from the SupportMapFragment.
        ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map))
                .getMapAsync(this);
        //to retrieve all markers from database to map
        getData();
        //to retrieve all warning msgs from database to map

        getData1();
        currentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMyCurrentLocation();
            }
        });

        goToNearbyLandmark=findViewById(R.id.NearbyLandmark);
        goToNearbyLandmark.setOnClickListener(v -> {
            Intent myintent = new Intent(GeofenceMapsOverlayActivity.this, NearbyLandmarks.class);
            startActivity(myintent);
        });

    }
    private void getData(){
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Map/Landmark/");
        myRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
//                 This method is called once with the initial value and again
//                 whenever data at this location is updated.
                Landmarks value = dataSnapshot.getValue(Landmarks.class);
                Log.d(TAG, "Value is: " + value);


                for (DataSnapshot ds : dataSnapshot.getChildren()) {
//

                    String lat = ds.child("latLng/latitude").getValue().toString();
                    String lng = ds.child("latLng/longitude").getValue().toString();
                    String msg = ds.child("Title").getValue(String.class);

                    double latitude = Double.parseDouble(lat);
                    double longitude = Double.parseDouble(lng);
                    LatLng loc = new LatLng(latitude, longitude);
                    mMap.addMarker(new MarkerOptions().position(loc).title(msg));
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
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(GeofenceMapsOverlayActivity.this);
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
                Landmarks value = dataSnapshot.getValue(Landmarks.class);
                Log.d(TAG, "Value is: " + value);


                for (DataSnapshot ds : dataSnapshot.getChildren()) {
//

                    String lat = ds.child("latLng/latitude").getValue().toString();
                    String lng = ds.child("latLng/longitude").getValue().toString();
                    String msg = ds.child("Title").getValue(String.class);

                    double latitude = Double.parseDouble(lat);
                    double longitude = Double.parseDouble(lng);
                    wmloc=new Location("WM_location");
                    wmloc.setLatitude(latitude);
                    wmloc.setLongitude(longitude);
                    LatLng loc = new LatLng(latitude, longitude);
                    mMap.addMarker(new MarkerOptions().position(loc).title(msg).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.warningmsg)));
                     if (crntLocation == wmloc) {
                         // Display the dialog.
                         AlertDialog.Builder builder = new AlertDialog.Builder(GeofenceMapsOverlayActivity.this);
                         builder.setTitle("Warning Message!!")
                                 .setMessage(msg)
                                 .setNeutralButton("OK", null);
                         AlertDialog dialog = builder.create();
                         dialog.show();
                     }
                }
            }


            @Override
            public void onCancelled(DatabaseError error) {
//                 Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }
    private void showMyCurrentLocation() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(GeofenceMapsOverlayActivity.this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Toast.makeText(GeofenceMapsOverlayActivity.this,"Location: "+locationResult.getLastLocation().getLongitude()+" , "+locationResult.getLastLocation().getLatitude(),Toast.LENGTH_LONG).show();
            }
        }, Looper.getMainLooper());
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

    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister location & region changes
        mIALocationManager.removeLocationUpdates(mListener);
        mIALocationManager.unregisterRegionListener(mRegionListener);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

        if (!ListExamplesActivity.checkLocationPermissions(this)) {
            finish(); // Handle permission asking in ListExamplesActivity
            return;
        }

        // do not show Google's outdoor location
        mMap.setMyLocationEnabled(false);


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

        final String url = floorPlan.getUrl();
        mLoadTarget = new Target() {

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Log.d(TAG, "onBitmap loaded with dimensions: " + bitmap.getWidth() + "x"
                        + bitmap.getHeight());
                if (mOverlayFloorPlan != null && floorPlan.getId().equals(mOverlayFloorPlan.getId())) {
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

    private void startListeningPlatformLocations() {
        try{
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Platform location permissions not granted",
                    Toast.LENGTH_LONG).show();
        }

    }
}
