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
        db = FirebaseFirestore.getInstance(); // Initialize Firestore
    }

    @NonNull
    @Override
    public Result doWork() {
        // Get input data
        String prodName = getInputData().getString("prodName");
        String prodDesc = getInputData().getString("prodDesc");
        String prodGroup = getInputData().getString("prodGroup");
        int prodQty = getInputData().getInt("prodQty", 0);
        double prodCost = getInputData().getDouble("prodCost", 0);
        double prodTotalPrice = getInputData().getDouble("prodTotalPrice", 0);
        String imagePath = getInputData().getString("image_path");

        // Ensure the image path exists
        if (imagePath == null || prodName == null || prodDesc == null || prodGroup == null) {
            return Result.failure();
        }

        // Create a Uri from the image path
        File file = new File(imagePath);
        Uri uri = Uri.fromFile(file);

        // Get a reference to Firebase Storage
        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("ProductImages")
                .child(file.getName());

        try {
            // Upload the image to Firebase Storage
            storageReference.putFile(uri).addOnSuccessListener(taskSnapshot -> {
                // Get the download URL once the image is uploaded
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    String imageURL = downloadUri.toString();
                    // Once the image is uploaded, save the product data to Firestore
                    addDataToFirestore(prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice, imageURL);
                }).addOnFailureListener(e -> {
                    Log.e("ImageUploadWorker", "Failed to get download URL: ", e);
                });
            }).addOnFailureListener(e -> {
                Log.e("ImageUploadWorker", "Image upload failed: ", e);
            });

            return Result.success();
        } catch (Exception e) {
            Log.e("ImageUploadWorker", "Error uploading image: ", e);
            return Result.failure();
        }
    }

    // Function to add data to Firestore
    private void addDataToFirestore(String prodName, String prodDesc, String prodGroup, int prodQty, double prodCost, double prodTotalPrice, String imageURL) {
        Date dateAdded = new Date(); // Set the current date and time

        // Create a new DataClass object
        DataClass data = new DataClass(prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice, imageURL, dateAdded);

        // Save the product information to Firestore
        db.collection("products").add(data).addOnSuccessListener(documentReference -> {
            Log.d("Firestore", "DocumentSnapshot added with ID: " + documentReference.getId());
        }).addOnFailureListener(e -> {
            Log.w("Firestore", "Error adding document", e);
        });
    }
}
