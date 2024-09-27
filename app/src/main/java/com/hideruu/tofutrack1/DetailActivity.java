package com.hideruu.tofutrack1;

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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    private TextView prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice, prodUnitType;
    private ImageView prodImage;
    private FirebaseFirestore db;
    private String productName; // Change productId to String

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Initialize views
        prodName = findViewById(R.id.prodName);
        prodDesc = findViewById(R.id.prodDesc);
        prodGroup = findViewById(R.id.prodGroup);
        prodQty = findViewById(R.id.prodQty);
        prodCost = findViewById(R.id.prodCost);
        prodTotalPrice = findViewById(R.id.prodTotalPrice);
        prodImage = findViewById(R.id.prodImage);

        db = FirebaseFirestore.getInstance();  // Initialize Firestore

        // Get the data passed from the adapter
        Intent intent = getIntent();
        if (intent != null) {
            productName = intent.getStringExtra("prodName"); // Get productName as String
            String desc = intent.getStringExtra("prodDesc");
            String group = intent.getStringExtra("prodGroup");
            int qty = intent.getIntExtra("prodQty", 0);
            double cost = intent.getDoubleExtra("prodCost", 0.0);
            double totalPrice = intent.getDoubleExtra("prodTotalPrice", 0.0);
            String imageUrl = intent.getStringExtra("prodImage");

            // Set data to views, handling potential null values
            prodName.setText(productName != null ? productName : "No Name");
            prodDesc.setText("Description: " + (desc != null ? desc : "No Description"));
            prodGroup.setText("Group: " + (group != null ? group : "No Group"));

            // Update prodQty with prodUnitType
            prodQty.setText("Quantity: " + qty);

            prodCost.setText(String.format(Locale.getDefault(), "Cost per unit: ₱%.2f", cost));
            prodTotalPrice.setText(String.format(Locale.getDefault(), "Total Price: ₱%.2f", totalPrice));

            // Load product image using Glide, handling potential missing image URL
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this).load(imageUrl).into(prodImage);
            } else {
                prodImage.setImageResource(R.drawable.kitalogo);  // Placeholder image if no URL
            }
        }

        findViewById(R.id.editButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DetailActivity.this, UpdateActivity.class);
                intent.putExtra("prodName", prodName.getText().toString());
                intent.putExtra("prodDesc", prodDesc.getText().toString());
                intent.putExtra("prodGroup", prodGroup.getText().toString());
                intent.putExtra("prodQty", Integer.parseInt(prodQty.getText().toString().split(" ")[1])); // Assuming format "Quantity: 5"
                intent.putExtra("prodCost", Double.parseDouble(prodCost.getText().toString().replace("Cost per unit: ₱", "").replace(",", "").trim()));
                startActivity(intent);
                finish();
            }
        });

        // Handle delete button click
        findViewById(R.id.deleteButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete this product: " + productName + "?") // Include product name in the message
                .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteProduct())
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void deleteProduct() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Cannot delete product.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("DetailActivity", "Attempting to delete product with name: " + productName);

        // Delete the product using the product name
        db.collection("products").whereEqualTo("prodName", productName).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0); // Get the first matching document
                        String documentId = documentSnapshot.getId(); // Get the document ID
                        db.collection("products").document(documentId)
                                .delete()
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
                    } else {
                        Toast.makeText(DetailActivity.this, "Product not found", Toast.LENGTH_SHORT).show();
                        Log.d("DetailActivity", "Product with name '" + productName + "' does not exist.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DetailActivity", "Error checking product: " + e.getMessage());
                    Toast.makeText(DetailActivity.this, "Error checking product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
