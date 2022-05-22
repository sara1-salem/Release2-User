package com.indooratlas.android.sdk.examples;

import com.google.android.gms.maps.model.LatLng;

public class WrngMsg {

    String Title;
    LatLng Location;
    boolean MsgShown;
    double lng;
    double lat;

    public WrngMsg(String t, LatLng l, boolean shown){
        Title= t;
        Location = l;
        MsgShown = shown;

    }










    public WrngMsg(){
        Title= "";
        Location = null;
    }


    public LatLng getLoc() {
        return this.Location;
    }


    public String getMsg() {
        return this.Title;
    }




    public boolean getShown() {
        return this.MsgShown;
    }

    public void setShown(boolean b) {
        this.MsgShown = b;
    }
}
