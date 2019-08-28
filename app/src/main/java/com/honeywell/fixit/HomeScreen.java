package com.honeywell.fixit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.NestedScrollView;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.content.Intent;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeScreen extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted = false;
    private Location mLastKnownLocation = null;
    private int DEFAULT_ZOOM = 18;
    GeoDataClient mGeoDataClient;
    PlaceDetectionClient mPlaceDetectionClient;
    FusedLocationProviderClient mFusedLocationProviderClient;
    private String TAG = "Error in map";
    private final int pullUpBarMinHeight = 55; //max and min heights in dp
    private final int pullUpBarMaxHeight = 350;
    private LatLng mDefaultLocation = new LatLng(12.344, 23.4454);
    private static JSONObject responseJson;
    private Context appContext;

    private String activeRequest = "none";
    private String assignedWorker = "none";

    private TextView cardTitle;
    private TextView cardContent;
    private View div1;
    private NestedScrollView pullUpBar;
    private FloatingActionButton newRequestBtn;
    private Button completeBtn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private double driverLat = 0;
    private double driverlng = 0;
    Thread driverUpdateThread = null;
    Thread allDriverUpdateThread = null;

    private LatLng customerLocation;
    private BroadcastReceiver customerRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String requestId = intent.getStringExtra("requestId");
            DocumentReference requestRef = db.collection("requests").document(requestId);
            requestRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot request = task.getResult();
                        if (request.exists()) {
                            foundWorkerActions(request.getData());
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
        }
    };

    private BroadcastReceiver completeRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String requestStatus = intent.getStringExtra("status");
            Toast.makeText(getApplicationContext(),"Request status: " + requestStatus ,Toast.LENGTH_LONG).show();

            completeBtn.setVisibility(View.VISIBLE);
            completeBtn.setEnabled(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        this.appContext = this;

        setContentView(R.layout.activity_home_screen);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        cardTitle = findViewById(R.id.cardTitle);
        cardContent = findViewById(R.id.cardContent);
        div1 = findViewById(R.id.cardBar);
        pullUpBar = findViewById(R.id.nestedScrollView);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        newRequestBtn = findViewById(R.id.pastRequestsFab);
        newRequestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openNewRequest = new Intent(view.getContext(), NewRequest.class);
                startActivityForResult(openNewRequest, 0);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Token assignment, check for active request
        final FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            final DocumentReference userDoc = db.collection("users").document(user.getUid());
            userDoc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String existingToken = (String) document.getData().get("token");
                            if (existingToken == null || existingToken == "") {
                                FirebaseInstanceId.getInstance().getInstanceId()
                                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                                if (!task.isSuccessful()) {
                                                    return;
                                                }

                                                // Get new Instance ID token
                                                String token = task.getResult().getToken();

                                                // Push to Firestore
                                                DocumentReference userDocNew = db.collection("users").document(mAuth.getCurrentUser().getUid());
                                                userDocNew.update("token", token);
                                            }
                                        });

                            }
                            final DocumentReference requestDoc2 = db.collection("customers").document(user.getUid());
                            requestDoc2.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if(task.isSuccessful()){
                                        DocumentSnapshot document = task.getResult();
                                        if (document.exists()) {
                                            HashMap<String, Double> userLocation = (HashMap<String, Double>) document.getData().get("location");
                                            customerLocation = new LatLng(userLocation.get("lat"), userLocation.get("lng"));
                                        }
                                    }
                                }
                            });

                            TextView userName = findViewById(R.id.userName);
                            TextView userEmail = findViewById(R.id.userEmail);

                            userName.setText(document.getData().get("fullName").toString());
                            userEmail.setText(user.getEmail());
                        }
                    }
                }
            });


            completeBtn = findViewById(R.id.completeBtn);
            completeBtn.setVisibility(View.GONE);
            completeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    completeBtn.setEnabled(false);
                    final DocumentReference customerDoc = db.collection("users").document(user.getUid());
                    customerDoc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    String actRequest = document.getData().get("activeRequest").toString();
                                    DocumentReference requestDoc = db.collection("requests").document(actRequest);
                                    requestDoc.update("status", "closed").addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // deactivate user request
                                            customerDoc.update("activeRequest", "none").addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    DocumentReference workerDoc = db.collection("users").document(assignedWorker);
                                                    workerDoc.update("activeRequest", "none").addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Toast.makeText(HomeScreen.this, "Job closed", Toast.LENGTH_SHORT).show();
                                                            noActiveRequestActions();
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        }
                    });

                }
            });
        }

        getLocationPermission();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this);

        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        //Fragment fragment = findViewById(R.id.fragmentMap);
        //FragmentActivity act = fragment.getAc
        //Intent intent = new Intent(this, MapsActivity.class);
        //startActivity(intent);
        //MapsActivity mapsActivity =


        LocalBroadcastManager.getInstance(this).registerReceiver(
                customerRequestReceiver, new IntentFilter("customerNotification"));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                completeRequestReceiver, new IntentFilter("completeNotification"));

    }

    @Override
    public void onResume() {
        super.onResume();
        cardTitle.setText("Loading...");

        //check for active request
        FirebaseUser user = mAuth.getCurrentUser();
        DocumentReference userDoc = db.collection("users").document(user.getUid());
        userDoc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        activeRequest = document.getData().get("activeRequest").toString();
                        if (activeRequest.matches("none") || activeRequest.matches("")){
                            noActiveRequestActions();
                            placeAllWorkers();
                            return;
                        } else {
                            // hide new request btn
                            CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) newRequestBtn.getLayoutParams();
                            p.setAnchorId(View.NO_ID);
                            newRequestBtn.setLayoutParams(p);
                            newRequestBtn.hide();
                            //check request for assigned worker
                            final DocumentReference requestDoc = db.collection("requests").document(activeRequest);
                            requestDoc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document.exists()) {
                                            assignedWorker = document.getData().get("worker").toString();
                                            if (assignedWorker.matches("none")) {
                                                placeAllWorkers();
                                                completeBtn.setVisibility(View.GONE);
                                                displayRequestInfo(true, document.getData());
                                                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) pullUpBar.getLayoutParams();
                                                params.height = (int) (pullUpBarMaxHeight * HomeScreen.this.getResources().getDisplayMetrics().density); // convert to dp
                                                pullUpBar.setLayoutParams(params);

                                            } else {
                                                foundWorkerActions(document.getData());
                                                displayRequestInfo(false, document.getData());
                                                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) pullUpBar.getLayoutParams();
                                                params.height = (int) (pullUpBarMaxHeight * HomeScreen.this.getResources().getDisplayMetrics().density); // convert to dp
                                                pullUpBar.setLayoutParams(params);
                                                driverUpdateThread = new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        double count = 0.0001;
                                                        while (true){
                                                            try{
                                                                updateDriverOnMap(assignedWorker);
                                                                count+=0.0001;
                                                                try{
                                                                    int timeInterval = 10000;
                                                                    Thread.sleep(timeInterval);
                                                                } catch (InterruptedException e) {
                                                                    e.printStackTrace();
                                                                }
                                                            }catch(Exception e){
                                                                Log.e("Exception", e.toString());
                                                            }

                                                        }

                                                    }
                                                });
                                                driverUpdateThread.start();
                                            }

                                        }
                                    }
                                }
                            });

                        }
                    }
                }
            }
        });

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_pastRequests) {
            Intent openPastRequests = new Intent(this, PastRequests.class);
            startActivityForResult(openPastRequests, 0);
        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        } else if (id == R.id.nav_logout) {
            logoutAction();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        String addy1 = "3390+Stratford+Road+NE+Atlanta+GA";
        String addy2 = "715+Peachtree+Street+NE+Atlanta+GA";



    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    updateLocationUI();
                    getDeviceLocation();
                }
            }

        }
    }


    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = (Location) task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    private void logoutAction() {
        DocumentReference userRef = db.collection("users").document(mAuth.getCurrentUser().getUid());
        userRef.update("token", "");
        mAuth.signOut();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Successfully signed out",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void noActiveRequestActions() {
        newRequestBtn.show();
        div1.setVisibility(View.GONE);
        cardContent.setVisibility(View.GONE);
        completeBtn.setVisibility(View.GONE);

        cardTitle.setText("No active request at this time");
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) pullUpBar.getLayoutParams();
        params.height =  (int) (pullUpBarMinHeight * HomeScreen.this.getResources().getDisplayMetrics().density); // convert to dp
        pullUpBar.setLayoutParams(params);
    }

    private void foundWorkerActions(Map request) {
        assignedWorker = request.get("worker").toString();
        if(allDriverUpdateThread!=null){
            allDriverUpdateThread.interrupt();
        }

        driverUpdateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                double count = 0.0001;
                while (true){
                    try{
                        updateDriverOnMap(assignedWorker);
                        count+=0.0001;
                        try{
                            int timeInterval = 10000;
                            Thread.sleep(timeInterval);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }catch(Exception e){
                        Log.e("Exception", e.toString());
                    }

                }

            }
        });
        driverUpdateThread.start();
        displayRequestInfo(false, request);
        displayRequestInfo(false, request);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) pullUpBar.getLayoutParams();
        params.height =  (int) (pullUpBarMaxHeight * HomeScreen.this.getResources().getDisplayMetrics().density); // convert to dp
        pullUpBar.setLayoutParams(params);
        div1.setVisibility(View.VISIBLE);
        cardContent.setVisibility(View.VISIBLE);
    }

    private void displayRequestInfo(final Boolean loading, final Map request) {
        if (loading) {
            String title = "Finding worker for you...";
            String content = "Request Title: " + request.get("title")
                    + "\nTag: " + request.get("tag")
                    + "\nComplexity: " + request.get("complexity")
                    + "\nTools Provided: " + request.get("toolsProvided")
                    + "\nDescription: " + request.get("description");

            cardTitle.setText(title);
            cardContent.setText(content);
            return;
        }
//        requestStr += "\n\nTitle: " + request.get("title")
//                + "\nTag: " + request.get("tag")
//                + "\nComplexity: " + request.get("complexity")
//                + "\nTools Provided: " + request.get("toolsProvided")
//                + "\nDescription: " + request.get("description");

        DocumentReference workerDoc = db.collection("users").document(request.get("worker").toString());
        workerDoc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Map worker = document.getData();
                        String title = "Worker: " + worker.get("fullName");
                        String content = "\nPhone: " + worker.get("phone")
                            + "\nRequest Title: " + request.get("title")
                            + "\nTag: " + request.get("tag")
                            + "\nComplexity: " + request.get("complexity")
                            + "\nTools Provided: " + request.get("toolsProvided")
                            + "\nDescription: " + request.get("description");

                        cardTitle.setText(title);
                        cardContent.setText(content);
                    }
                }
            }
        });
    }

    private void updateDriverOnMap(final String driverID){
        final DocumentReference userDoc = db.collection("workers").document(driverID);
        userDoc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    mMap.clear();
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        HashMap<String, Double> location = (HashMap<String, Double>) document.getData().get("location");
                        if (location != null) {
                            driverLat = location.get("lat");
                            driverlng = location.get("lng");

                            LatLng driverPosition = new LatLng(driverLat, driverlng);

                            GenerateRoute(driverPosition, customerLocation);
//                            VectorDrawable drawable = (VectorDrawable)ContextCompat.getDrawable(appContext, R.drawable.ic_tools_circle);
//                            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
//                                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                            Bitmap img = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.ic_tools);
                            mMap.addMarker(new MarkerOptions().position(driverPosition).title("Maintenance Worker").icon(BitmapDescriptorFactory.fromBitmap(img)));
                        }
                    }
                }
            }
        });
    }

    private void GenerateRoute(LatLng addy1, LatLng addy2){

        String stringUrl = FixitUtilities.GetUrl(addy1, addy2, "driving");
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, stringUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        try {
                            responseJson = new JSONObject(response);
                            String points = responseJson.getJSONArray("routes").getJSONObject(0).getJSONObject("overview_polyline").getString("points");
                            List<LatLng> latLngs = PolyUtil.decode(points);
                            PolylineOptions polylineOptions = new PolylineOptions().width(10).color(Color.BLUE).addAll(latLngs);

                            JSONArray legsArray = responseJson.getJSONArray("routes").getJSONObject(0).getJSONArray("legs");

                            //DONT NEED DRIVER MARKER
//                            LatLng start = new LatLng(legsArray.getJSONObject(0).getJSONObject("start_location").getDouble("lat"), legsArray.getJSONObject(0).getJSONObject("start_location").getDouble("lng"));
//                            mMap.addMarker(new MarkerOptions().position(start).title("Start"));

                            LatLng end = new LatLng(legsArray.getJSONObject(0).getJSONObject("end_location").getDouble("lat"), legsArray.getJSONObject(0).getJSONObject("end_location").getDouble("lng"));
                            mMap.addMarker(new MarkerOptions().position(end).title("Destination"));

                            mMap.addPolyline(polylineOptions);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //testText.setText("That didn't work!");
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void placeAllWorkers(){
        allDriverUpdateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    while (true){

                        final CollectionReference allWorkers = db.collection("workers");
                        allWorkers.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    mMap.clear();
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        HashMap<String, Double> location = (HashMap<String, Double>) document.getData().get("location");
                                        if (location != null) {
                                            driverLat = location.get("lat");
                                            driverlng = location.get("lng");

                                            LatLng driverPosition = new LatLng(driverLat, driverlng);
                                            Bitmap img = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.ic_tools);
                                            mMap.addMarker(new MarkerOptions().position(driverPosition).title("Maintenance Worker").icon(BitmapDescriptorFactory.fromBitmap(img)));
                                        }
                                    }
                                }

                            }
                        });
                        try{
                            int timeInterval = 10000;
                            Thread.sleep(timeInterval);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        };
                    }
                }catch(Exception e){
                    Log.w("Exception", e);
                }

            }
        });
        allDriverUpdateThread.start();

    }

}
