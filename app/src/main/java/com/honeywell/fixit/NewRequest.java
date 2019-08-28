package com.honeywell.fixit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NewRequest extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText titleText, descriptionText;
    private ImageView img;
    private Bitmap bitmap;
    private Spinner tagSpinner, complexitySpinner, toolsSpinner;
    private Button btnChoose, btnSubmit;
    private Uri filePath;
    private final int PICK_IMAGE_REQUEST = 71;
    private Toast loadingToast;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_request);

        titleText = findViewById(R.id.titleText);
        descriptionText = findViewById(R.id.descriptionText);


        String[] tags = new String[] {"Electrical", "General Maintenance", "Painting", "Plumbing"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, tags);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tagSpinner = findViewById(R.id.tagSpinner);
        tagSpinner.setAdapter(adapter);

        String[] complexities = new String[] {"Simple", "Medium", "Hard"};
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, complexities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        complexitySpinner = findViewById(R.id.complexitySpinner);
        complexitySpinner.setAdapter(adapter);

        String[] tools = new String[] {"Yes", "No"};
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, tools);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        toolsSpinner = findViewById(R.id.toolsSpinner);
        toolsSpinner.setAdapter(adapter);

        btnChoose = findViewById(R.id.btnChoose);
        btnSubmit = findViewById(R.id.btnSubmit);
        img = findViewById(R.id.img);

        btnChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (titleText.getText().toString().matches("") || descriptionText.getText().toString().matches("") || bitmap == null) {
                    Toast.makeText(NewRequest.this, "Please Enter All Fields", Toast.LENGTH_SHORT).show();
                } else {
                    loadingToast = Toast.makeText(NewRequest.this, "Submitting request...", Toast.LENGTH_LONG);
                    loadingToast.show();
                    submitData();
                }
            }
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null )
        {
            filePath = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                img.setImageBitmap(bitmap);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void submitData() {
        final String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> request = new HashMap<>();
        request.put("customer", uid);
        request.put("worker", "none");
        request.put("title", titleText.getText().toString());
        request.put("description", descriptionText.getText().toString());
        request.put("complexity", complexitySpinner.getSelectedItem().toString());
        request.put("tag", tagSpinner.getSelectedItem().toString());
        request.put("toolsProvided", toolsSpinner.getSelectedItem().toString());
        request.put("status", "in progress");

       double size = bitmap.getByteCount();
       ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
       bitmap.compress(Bitmap.CompressFormat.JPEG, Math.max((int)(Math.pow(2, 20)/size*0.75*100), 100), byteArrayOutputStream);
       byte[] byteArray = byteArrayOutputStream.toByteArray();
       String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
       request.put("image", encoded);

        final DocumentReference requestRef = db.collection("requests").document();

        requestRef
            .set(request)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    DocumentReference userRef = db.collection("users").document(uid);

                    userRef
                        .update("activeRequest", requestRef.getId())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                loadingToast.cancel();
                                Toast.makeText(NewRequest.this, "Submitted successfully!", Toast.LENGTH_SHORT).show();
                                Intent openHomeScreen = new Intent(NewRequest.this, HomeScreen.class);
                                startActivityForResult(openHomeScreen, 1);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("failed", "Error updating user's active request", e);
                            }
                        });
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w("failed", "Error adding document", e);
                }
            });

    }
}
