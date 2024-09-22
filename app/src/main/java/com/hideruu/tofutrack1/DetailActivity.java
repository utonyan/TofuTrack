package com.hideruu.tofutrack1;

import androidx.annotation.NonNull;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DetailActivity extends AppCompatActivity {

    private TextView prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice;
    private ImageView prodImage;
    private FirebaseFirestore db;
    private String productId;

    // Queue to store deletion requests
    private List<String> deletionQueue = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        prodName = findViewById(R.id.prodName);
        prodDesc = findViewById(R.id.prodDesc);
        prodGroup = findViewById(R.id.prodGroup);
        prodQty = findViewById(R.id.prodQty);
        prodCost = findViewById(R.id.prodCost);
        prodTotalPrice = findViewById(R.id.prodTotalPrice);
        prodImage = findViewById(R.id.prodImage);

        // Get the data passed from the adapter
        Intent intent = getIntent();
        if (intent != null) {
            productId = intent.getStringExtra("productId");
            loadProductDetails();
        }

        // Handle delete button click
        findViewById(R.id.deleteButton).setOnClickListener(v -> confirmDelete());
    }

    private void loadProductDetails() {
        // Load product details (similar to your existing code)
        // ...
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete this product?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteProduct())
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void deleteProduct() {
        String productName = prodName.getText().toString();
        Log.d("DetailActivity", "Attempting to delete product with name: " + productName);

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Deletion will be queued when an internet connection returns", Toast.LENGTH_SHORT).show();
            deletionQueue.add(productName); // Queue the deletion
            return;
        }

        // Query and delete the product
        db.collection("products")
                .whereEqualTo("prodName", productName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            Log.d("DetailActivity", "Found document with ID: " + document.getId());
                            document.getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("DetailActivity", "Product deleted successfully.");
                                        Toast.makeText(DetailActivity.this, "Product deleted successfully", Toast.LENGTH_SHORT).show();
                                        setResult(RESULT_OK);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("DetailActivity", "Error deleting product: " + e.getMessage());
                                        Toast.makeText(DetailActivity.this, "Error deleting product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Log.e("DetailActivity", "No document found with that product name");
                        Toast.makeText(DetailActivity.this, "No product found with that name", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DetailActivity", "Error querying document: " + e.getMessage());
                    Toast.makeText(DetailActivity.this, "Error querying document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void processDeletionQueue() {
        for (String productName : deletionQueue) {
            scheduleDeletionTask(productName);
        }
        deletionQueue.clear(); // Clear the queue after processing
    }

    private void scheduleDeletionTask(String productName) {
        Data inputData = new Data.Builder()
                .putString("productName", productName)
                .build();

        OneTimeWorkRequest deleteWorkRequest = new OneTimeWorkRequest.Builder(DeleteWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(deleteWorkRequest);
    }

    public static class DeleteWorker extends Worker {
        public DeleteWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            String productName = getInputData().getString("productName");
            if (productName != null) {
                // Perform deletion from Firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("products")
                        .whereEqualTo("prodName", productName)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                    document.getReference().delete();
                                }
                            }
                        })
                        .addOnFailureListener(e -> Log.e("DeleteWorker", "Error deleting product: " + e.getMessage()));
            }
            return Result.success();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNetworkAvailable()) {
            processDeletionQueue(); // Process the queue if connected
        }
    }
}
