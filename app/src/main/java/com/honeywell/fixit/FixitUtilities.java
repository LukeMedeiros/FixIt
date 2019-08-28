package com.honeywell.fixit;


import com.google.android.gms.maps.model.LatLng;

final class FixitUtilities {

    static String GetUrl(LatLng address1, LatLng address2, String travelMethod){
        String str_origin = "origin="+address1.latitude+","+address1.longitude;
        String str_destination = "destination="+address2.latitude+","+address2.longitude;
        String mode = "mode="+travelMethod;

        String parameters = str_origin + "&" + str_destination + "&" + mode;
        String output = "json";

        String url = "https://maps.googleapis.com/maps/api/directions/"+ output + "?" + parameters + "&key=" + "AIzaSyA_8WGHu66eQHF3TzXCgKo5dJICISZXddU";
        return  url;
    }
}
