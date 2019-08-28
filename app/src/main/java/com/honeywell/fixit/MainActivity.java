package com.honeywell.fixit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.support.annotation.NonNull;
import android.widget.TextView;
import android.widget.Button;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView usernameTextview;
    private TextView passwordTextview;
    private Button loginButton;
    private Toast singingInToast;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        usernameTextview = findViewById(R.id.username_textview);
        passwordTextview = findViewById(R.id.password_textview);
        loginButton = findViewById(R.id.login_button);

        singingInToast = Toast.makeText(this, "Signing in...", Toast.LENGTH_SHORT);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loginButton.setEnabled(true);
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
                            // Can't find the current user's information
                        }
                    } else {
                        // exception
                    }
                }
            });
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
                singingInToast.cancel();
                Toast.makeText(this, "Signed in as a customer.",
                        Toast.LENGTH_SHORT).show();
                // Change this later
                Intent intent = new Intent(this, HomeScreen.class);
                startActivity(intent);
            } else if (role.equals("worker")) {
                singingInToast.cancel();
                Toast.makeText(this, "Signed in as a maintenance worker.",
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MWHomeScreen.class);
                startActivity(intent);
            } else {
                // role not defined
            }
        }
    }

    public void LoginClicked(View view){
        loginButton.setEnabled(false);

        String email = usernameTextview.getText().toString();
        String password = passwordTextview.getText().toString();

        if(!email.equals("") && !password.equals("") ){
            singingInToast.show();
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                FirebaseUser user = mAuth.getCurrentUser();
                                loginCheck(user);
                            } else {
                                // If sign in fails, display a message to the user.
                                singingInToast.cancel();
                                Toast.makeText(MainActivity.this, "Authentication failed. Please try again.",
                                        Toast.LENGTH_SHORT).show();
                                loginButton.setEnabled(true);
                            }
                        }
                    });
        }

        else{
            Toast.makeText(MainActivity.this, "Please enter a username and password.",
                    Toast.LENGTH_SHORT).show();
        }
    }


}
