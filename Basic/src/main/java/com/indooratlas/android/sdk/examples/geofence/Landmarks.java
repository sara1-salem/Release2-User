package com.indooratlas.android.sdk.examples.geofence;
import com.google.android.gms.maps.model.LatLng;

public class Landmarks {

    public LatLng latLng;
    public String Title;
    //String LDes;

    public Landmarks(LatLng latlng, String t) {
        latLng = latlng;
        Title = t;
        // LDes = d;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public Landmarks(){

    }

    public String getTitle1() {
        return Title;
    }
}
