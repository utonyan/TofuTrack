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
import java.util.concurrent.CountDownLatch;

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

        String prodName = getInputData().getString("prodName");
        String prodDesc = getInputData().getString("prodDesc");
        String prodGroup = getInputData().getString("prodGroup");
        int prodQty = getInputData().getInt("prodQty", 0);
        double prodCost = getInputData().getDouble("prodCost", 0);
        double prodTotalPrice = getInputData().getDouble("prodTotalPrice", 0);
        String imageName = getInputData().getString("image_name");

        if (imageName == null || prodName == null || prodDesc == null || prodGroup == null) {
            return Result.failure();
        }

        File file = new File(getApplicationContext().getCacheDir(), imageName + ".jpg");
        Uri uri = Uri.fromFile(file);

        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("ProductImages")
                .child(imageName);

        try {
            storageReference.putFile(uri).addOnSuccessListener(taskSnapshot -> {
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    String imageURL = downloadUri.toString();
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

    private void addDataToFirestore(String prodName, String prodDesc, String prodGroup, int prodQty, double prodCost, double prodTotalPrice, String imageURL) {
        Date dateAdded = new Date();
        DataClass data = new DataClass(prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice, imageURL, dateAdded);
        db.collection("products").add(data)
                .addOnSuccessListener(documentReference -> Log.d("Firestore", "DocumentSnapshot added with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.w("Firestore", "Error adding document", e));
    }
}
