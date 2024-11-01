package com.hideruu.tofutrack1;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UpdateActivity extends AppCompatActivity {

    private static final String TAG = "UpdateActivity";

    private EditText uploadProd, uploadDesc, uploadQty, uploadCost, uploadGroup, unitType;
    private Button saveButton;

    private FirebaseFirestore db;
    private String productName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Get data from intent
        getDataFromIntent();

        // Save button click listener
        saveButton.setOnClickListener(view -> updateData());
    }

    private void initializeViews() {
        uploadProd = findViewById(R.id.uploadProd);
        uploadDesc = findViewById(R.id.uploadDesc);
        uploadQty = findViewById(R.id.uploadQty);
        uploadCost = findViewById(R.id.uploadCost);
        uploadGroup = findViewById(R.id.uploadGroup); // Non-editable field
        unitType = findViewById(R.id.unitType);       // Non-editable field
        saveButton = findViewById(R.id.saveButton);
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            productName = intent.getStringExtra("prodName");
            String desc = intent.getStringExtra("prodDesc");
            String group = intent.getStringExtra("prodGroup");
            int qty = intent.getIntExtra("prodQty", 0);
            double cost = intent.getDoubleExtra("prodCost", 0.0);
            String prodUnitType = intent.getStringExtra("prodUnitType");

            // Populate fields
            uploadProd.setText(productName);

            // Set the description directly without modifications
            if (desc != null) {
                uploadDesc.setText(desc.replace("Description: ", "").trim());
            } else {
                uploadDesc.setText("");
            }

            uploadQty.setText(String.valueOf(qty));
            uploadCost.setText(String.format(Locale.getDefault(), "%.2f", cost));

            // Set non-editable group field
            if (group != null) {
                uploadGroup.setText(group.replace("Group:", "").trim());
            }

            // Retrieve prodUnitType if not present in the intent
            if (prodUnitType != null) {
                unitType.setText(prodUnitType);
            } else {
                fetchProdUnitTypeFromFirestore(productName);
            }
        }
    }

    private void fetchProdUnitTypeFromFirestore(String productName) {
        db.collection("products")
                .whereEqualTo("prodName", productName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        String retrievedProdUnitType = documentSnapshot.getString("prodUnitType");
                        if (retrievedProdUnitType != null) {
                            unitType.setText(retrievedProdUnitType);
                        }
                    } else {
                        Log.d(TAG, "Product with name '" + productName + "' not found in Firestore.");
                        Toast.makeText(UpdateActivity.this, "Product unit type not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching product unit type: ", e);
                    Toast.makeText(UpdateActivity.this, "Error fetching unit type: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void updateData() {
        if (!isNetworkAvailable()) {
            Toast.makeText(UpdateActivity.this, "No internet connection. Please try again later.", Toast.LENGTH_SHORT).show();
            return;
        }

        String prodName = uploadProd.getText().toString().trim();
        String prodDesc = uploadDesc.getText().toString().trim();
        String prodGroup = uploadGroup.getText().toString(); // Non-editable, retrieved from intent
        String prodQtyStr = uploadQty.getText().toString().trim();
        String prodCostStr = uploadCost.getText().toString().trim();
        String prodUnitType = unitType.getText().toString(); // Non-editable, retrieved from intent

        if (prodName.isEmpty() || prodGroup.isEmpty() || prodQtyStr.isEmpty() || prodCostStr.isEmpty() ||
                (prodDesc.isEmpty() && !prodDesc.equals("N/A"))) {
            Toast.makeText(UpdateActivity.this, "Please fill all fields (Description can be 'N/A')", Toast.LENGTH_SHORT).show();
            return;
        }

        int prodQty = Integer.parseInt(prodQtyStr);
        double prodCost = Double.parseDouble(prodCostStr);
        double prodTotalPrice = prodQty * prodCost;

        Log.d(TAG, "Updating product with name: '" + prodName + "'");

        db.collection("products").whereEqualTo("prodName", prodName).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        Log.d(TAG, "Document exists. Proceeding with update.");

                        String documentId = documentSnapshot.getId();
                        updateProduct(documentId, prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice, prodUnitType);
                    } else {
                        Toast.makeText(UpdateActivity.this, "Product not found", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Product with name '" + prodName + "' does not exist.");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UpdateActivity.this, "Error checking product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error checking product: ", e);
                });
    }

    private void updateProduct(String documentId, String prodName, String prodDesc, String prodGroup, int prodQty, double prodCost, double prodTotalPrice, String prodUnitType) {
        // Create a map for the update data
        Map<String, Object> updates = new HashMap<>();
        updates.put("prodName", prodName);
        updates.put("prodDesc", prodDesc);
        updates.put("prodGroup", prodGroup);
        updates.put("prodQty", prodQty);
        updates.put("prodCost", prodCost);
        updates.put("prodTotalPrice", prodTotalPrice);
        updates.put("prodUnitType", prodUnitType);

        db.collection("products").document(documentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UpdateActivity.this, "Product updated successfully", Toast.LENGTH_SHORT).show();

                    // Now save the update record to a new collection
                    recordUpdate(prodName, prodQty, prodCost, prodUnitType, prodGroup); // Pass the new parameters

                    // Set the result to RESULT_OK and finish the activity
                    setResult(RESULT_OK); // This will notify InventoryActivity
                    finish(); // Close activity
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UpdateActivity.this, "Error updating product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating product: ", e);
                });
    }

    private void recordUpdate(String prodName, int prodQty, double prodCost, String prodUnitType, String prodGroup) {
        // Create a new record with the current date and time
        Map<String, Object> updateRecord = new HashMap<>();
        updateRecord.put("prodName", prodName);
        updateRecord.put("prodQty", prodQty);
        updateRecord.put("prodCost", prodCost);
        updateRecord.put("prodUnitType", prodUnitType); // Add product unit type
        updateRecord.put("prodGroup", prodGroup); // Add product group

        // Format the current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String formattedDate = sdf.format(new Date()); // Format current date

        updateRecord.put("timestamp", formattedDate); // Add the formatted date to the record

        // Add a new document to the product_updates collection
        db.collection("product_updates").add(updateRecord)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Update record added successfully."))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding update record: ", e));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
