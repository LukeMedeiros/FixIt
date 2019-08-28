package com.honeywell.fixit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.Map;

public class PastRequests extends AppCompatActivity {
    private TextView pastRequestsText;
    private FloatingActionButton pastRequestsFab;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String defaultMsg = "";
    private String role = "";
    private boolean hasPastRequests = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_requests);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pastRequestsFab = findViewById(R.id.pastRequestsFab);
        pastRequestsFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openNewRequest = new Intent(view.getContext(), NewRequest.class);
                startActivityForResult(openNewRequest, 0);
            }
        });

        pastRequestsText = findViewById(R.id.pastRequestsText);
    }

    @Override
    public void onResume() {
        super.onResume();
        pastRequestsText.setText("Loading " + defaultMsg + "...");

        final String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Map docMap = document.getData();
                        role = docMap.get("role").toString();
                        if (role.matches("customer")) {
                            defaultMsg = "past requests";
                            if (!docMap.get("activeRequest").toString().matches("none")) {
                                CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) pastRequestsFab.getLayoutParams();
                                p.setAnchorId(View.NO_ID);
                                pastRequestsFab.setLayoutParams(p);
                                pastRequestsFab.hide();
                            } else {
                                pastRequestsFab.show();
                            }
                        } else {
                            defaultMsg = "past jobs";
                            CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) pastRequestsFab.getLayoutParams();
                            p.setAnchorId(View.NO_ID);
                            pastRequestsFab.setLayoutParams(p);
                            pastRequestsFab.hide();
                        }

                        db.collection("requests")
                            .whereEqualTo(role, uid)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        if (task.getResult().isEmpty()) {
                                            pastRequestsText.setText("No " + defaultMsg);
                                            return;
                                        }
                                        pastRequestsText.setText("");
                                        hasPastRequests = false;
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            displayPastRequest(document.getData());
                                        }
                                        if (!hasPastRequests) {
                                            pastRequestsText.setText("No " +  defaultMsg);
                                        }
                                    } else {
                                        pastRequestsText.setText("Failed to load " + defaultMsg);
                                        Log.d("past request error", "Error getting documents: ", task.getException());
                                    }
                                }
                            });
                    }
                }
            }
        });
    }

    private void displayPastRequest(final Map request) {
        if (!request.get("status").toString().matches("closed")) {
            return;
        }

        hasPastRequests = true;
        String oppositeRoleStr = role.matches("customer") ? "worker" : "customer";
        String oppositeRoleId = request.get(oppositeRoleStr).toString();
        DocumentReference oppositeRoleDoc = db.collection("users").document(oppositeRoleId);

        oppositeRoleDoc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot oppositeRole = task.getResult();
                    if (oppositeRole.exists()) {
                        Map oppositeRoleMap = oppositeRole.getData();
                        byte[] decodedString = Base64.decode(request.get("image").toString(), Base64.DEFAULT);
                        Bitmap decodedImg = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                        String requestStr = "Title: " + request.get("title")
                                + "\n" + (role.matches("customer") ? "Worker" : "Client") + ": " + oppositeRoleMap.get("fullName")
                                + "\nPhone: " + oppositeRoleMap.get("phone")
                                + "\nTag: " + request.get("tag")
                                + "\nComplexity: " + request.get("complexity")
                                + "\nTools Provided: " + request.get("toolsProvided")
                                + "\nDescription: " + request.get("description")
                                + "\nImage: ";

                        SpannableStringBuilder builder = new SpannableStringBuilder();

                        builder.append(requestStr).append(" ");
                        builder.setSpan(new ImageSpan(decodedImg), builder.length() - 1, builder.length(), 0);

                        pastRequestsText.append(builder);
                        pastRequestsText.append("\n\n");
                    }
                }
            }
        });
    }
}
