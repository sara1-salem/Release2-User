package com.indooratlas.android.sdk.examples;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.geofence.GeofenceMapsOverlayActivity;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;

import java.util.ArrayList;

public class NearbyLandmarks extends AppCompatActivity {
    SwipeRefreshLayout swipeRefreshLayout;
    RecyclerView recyclerView;
    Adapter adapter;
    FirebaseDatabase database;
    DatabaseReference myRef;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    public static  double currentLatitude1;
    public static  double currentLongitude1;

    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.nearby_layout);
        database = FirebaseDatabase.getInstance();
        swipeRefreshLayout=findViewById(R.id.swip);
        recyclerView=findViewById(R.id.rv);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager manager=new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);
        adapter= new Adapter(this);
        recyclerView.setAdapter(adapter);


        LoadData();



    }


    private void LoadData(){
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Map/Landmark/");
//////////////////////////////////////////////////////
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(NearbyLandmarks.this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                 currentLatitude1=locationResult.getLastLocation().getLatitude();
                 currentLongitude1=locationResult.getLastLocation().getLongitude();
            }
        }, Looper.getMainLooper());
        ///////////////////////////////////////////////
        myRef.addValueEventListener(new ValueEventListener() {
            String landmark;
            double longitude;
            double latitude;
            double distance;
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
//                 This method is called once with the initial value and again
//                 whenever data at this location is updated.
//                Landmarks value = dataSnapshot.getValue(Landmarks.class);
                ArrayList<String> MSGS=new ArrayList<>();
                ArrayList<Double> distances=new ArrayList<Double>();
                Location crntLocation=new Location("crntlocation");
                crntLocation.setLatitude(currentLatitude1);
                crntLocation.setLongitude(currentLongitude1);

                Location newLocation=new Location("newlocation");
                newLocation.setLatitude(latitude);
                newLocation.setLongitude(longitude);
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String lat = ds.child("latLng/latitude").getValue().toString();
                    String lng = ds.child("latLng/longitude").getValue().toString();
                    landmark = ds.child("Title").getValue(String.class);
                    latitude = Double.parseDouble(lat);
                    longitude = Double.parseDouble(lng);

                    distance =crntLocation.distanceTo(newLocation);

                    MSGS.add(landmark);
                    distances.add( distance);
                }
                adapter.setItems(MSGS);
                adapter.setItem(distances);
                adapter.notifyDataSetChanged();





//                Location crntLocation=new Location("crntlocation");
//                crntLocation.setLatitude(currentLatitude);
//                crntLocation.setLongitude(currentLongitude);
//
//                Location newLocation=new Location("newlocation");
//                newLocation.setLatitude(latitude);
//                newLocation.setLongitude(longitude);
//                distance =crntLocation.distanceTo(newLocation);
//                for (DataSnapshot ds : dataSnapshot.getChildren()) {
//                    distances.add((double) distance);
//                }
//                adapter.setItem(distances);
                Toast.makeText(NearbyLandmarks.this, "The distance from your current location to "+landmark+" is"+distance, Toast.LENGTH_SHORT).show();
            }


            @Override
            public void onCancelled(DatabaseError error) {
//                 Failed to read value
            }
        });
    }}




