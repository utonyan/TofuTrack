package com.hideruu.tofutrack1;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import android.util.Log;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;

public class UploadActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ImageView uploadImage;
    private Button saveButton;
    private EditText uploadProd, uploadDesc, uploadGroup, uploadQty, uploadCost;
    private String imageURL;
    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        // Initialize Firestore and enable offline persistence
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // Enable offline persistence
                .build();
        db.setFirestoreSettings(settings);

        // Initialize UI elements
        uploadImage = findViewById(R.id.uploadImage);
        uploadProd = findViewById(R.id.uploadProd);
        uploadDesc = findViewById(R.id.uploadDesc);
        uploadGroup = findViewById(R.id.uploadGroup);
        uploadQty = findViewById(R.id.uploadQty);
        uploadCost = findViewById(R.id.uploadCost);
        saveButton = findViewById(R.id.saveButton);

        // Set up ActivityResultLauncher for image picking
        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            uri = Objects.requireNonNull(data).getData();
                            uploadImage.setImageURI(uri);
                        } else {
                            Toast.makeText(UploadActivity.this, "No Image Selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Image upload button click listener
        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoPicker = new Intent(Intent.ACTION_PICK);
                photoPicker.setType("image/*");
                activityResultLauncher.launch(photoPicker);
            }
        });

        // Save button click listener
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveData();
            }
        });
    }

    // Function to save data
    private void saveData() {
        String prodName = uploadProd.getText().toString().trim();
        String prodDesc = uploadDesc.getText().toString().trim();
        String prodGroup = uploadGroup.getText().toString().trim();
        String prodQtyStr = uploadQty.getText().toString().trim();
        String prodCostStr = uploadCost.getText().toString().trim();

        // Validate input
        if (prodName.isEmpty() || prodDesc.isEmpty() || prodGroup.isEmpty() || prodQtyStr.isEmpty() || prodCostStr.isEmpty() || uri == null) {
            Toast.makeText(UploadActivity.this, "Please fill all fields and select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert quantity and cost to their respective types
        int prodQty = Integer.parseInt(prodQtyStr);
        double prodCost = Double.parseDouble(prodCostStr);
        double prodTotalPrice = prodQty * prodCost; // Calculate total price

        if (isNetworkAvailable()) {
            // If network is available, upload image and data to Firestore
            uploadImageToFirebase(uri, prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice);
        } else {
            // Save image locally and schedule upload when network is available
            saveImageLocally(uri);
            scheduleImageUploadTask(prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice);
            Toast.makeText(UploadActivity.this, "Saved locally, will upload when online", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // Upload image to Firebase Storage
    private void uploadImageToFirebase(Uri uri, String prodName, String prodDesc, String prodGroup, int prodQty, double prodCost, double prodTotalPrice) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("ProductImages")
                .child(Objects.requireNonNull(uri.getLastPathSegment()));

        AlertDialog.Builder builder = new AlertDialog.Builder(UploadActivity.this);
        builder.setCancelable(false);
        builder.setView(R.layout.progress_layout);
        AlertDialog dialog = builder.create();
        dialog.show();

        storageReference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        imageURL = uri.toString();
                        addDataToFirestore(prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice, imageURL);
                        dialog.dismiss();
                        Toast.makeText(UploadActivity.this, "Saved Successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                dialog.dismiss();
                Toast.makeText(UploadActivity.this, "Failed to save data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Function to add data to Firestore
    private void addDataToFirestore(String prodName, String prodDesc, String prodGroup, int prodQty, double prodCost, double prodTotalPrice, String imageURL) {
        Date dateAdded = new Date(); // Set the current date and time

        DataClass data = new DataClass(prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice, imageURL, dateAdded);

        db.collection("products").add(data).addOnSuccessListener(documentReference -> {
            Log.d("Firestore", "DocumentSnapshot added with ID: " + documentReference.getId());
        }).addOnFailureListener(e -> {
            Log.w("Firestore", "Error adding document", e);
        });
    }

    // Function to save image locally
    private void saveImageLocally(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File file = new File(getCacheDir(), "temp_image.jpg");
            OutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Check if network is available
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    // Schedule image upload task when network is available
    private void scheduleImageUploadTask(String prodName, String prodDesc, String prodGroup, int prodQty, double prodCost, double prodTotalPrice) {
        Data inputData = new Data.Builder()
                .putString("prodName", prodName)
                .putString("prodDesc", prodDesc)
                .putString("prodGroup", prodGroup)
                .putInt("prodQty", prodQty)
                .putDouble("prodCost", prodCost)
                .putDouble("prodTotalPrice", prodTotalPrice)
                .putString("image_path", new File(getCacheDir(), "temp_image.jpg").getAbsolutePath())
                .build();

        OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(ImageUploadWorker.class)
                .setInputData(inputData)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();

        WorkManager.getInstance(this).enqueue(uploadWorkRequest);
    }
}
