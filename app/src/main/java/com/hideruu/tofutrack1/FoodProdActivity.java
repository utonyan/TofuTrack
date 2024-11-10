package com.hideruu.tofutrack1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class FoodProdActivity extends AppCompatActivity {

    private Spinner productSpinner;
    private RecyclerView rawMaterialRecycler, packagingRecycler;
    private Button confirmButton;
    private EditText productionQuantityEditText;
    private TextView currentStockTextView; // Declare current stock TextView

    private FirebaseFirestore db;
    private List<DataClass> productList = new ArrayList<>();
    private List<DataClass> rawMaterialList = new ArrayList<>();
    private List<DataClass> packagingList = new ArrayList<>();

    private ProductAdapter productAdapter;
    private MaterialAdapter rawMaterialAdapter, packagingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_prod);

        initializeViews();
        setupRecyclerViews();
        loadProducts();
        loadRawMaterials();
        loadPackaging();

        confirmButton.setOnClickListener(v -> confirmSelection());
    }

    private void initializeViews() {
        db = FirebaseFirestore.getInstance();
        productSpinner = findViewById(R.id.productSpinner);
        rawMaterialRecycler = findViewById(R.id.rawMaterialRecycler);
        packagingRecycler = findViewById(R.id.packagingRecycler);
        confirmButton = findViewById(R.id.confirmButton);
        productionQuantityEditText = findViewById(R.id.productionQuantityEditText);
        currentStockTextView = findViewById(R.id.currentStockTextView); // Initialize current stock TextView

        productSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DataClass selectedProduct = (DataClass) productSpinner.getSelectedItem();
                if (selectedProduct != null) {
                    // Update the current stock TextView with the quantity of the selected product
                    currentStockTextView.setText("Current Stock: " + selectedProduct.getProdQty());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentStockTextView.setText("Current Stock: 0");
            }
        });
    }

    private void setupRecyclerViews() {
        rawMaterialRecycler.setLayoutManager(new LinearLayoutManager(this));
        packagingRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadProducts() {
        db.collection("products")
                .whereEqualTo("prodGroup", "Product")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        productList.clear(); // Clear previous data
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            DataClass product = document.toObject(DataClass.class);
                            productList.add(product);
                        }
                        // Sort by quantity (ascending), then by product name alphabetically
                        Collections.sort(productList, Comparator.comparingInt(DataClass::getProdQty)
                                .thenComparing(DataClass::getProdName));

                        productAdapter = new ProductAdapter(this, productList);
                        productSpinner.setAdapter(productAdapter);
                    }
                });
    }

    private void loadRawMaterials() {
        loadMaterials("Raw Material", rawMaterialList, rawMaterialRecycler);
    }

    private void loadPackaging() {
        loadMaterials("Packaging", packagingList, packagingRecycler);
    }

    private void loadMaterials(String group, List<DataClass> list, RecyclerView recyclerView) {
        db.collection("products")
                .whereEqualTo("prodGroup", group)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        list.clear(); // Clear previous data
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            DataClass material = document.toObject(DataClass.class);
                            list.add(material);
                        }
                        Log.d("FoodProdActivity", "Loaded " + list.size() + " items for group: " + group);

                        // Sort alphabetically by product name
                        Collections.sort(list, Comparator.comparing(DataClass::getProdName));

                        MaterialAdapter adapter = new MaterialAdapter(this, list, recyclerView);
                        recyclerView.setAdapter(adapter);
                        if (group.equals("Raw Material")) {
                            rawMaterialAdapter = adapter;
                        } else {
                            packagingAdapter = adapter;
                        }
                    } else {
                        Log.e("FoodProdActivity", "Error loading materials: ", task.getException());
                    }
                });
    }


    private void confirmSelection() {
        DataClass selectedProduct = (DataClass) productSpinner.getSelectedItem();
        String productionQtyStr = productionQuantityEditText.getText().toString();

        if (selectedProduct == null) {
            Toast.makeText(this, "Please select a product", Toast.LENGTH_SHORT).show();
            return;
        }

        if (productionQtyStr.isEmpty()) {
            Toast.makeText(this, "Please enter production quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        int productionQuantity = Integer.parseInt(productionQtyStr);

        if (!rawMaterialAdapter.hasSelectedItems() || !packagingAdapter.hasSelectedItems()) {
            Toast.makeText(this, "Please select at least one raw material and one packaging item", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!rawMaterialAdapter.hasSufficientQuantities() || !packagingAdapter.hasSufficientQuantities()) {
            Toast.makeText(this, "Insufficient materials for the entered quantities", Toast.LENGTH_SHORT).show();
            return;
        }

        if (productionQuantity <= 0) {
            Toast.makeText(this, "Production quantity must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        // Retrieve selected raw materials and packaging for display in the dialog
        Map<String, Integer> selectedRawMaterials = rawMaterialAdapter.getSelectedItems();
        Map<String, Integer> selectedPackaging = packagingAdapter.getSelectedItems();

        // Show the confirmation dialog
        showConfirmationDialog(selectedProduct, productionQuantity, selectedRawMaterials, selectedPackaging);
    }

    private void showConfirmationDialog(DataClass selectedProduct, int productionQuantity, Map<String, Integer> rawMaterials, Map<String, Integer> packaging) {
        StringBuilder message = new StringBuilder();
        message.append("Product: ").append(selectedProduct.getProdName()).append("\n")
                .append("Quantity to Produce: ").append(productionQuantity).append("\n\n")
                .append("Selected Raw Materials:\n");

        for (Map.Entry<String, Integer> entry : rawMaterials.entrySet()) {
            message.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        message.append("\nSelected Packaging:\n");
        for (Map.Entry<String, Integer> entry : packaging.entrySet()) {
            message.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Production")
                .setMessage(message.toString())
                .setPositiveButton("Confirm", (dialog, which) -> {
                    // Convert selected items to arrays
                    String[] rawMaterialsArray = new String[rawMaterials.size()];
                    int[] rawMaterialsQuantitiesArray = new int[rawMaterials.size()];
                    int index = 0;
                    for (Map.Entry<String, Integer> entry : rawMaterials.entrySet()) {
                        rawMaterialsArray[index] = entry.getKey(); // material name
                        rawMaterialsQuantitiesArray[index] = entry.getValue(); // quantity
                        index++;
                    }

                    String[] packagingArray = new String[packaging.size()];
                    int[] packagingQuantitiesArray = new int[packaging.size()];
                    index = 0;
                    for (Map.Entry<String, Integer> entry : packaging.entrySet()) {
                        packagingArray[index] = entry.getKey(); // packaging name
                        packagingQuantitiesArray[index] = entry.getValue(); // quantity
                        index++;
                    }

                    // Log selected items for debugging
                    Log.d("FoodProdActivity", "Selected Raw Materials: " + Arrays.toString(rawMaterialsArray) + " with quantities: " + Arrays.toString(rawMaterialsQuantitiesArray));
                    Log.d("FoodProdActivity", "Selected Packaging: " + Arrays.toString(packagingArray) + " with quantities: " + Arrays.toString(packagingQuantitiesArray));

                    // Deduct quantities from raw materials and packaging before updating the product
                    rawMaterialAdapter.deductQuantitiesInFirestore(db);
                    packagingAdapter.deductQuantitiesInFirestore(db);

                    // Update the product quantity and total price in Firestore
                    updateQuantitiesInFirestore(selectedProduct, productionQuantity);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void updateQuantitiesInFirestore(DataClass selectedProduct, int productionQuantity) {
        int newProductQty = selectedProduct.getProdQty() + productionQuantity; // Update quantity based on production
        double prodCost = selectedProduct.getProdCost(); // Get the current cost per unit
        double totalPrice = newProductQty * prodCost; // Calculate total price

        db.collection("products")
                .whereEqualTo("prodName", selectedProduct.getProdName())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String documentId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        // Update quantity and total price in Firestore
                        db.collection("products")
                                .document(documentId)
                                .update("prodQty", newProductQty, "prodTotalPrice", totalPrice)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Production completed and quantities updated", Toast.LENGTH_SHORT).show();

                                    // Save production record
                                    saveProductionRecord(selectedProduct.getProdName(), productionQuantity, totalPrice);

                                    // Store the current spinner position
                                    int selectedPosition = productSpinner.getSelectedItemPosition();

                                    // Refresh raw materials and packaging lists without resetting spinner selection
                                    loadRawMaterials();
                                    loadPackaging();

                                    // Update displayed stock for the currently selected product
                                    currentStockTextView.setText("Current Stock: " + newProductQty);

                                    // Clear the production quantity EditText
                                    productionQuantityEditText.setText("");

                                    // Restore the selected spinner position
                                    productSpinner.setSelection(selectedPosition);
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update product quantity", Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to retrieve product", Toast.LENGTH_SHORT).show());
    }

    private void saveProductionRecord(String productName, int quantityProduced, double totalPrice) {
        // Retrieve selected raw materials and their quantities
        Map<String, Integer> selectedRawMaterials = rawMaterialAdapter.getSelectedItems();
        Map<String, Integer> selectedPackaging = packagingAdapter.getSelectedItems();

        // Create a new ProductionRecord with raw materials and packaging included
        ProductionRecord record = new ProductionRecord(productName, quantityProduced, totalPrice, selectedRawMaterials, selectedPackaging);

        db.collection("production_records") // Create a new collection for production records
                .add(record)
                .addOnSuccessListener(documentReference -> {
                    Log.d("FoodProdActivity", "Production record saved with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("FoodProdActivity", "Error saving production record", e);
                });
    }


}
