package com.hideruu.tofutrack1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProductionActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_PRODUCTION = 1;
    private RecyclerView recyclerView;
    private ProductionAdapter adapter; // Use ProductionAdapter
    private List<DataClass> productList;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private static final String RAW_MATERIAL_GROUP = "Raw Material"; // Group to filter
    private static final String TARGET_PRODUCT_NAME = "Soybean"; // Product name to filter for

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_production);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);

        // Initialize product list and adapter with OnItemClickListener
        productList = new ArrayList<>();
        adapter = new ProductionAdapter(productList, this::onProductClick); // Set the ProductionAdapter with click listener
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        recyclerView.setAdapter(adapter);

        // Fetch data from Firestore
        fetchData();
    }

    private void fetchData() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("products")
                .whereEqualTo("prodGroup", RAW_MATERIAL_GROUP) // Filter for Raw Material group
                .whereEqualTo("prodName", TARGET_PRODUCT_NAME) // Filter for product name "Soybean"
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        productList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            DataClass product = document.toObject(DataClass.class);
                            productList.add(product);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.d("Firestore", "No Soybean found in cache, fetching from server...");
                        fetchDataFromServer();
                    }
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error loading data", e);
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void fetchDataFromServer() {
        db.collection("products")
                .whereEqualTo("prodGroup", RAW_MATERIAL_GROUP) // Filter for Raw Material group
                .whereEqualTo("prodName", TARGET_PRODUCT_NAME) // Filter for product name "Soybean"
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        DataClass product = document.toObject(DataClass.class);
                        productList.add(product);
                    }
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error loading data from server", e);
                    progressBar.setVisibility(View.GONE);
                });
    }

    // Handle product click
    private void onProductClick(DataClass product) {
        Intent intent = new Intent(ProductionActivity.this, ProductionDetailActivity.class);
        intent.putExtra("productId", product.getProductId());
        intent.putExtra("prodName", product.getProdName());
        intent.putExtra("prodDesc", product.getProdDesc());
        intent.putExtra("prodGroup", product.getProdGroup());
        intent.putExtra("prodQty", product.getProdQty());
        intent.putExtra("prodUnitType", product.getProdUnitType());
        intent.putExtra("prodCost", product.getProdCost());
        intent.putExtra("prodTotalPrice", product.getProdTotalPrice());
        intent.putExtra("prodImage", product.getDataImage());

        // Start ProductionDetailActivity
        startActivityForResult(intent, REQUEST_CODE_PRODUCTION);
    }
}
