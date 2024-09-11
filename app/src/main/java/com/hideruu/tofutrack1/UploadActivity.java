package com.hideruu.tofutrack1;

import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
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

        // Initialize Firestore and UI elements
        db = FirebaseFirestore.getInstance();
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

        // Upload image to Firebase Storage
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
                        addData(prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice, imageURL);
                        dialog.dismiss();
                        Toast.makeText(UploadActivity.this, "Saved Successfully", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        setResult(Activity.RESULT_OK, resultIntent);
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
    private void addData(String prodName, String prodDesc, String prodGroup, int prodQty, double prodCost, double prodTotalPrice, String imageURL) {
        Date dateAdded = new Date(); // Set the current date and time

        DataClass data = new DataClass(prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice, imageURL, dateAdded);

        db.collection("products").add(data).addOnSuccessListener(documentReference -> {
            Log.d("Firestore", "DocumentSnapshot added with ID: " + documentReference.getId());
        }).addOnFailureListener(e -> {
            Log.w("Firestore", "Error adding document", e);
        });
    }
}
