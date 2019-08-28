package com.honeywell.fixit;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public void onNewToken(String token) {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db = FirebaseFirestore.getInstance();
            DocumentReference userDoc = db.collection("users").document(user.getUid());
            userDoc.update("token", token);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // Check if message contains a data payload.
        Log.d("MESSAGE", remoteMessage.getData().toString());

        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            Log.d("MESSAGE", data.keySet().toString());
            if(data.containsKey("status") && data.get("status").equals("completed")){
                requestCompleted();
            }
            else if(data.containsKey("status") && data.get("status").equals("closed")){
                requestClosed();
            }
            else if(data.containsKey("type") && data.get("type").equals("worker")) {
                sendWorkerNotification(data.get("request"));
            }
            else if(data.containsKey("type") && data.get("type").equals("customer")){
                sendCustomerNotification(data.get("request"));
            }
        }
    }

    private void requestCompleted(){
        Intent intent = new Intent("completeNotification");
        intent.putExtra("status", "completed");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void requestClosed(){
        Intent intent = new Intent("closedNotification");
        intent.putExtra("status", "closed");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    private void sendWorkerNotification(String msg) {
        Intent intent = new Intent("workerNotification");
        intent.putExtra("requestId", msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendCustomerNotification(String msg){
        Intent intent = new Intent("customerNotification");
        intent.putExtra("requestId", msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


}
