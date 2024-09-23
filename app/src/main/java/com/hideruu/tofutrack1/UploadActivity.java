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
import java.util.UUID;

public class UploadActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ImageView uploadImage;
    private Button saveButton;
    private EditText uploadProd, uploadDesc, uploadGroup, uploadQty, uploadCost;
    private String imageURL;
    private Uri uri;
    private String uniqueImageName;
    private String productId;  // New field for product ID

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
                            uniqueImageName = UUID.randomUUID().toString(); // Generate unique image name
                            uploadImage.setImageURI(uri);
                        }
                    }
                }
        );

        // Image upload button click listener
        uploadImage.setOnClickListener(view -> {
            Intent photoPicker = new Intent(Intent.ACTION_PICK);
            photoPicker.setType("image/*");
            activityResultLauncher.launch(photoPicker);
        });

        // Save button click listener
        saveButton.setOnClickListener(view -> saveData());
    }

    // Function to save data
    private void saveData() {
        String prodName = uploadProd.getText().toString().trim();
        String prodDesc = uploadDesc.getText().toString().trim();
        String prodGroup = uploadGroup.getText().toString().trim();
        String prodQtyStr = uploadQty.getText().toString().trim();
        String prodCostStr = uploadCost.getText().toString().trim();

        // Set prodDesc to "N/A" if it's empty
        if (prodDesc.isEmpty()) {
            prodDesc = "N/A";
        }

        // Validate input
        if (prodName.isEmpty() || prodGroup.isEmpty() || prodQtyStr.isEmpty() || prodCostStr.isEmpty() || uri == null) {
            Toast.makeText(UploadActivity.this, "Please fill all fields and select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert quantity and cost to their respective types
        int prodQty = Integer.parseInt(prodQtyStr);
        double prodCost = Double.parseDouble(prodCostStr);
        double prodTotalPrice = prodQty * prodCost; // Calculate total price

        // Generate a unique productId
        productId = UUID.randomUUID().toString();

        // Check for network connectivity
        if (isNetworkAvailable()) {
            // If network is available, upload image and data to Firestore
            uploadImageToFirebase(uri, productId, prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice);
        } else {
            // Show a toast indicating that an internet connection is required
            Toast.makeText(UploadActivity.this, "An internet connection is required to add a product", Toast.LENGTH_SHORT).show();
        }
    }

    // Upload image to Firebase Storage
    private void uploadImageToFirebase(Uri uri, String productId, String prodName, String prodDesc, String prodGroup, int prodQty, double prodCost, double prodTotalPrice) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("ProductImages")
                .child(uniqueImageName);

        AlertDialog.Builder builder = new AlertDialog.Builder(UploadActivity.this);
        builder.setCancelable(false);
        builder.setView(R.layout.progress_layout);
        AlertDialog dialog = builder.create();
        dialog.show();

        storageReference.putFile(uri).addOnSuccessListener(taskSnapshot -> {
            taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri1 -> {
                imageURL = uri1.toString();
                addDataToFirestore(productId, prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice, imageURL);
                dialog.dismiss();

                // Return result to InventoryActivity indicating success
                Intent resultIntent = new Intent();
                setResult(RESULT_OK, resultIntent);

                Toast.makeText(UploadActivity.this, "Saved Successfully", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).addOnFailureListener(e -> {
            dialog.dismiss();
            Toast.makeText(UploadActivity.this, "Failed to save data", Toast.LENGTH_SHORT).show();
        });
    }

    // Function to add data to Firestore
    private void addDataToFirestore(String productId, String prodName, String prodDesc, String prodGroup, int prodQty, double prodCost, double prodTotalPrice, String imageURL) {
        Date dateAdded = new Date(); // Set the current date and time

        DataClass data = new DataClass(productId, prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice, imageURL, dateAdded);

        db.collection("products").add(data).addOnSuccessListener(documentReference -> {
            Log.d("Firestore", "DocumentSnapshot added with ID: " + documentReference.getId());
        }).addOnFailureListener(e -> {
            Log.w("Firestore", "Error adding document", e);
        });
    }

    // Function to save image locally with unique name *FOR FUTURE USE*
    private void saveImageLocally(Uri uri, String uniqueImageName) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File file = new File(getCacheDir(), uniqueImageName + ".jpg");
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

    // Schedule image upload task when network is available *FOR FUTURE USE*
    private void scheduleImageUploadTask(String productId, String prodName, String prodDesc, String prodGroup, int prodQty, double prodCost, double prodTotalPrice, String uniqueImageName) {
        Data inputData = new Data.Builder()
                .putString("productId", productId)  // Add productId to input data
                .putString("prodName", prodName)
                .putString("prodDesc", prodDesc)
                .putString("prodGroup", prodGroup)
                .putInt("prodQty", prodQty)
                .putDouble("prodCost", prodCost)
                .putDouble("prodTotalPrice", prodTotalPrice)
                .putString("image_name", uniqueImageName)
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
