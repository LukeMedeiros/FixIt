package com.honeywell.fixit;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import static android.support.constraint.Constraints.TAG;

public class LoadingActivity extends Activity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_loading);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        loginCheck(currentUser);


    }

    public void loginCheck(FirebaseUser user) {
        if (user != null) {
            String uid = user.getUid();
            DocumentReference userDoc = db.collection("users").document(uid);
            userDoc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            startIntent((String) document.getData().get("role"));
                        } else {
                            startIntent(null);
                        }
                    } else {
                        startIntent(null);
                    }
                }
            });
        } else {
            startIntent(null);
        }
    }

    public void startIntent(String role) {
        if (role != null) {
            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            if (!task.isSuccessful()) {
                                return;
                            }

                            String token = task.getResult().getToken();

                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                db = FirebaseFirestore.getInstance();
                                DocumentReference userDoc = db.collection("users").document(user.getUid());
                                userDoc.update("token", token);
                            }
                        }
                    });
            if (role.equals("customer")) {
                // Change this later
                Intent intent = new Intent(this, HomeScreen.class);
                startActivity(intent);
            } else if (role.equals("worker")) {
                Intent intent = new Intent(this, MWHomeScreen.class);
                startActivity(intent);
            }
        } else {

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }
}
