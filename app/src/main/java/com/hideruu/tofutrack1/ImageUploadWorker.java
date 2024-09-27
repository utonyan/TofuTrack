package com.hideruu.tofutrack1;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.Date;

public class ImageUploadWorker extends Worker {
    private FirebaseFirestore db;

    public ImageUploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("ImageUploadWorker", "Worker started");

        // Change productId to int and update input data retrieval
        int productId = getInputData().getInt("productId", -1); // Changed to int with default -1
        String prodName = getInputData().getString("prodName");
        String prodDesc = getInputData().getString("prodDesc");
        String prodGroup = getInputData().getString("prodGroup");
        int prodQty = getInputData().getInt("prodQty", 0);
        double prodCost = getInputData().getDouble("prodCost", 0);
        double prodTotalPrice = getInputData().getDouble("prodTotalPrice", 0);
        String imageName = getInputData().getString("image_name");
        String prodUnitType = getInputData().getString("prodUnitType");

        // Validate required fields
        if (imageName == null || prodName == null || prodDesc == null || prodGroup == null || productId == -1 || prodUnitType == null) {
            return Result.failure(); // Return failure if any required field is missing
        }

        // Create a file reference for the uploaded image
        File file = new File(getApplicationContext().getCacheDir(), imageName + ".jpg");
        Uri uri = Uri.fromFile(file);

        // Create a storage reference in Firebase Storage
        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("ProductImages")
                .child(imageName);

        try {
            // Upload image to Firebase Storage
            storageReference.putFile(uri).addOnSuccessListener(taskSnapshot -> {
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    String imageURL = downloadUri.toString();
                    // Add data to Firestore
                    addDataToFirestore(productId, prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice, imageURL, prodUnitType);
                }).addOnFailureListener(e -> {
                    Log.e("ImageUploadWorker", "Failed to get download URL: ", e);
                });
            }).addOnFailureListener(e -> {
                Log.e("ImageUploadWorker", "Image upload failed: ", e);
            });

            return Result.success(); // Return success if upload is initiated
        } catch (Exception e) {
            Log.e("ImageUploadWorker", "Error uploading image: ", e);
            return Result.failure(); // Return failure on error
        }
    }

    // Modify this method to take int productId
    private void addDataToFirestore(int productId, String prodName, String prodDesc, String prodGroup, int prodQty, double prodCost, double prodTotalPrice, String imageURL, String prodUnitType) {
        Date dateAdded = new Date();
        DataClass data = new DataClass(productId, prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice, imageURL, dateAdded, prodUnitType);

        // Save to Firestore with productId as the document ID
        db.collection("products").document(String.valueOf(productId)) // Convert int to String for document ID
                .set(data)
                .addOnSuccessListener(documentReference -> Log.d("Firestore", "DocumentSnapshot added with ID: " + productId))
                .addOnFailureListener(e -> Log.w("Firestore", "Error adding document", e));
    }
}
