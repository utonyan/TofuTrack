package com.hideruu.tofutrack1;

import android.content.DialogInterface;
import android.content.Intent;
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

    private TextView prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice;
    private ImageView prodImage;
    private FirebaseFirestore db;
    private String productId;

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
            productId = intent.getStringExtra("productId");  // Get the product ID
            Log.d("DetailActivity", "Received product ID: " + productId);


            String name = intent.getStringExtra("prodName");
            String desc = intent.getStringExtra("prodDesc");
            String group = intent.getStringExtra("prodGroup");
            int qty = intent.getIntExtra("prodQty", 0);
            double cost = intent.getDoubleExtra("prodCost", 0.0);
            double totalPrice = intent.getDoubleExtra("prodTotalPrice", 0.0);
            String imageUrl = intent.getStringExtra("prodImage");

            // Set data to views, handling potential null values
            prodName.setText(name != null ? name : "No Name");
            prodDesc.setText("Description: " + (desc != null ? desc : "No Description"));
            prodGroup.setText("Group: " + (group != null ? group : "No Group"));
            prodQty.setText(String.format(Locale.getDefault(), "Quantity: %d", qty));
            prodCost.setText(String.format(Locale.getDefault(), "Cost per unit: ₱%.2f", cost));
            prodTotalPrice.setText(String.format(Locale.getDefault(), "Total Price: ₱%.2f", totalPrice));

            // Load product image using Glide, handling potential missing image URL
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this).load(imageUrl).into(prodImage);
            } else {
                prodImage.setImageResource(R.drawable.kitalogo);  // Placeholder image if no URL
            }
        }

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
                .setMessage("Are you sure you want to delete this product?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteProduct();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void deleteProduct() {
        String productName = prodName.getText().toString();
        Log.d("DetailActivity", "Attempting to delete product with name: " + productName);

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
                                        // Notify InventoryActivity about the deletion
                                        setResult(RESULT_OK);
                                        finish();  // Close DetailActivity
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

}