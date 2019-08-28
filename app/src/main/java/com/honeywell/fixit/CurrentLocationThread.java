package com.honeywell.fixit;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class CurrentLocationThread implements Runnable {

    private final LocationManager locationManager;
    private FirebaseFirestore db;
    private String uid;

    CurrentLocationThread(LocationManager _locationManager) {
        this.locationManager = _locationManager;
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        uid = mAuth.getCurrentUser().getUid();
    }

    public void run() {
        while (true){
            Criteria criteria = new Criteria();
            try{
                Location location = locationManager.getLastKnownLocation(locationManager
                        .getBestProvider(criteria, false));
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                db = FirebaseFirestore.getInstance();
                DocumentReference workerRef = db.collection("workers").document(uid);

                HashMap<String, Double> hashMap = new HashMap<>();
                hashMap.put("lat", latitude);
                hashMap.put("lng", longitude);




                workerRef
                        .update("location", hashMap)
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("failed", "Error updating workers location", e);
                            }
                        }).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.w("Location Changed!!!!!", "LOCATION CHANGED!!!");
                            }
                });
                try{
                    int timeInterval = 10000;
                    Thread.sleep(timeInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }catch (SecurityException e)  {
                Log.e("Exception: %s", e.getMessage());
            }
        }



    }
}
