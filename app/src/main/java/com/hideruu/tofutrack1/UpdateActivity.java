package com.hideruu.tofutrack1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import android.widget.ArrayAdapter;

public class UpdateActivity extends AppCompatActivity {

    private static final String TAG = "UpdateActivity";

    private EditText uploadProd, uploadDesc, uploadQty, uploadCost;
    private Spinner unitTypeSpinner, groupSpinner;
    private Button saveButton;

    private FirebaseFirestore db;
    private String productName; // Store product name as String

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update); // Ensure this matches your XML file name

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Set up adapters for Spinners
        setupSpinners();

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
        unitTypeSpinner = findViewById(R.id.unitTypeSpinner);
        groupSpinner = findViewById(R.id.uploadGroupSpinner);
        saveButton = findViewById(R.id.saveButton);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> unitTypeAdapter = ArrayAdapter.createFromResource(this,
                R.array.unit_types, android.R.layout.simple_spinner_item);
        unitTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitTypeSpinner.setAdapter(unitTypeAdapter);

        ArrayAdapter<CharSequence> groupAdapter = ArrayAdapter.createFromResource(this,
                R.array.product_groups, android.R.layout.simple_spinner_item);
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(groupAdapter);
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            productName = intent.getStringExtra("prodName"); // Get productName from intent
            String desc = intent.getStringExtra("prodDesc");
            String group = intent.getStringExtra("prodGroup");
            int qty = intent.getIntExtra("prodQty", 0);
            double cost = intent.getDoubleExtra("prodCost", 0.0);

            // Populate fields
            uploadProd.setText(productName);
            uploadDesc.setText(desc != null ? desc : "");
            // Set the description directly
            uploadQty.setText(String.valueOf(qty));
            uploadCost.setText(String.format(Locale.getDefault(), "%.2f", cost));

            // Set spinner selections
            setSpinnerSelection(groupSpinner, group);
            setSpinnerSelection(unitTypeSpinner, intent.getStringExtra("prodUnitType"));
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value != null) {
            int position = ((ArrayAdapter<String>) spinner.getAdapter()).getPosition(value);
            if (position >= 0) {
                spinner.setSelection(position);
            }
        }
    }

    private void updateData() {
        String prodName = uploadProd.getText().toString().trim();
        String prodDesc = uploadDesc.getText().toString().trim();
        String prodGroup = groupSpinner.getSelectedItem().toString();
        String prodQtyStr = uploadQty.getText().toString().trim();
        String prodCostStr = uploadCost.getText().toString().trim();
        String prodUnitType = unitTypeSpinner.getSelectedItem().toString();

        if (prodName.isEmpty() || prodGroup.isEmpty() || prodQtyStr.isEmpty() || prodCostStr.isEmpty()) {
            Toast.makeText(UpdateActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int prodQty = Integer.parseInt(prodQtyStr);
        double prodCost = Double.parseDouble(prodCostStr);
        double prodTotalPrice = prodQty * prodCost;

        // Log for debugging
        Log.d(TAG, "Updating product with name: '" + prodName + "'");

        // Check if the document exists by name
        db.collection("products").whereEqualTo("prodName", prodName).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0); // Get the first matching document
                        Log.d(TAG, "Document exists. Proceeding with update.");

                        // Prepare the update data
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

                    // Set the result to RESULT_OK and finish the activity
                    setResult(RESULT_OK); // This will notify InventoryActivity
                    finish(); // Close activity
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UpdateActivity.this, "Error updating product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating product: ", e);
                });
    }

}
