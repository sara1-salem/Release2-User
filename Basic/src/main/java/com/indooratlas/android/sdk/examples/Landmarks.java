package com.indooratlas.android.sdk.examples;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class Landmarks {

    public LatLng latLng;
    public String titli;
    public Marker marker;
    public String Type;
    //String LDes;


    public Landmarks(LatLng latlng, String t, String ty,Marker m ) {
        latLng = latlng;
        titli = t;
        Type = ty;
        marker = m;
        // LDes = d;
    }

    public Marker getMarker() {
        return marker;
    }

    public String getType() {
        return Type;
    }



    public LatLng getLatLng() {
        return latLng;
    }



    public String getTitle1() {
        return titli;
    }


}
