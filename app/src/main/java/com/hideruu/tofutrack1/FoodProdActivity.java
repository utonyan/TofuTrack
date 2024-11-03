package com.hideruu.tofutrack1;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

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
                        MaterialAdapter adapter = new MaterialAdapter(this, list, recyclerView);
                        recyclerView.setAdapter(adapter);
                        if (group.equals("Raw Material")) {
                            rawMaterialAdapter = adapter;
                        } else {
                            packagingAdapter = adapter;
                        }
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

        // Check if selected quantities of raw materials and packaging are valid
        if (!rawMaterialAdapter.hasSelectedItems() || !packagingAdapter.hasSelectedItems()) {
            Toast.makeText(this, "Please select at least one raw material and one packaging item", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!rawMaterialAdapter.hasSufficientQuantities() || !packagingAdapter.hasSufficientQuantities()) {
            Toast.makeText(this, "Insufficient materials for the entered quantities", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the production quantity is greater than current stock
        if (productionQuantity <= 0 || productionQuantity > selectedProduct.getProdQty()) {
            Toast.makeText(this, "Production quantity must be greater than 0 and less than or equal to current stock", Toast.LENGTH_SHORT).show();
            return;
        }

        // Deduct quantities from raw materials and packaging before updating the product
        rawMaterialAdapter.deductQuantitiesInFirestore(db);
        packagingAdapter.deductQuantitiesInFirestore(db);

        // Update the product quantity and total price in Firestore
        updateQuantitiesInFirestore(selectedProduct, productionQuantity);
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

}