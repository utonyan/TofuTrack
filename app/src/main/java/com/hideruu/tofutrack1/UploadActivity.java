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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
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
import java.util.Random;
import java.util.UUID;

public class UploadActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ImageView uploadImage;
    private Button saveButton;
    private EditText uploadProd, uploadDesc, uploadQty, uploadCost;
    private Spinner unitTypeSpinner;  // Spinner for selecting unit type
    private Spinner groupSpinner;      // Spinner for selecting product group
    private String imageURL;
    private Uri uri;
    private String uniqueImageName;
    private int productId;  // Changed productId to int

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
        uploadQty = findViewById(R.id.uploadQty);
        uploadCost = findViewById(R.id.uploadCost);
        saveButton = findViewById(R.id.saveButton);

        // Initialize Spinners
        unitTypeSpinner = findViewById(R.id.unitTypeSpinner);  // Spinner for selecting unit type
        groupSpinner = findViewById(R.id.uploadGroupSpinner);    // Spinner for selecting product group

        // Set up adapters for Spinners
        ArrayAdapter<CharSequence> unitTypeAdapter = ArrayAdapter.createFromResource(this, R.array.unit_types, android.R.layout.simple_spinner_item);
        unitTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitTypeSpinner.setAdapter(unitTypeAdapter);

        ArrayAdapter<CharSequence> groupAdapter = ArrayAdapter.createFromResource(this, R.array.product_groups, android.R.layout.simple_spinner_item);
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(groupAdapter);

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
        String prodGroup = groupSpinner.getSelectedItem().toString(); // Get selected product group from Spinner
        String prodQtyStr = uploadQty.getText().toString().trim();
        String prodCostStr = uploadCost.getText().toString().trim();
        String prodUnitType = unitTypeSpinner.getSelectedItem().toString();  // Get selected unit type

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

        // Generate a random integer productId
        productId = generateRandomProductId();

        // Check for network connectivity
        if (isNetworkAvailable()) {
            // Check for duplicate product name
            String finalProdDesc = prodDesc;
            checkDuplicateProduct(prodName, prodId -> {
                if (prodId == null) {
                    // If no duplicate found, upload image and data to Firestore
                    uploadImageToFirebase(uri, productId, prodName, finalProdDesc, prodGroup, prodQty, prodCost, prodTotalPrice, prodUnitType);
                } else {
                    // Show a toast indicating that the product already exists
                    Toast.makeText(UploadActivity.this, "Product with this name already exists.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Show a toast indicating that an internet connection is required
            Toast.makeText(UploadActivity.this, "An internet connection is required to add a product", Toast.LENGTH_SHORT).show();
        }
    }

    // Function to generate a random product ID
    private int generateRandomProductId() {
        Random random = new Random();
        return random.nextInt(100000); // Generates a random ID between 0 and 99999
    }

    // Check for duplicate product name in Firestore
    private void checkDuplicateProduct(String prodName, OnDuplicateCheckListener listener) {
        db.collection("products")
                .whereEqualTo("prodName", prodName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No duplicate found
                        listener.onDuplicateCheck(null);
                    } else {
                        // Duplicate found
                        listener.onDuplicateCheck("duplicate");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UploadActivity", "Error checking for duplicates", e);
                    listener.onDuplicateCheck(null);
                });
    }

    // Upload image to Firebase Storage
    private void uploadImageToFirebase(Uri uri, int productId, String prodName, String prodDesc, String prodGroup, int prodQty, double prodCost, double prodTotalPrice, String prodUnitType) {
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
                addDataToFirestore(productId, prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice, imageURL, prodUnitType);
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
    private void addDataToFirestore(int productId, String prodName, String prodDesc, String prodGroup, int prodQty, double prodCost, double prodTotalPrice, String imageURL, String prodUnitType) {
        Date dateAdded = new Date(); // Set the current date and time

        DataClass data = new DataClass(productId, prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice, imageURL, dateAdded, prodUnitType);

        db.collection("products").add(data).addOnSuccessListener(documentReference -> {
            Log.d("Firestore", "DocumentSnapshot added with ID: " + documentReference.getId());
        }).addOnFailureListener(e -> {
            Log.w("Firestore", "Error adding document", e);
        });
    }

    // Check if network is available
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    // Schedule image upload task when network is available *FOR FUTURE USE*
    private void scheduleImageUploadTask(int productId, String prodName, String prodDesc, String prodGroup, int prodQty, double prodCost, double prodTotalPrice, String uniqueImageName) {
        Data inputData = new Data.Builder()
                .putInt("productId", productId)  // Change to putInt for productId
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

    // Interface for duplicate check callback
    private interface OnDuplicateCheckListener {
        void onDuplicateCheck(String prodId);
    }
}
