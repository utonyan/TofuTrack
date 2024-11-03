package com.hideruu.tofutrack1;

import android.graphics.Color;
import android.graphics.PorterDuff;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog; // Import AlertDialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import androidx.appcompat.widget.SearchView;
import com.github.clans.fab.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_UPLOAD = 1;
    public static final int REQUEST_CODE_DELETE = 2;
    private static final int REQUEST_CODE_UPDATE = 3;

    private FloatingActionButton fab;
    private FloatingActionButton filterFab;
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<DataClass> productList;
    private List<DataClass> originalProductList; // Store original product list
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private SearchView searchView;
    private String currentlySelectedGroup; // Variable to track currently selected group

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        fab = findViewById(R.id.fab);
        filterFab = findViewById(R.id.fab_filter);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        searchView = findViewById(R.id.searchView);
        fab.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

        // Initialize product list and adapter
        productList = new ArrayList<>();
        originalProductList = new ArrayList<>(); // Initialize the original product list
        adapter = new InventoryAdapter(productList);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        // FloatingActionButton to open UploadActivity for adding a new product
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(InventoryActivity.this, UploadActivity.class);
            startActivityForResult(intent, REQUEST_CODE_UPLOAD);
        });

        // Setup SearchView
        setupSearchView();

        // Fetch data from Firestore
        fetchData();

        // Set up filter FAB
        filterFab.setOnClickListener(view -> showGroupSelectionDialog()); // Show dialog on click
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchProducts(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    // When search text is empty, reload original product list and filter by selected group
                    if (currentlySelectedGroup != null && !currentlySelectedGroup.isEmpty()) {
                        productList.clear(); // Clear current product list
                        productList.addAll(originalProductList); // Load original products
                        filterProductsByGroup(currentlySelectedGroup); // Filter by selected group
                    } else {
                        fetchData(); // Load all data if no group is selected
                    }
                } else {
                    searchProducts(newText);
                }
                return true;
            }
        });
    }

    private void fetchData() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("products")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        productList.clear();
                        originalProductList.clear(); // Clear original product list
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            DataClass product = document.toObject(DataClass.class);
                            productList.add(product); // Add all products
                            originalProductList.add(product); // Add to original list as well
                        }
                        // Sort both lists by group and then alphabetically by product name
                        sortProductsByGroupAndName(productList);
                        sortProductsByGroupAndName(originalProductList);

                        adapter.notifyDataSetChanged();
                    } else {
                        Log.d("Firestore", "No data found in cache, fetching from server...");
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
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    originalProductList.clear(); // Clear original product list
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        DataClass product = document.toObject(DataClass.class);

                        // Exclude products with the "Product" group
                        if (!"Product".equalsIgnoreCase(product.getProdGroup())) {
                            productList.add(product);
                            originalProductList.add(product); // Add to original list as well
                        }
                    }
                    // Sort both lists by group and then alphabetically by product name
                    sortProductsByGroupAndName(productList);
                    sortProductsByGroupAndName(originalProductList);

                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error loading data from server", e);
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void sortProductsByGroupAndName(List<DataClass> products) {
        Collections.sort(products, (p1, p2) -> {
            int groupComparison = compareGroups(p1.getProdGroup(), p2.getProdGroup());
            if (groupComparison != 0) {
                return groupComparison; // Return group comparison result
            }
            return p1.getProdName().compareToIgnoreCase(p2.getProdName()); // Sort alphabetically by name
        });
    }

    private int compareGroups(String group1, String group2) {
        if ("Product".equalsIgnoreCase(group1) && !"Product".equalsIgnoreCase(group2)) {
            return 1; // group1 comes after group2
        } else if (!"Product".equalsIgnoreCase(group1) && "Product".equalsIgnoreCase(group2)) {
            return -1; // group1 comes before group2
        }
        return group1.compareToIgnoreCase(group2); // Otherwise, sort normally
    }

    private void searchProducts(String query) {
        progressBar.setVisibility(View.VISIBLE);
        String queryLower = query.toLowerCase();
        List<DataClass> searchResults = new ArrayList<>();

        for (DataClass product : productList) {
            boolean matchesGroup = currentlySelectedGroup == null || currentlySelectedGroup.isEmpty() || product.getProdGroup().equalsIgnoreCase(currentlySelectedGroup);
            boolean matchesQuery = product.getProdName().toLowerCase().contains(queryLower) ||
                    product.getProdGroup().toLowerCase().contains(queryLower) ||
                    String.valueOf(product.getProdQty()).toLowerCase().contains(queryLower) ||
                    String.valueOf(product.getProdCost()).toLowerCase().contains(queryLower) ||
                    String.valueOf(product.getProdTotalPrice()).toLowerCase().contains(queryLower);

            if (matchesGroup && matchesQuery) {
                searchResults.add(product); // Add matching products
            }
        }

        productList.clear();
        productList.addAll(searchResults);
        adapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
    }

    private void showGroupSelectionDialog() {
        String[] productGroups = getResources().getStringArray(R.array.product_groups);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Select Product Group")
                .setItems(productGroups, (dialog, which) -> {
                    String selectedGroup = productGroups[which];

                    // Check if a group is already selected
                    if (currentlySelectedGroup != null && !currentlySelectedGroup.isEmpty()) {
                        // Inform the user they need to reset before changing the filter
                        new AlertDialog.Builder(this)
                                .setTitle("Active Filter")
                                .setMessage("Please reset the current filter before selecting a new one.")
                                .setPositiveButton("OK", (resetDialog, resetWhich) -> resetDialog.dismiss())
                                .show();
                    } else {
                        currentlySelectedGroup = selectedGroup; // Update the currently selected group
                        filterProductsByGroup(selectedGroup); // Filter by new group
                    }
                })
                .setPositiveButton("Reset Filters", (dialog, which) -> {
                    fetchData(); // Reset the filters by reloading all products
                    currentlySelectedGroup = null; // Clear the currently selected group
                })
                .show();
    }

    private void filterProductsByGroup(String selectedGroup) {
        progressBar.setVisibility(View.VISIBLE); // Show progress bar
        List<DataClass> filteredList = new ArrayList<>();

        // Load all products and filter only if a group is selected
        if (currentlySelectedGroup != null && !currentlySelectedGroup.isEmpty()) {
            for (DataClass product : originalProductList) {
                if (product.getProdGroup().equalsIgnoreCase(selectedGroup)) {
                    filteredList.add(product); // Add products that match the selected group
                }
            }
            productList.clear(); // Clear current product list
            productList.addAll(filteredList); // Add filtered products
            adapter.notifyDataSetChanged(); // Notify adapter
        } else {
            // If no group is selected, fetch all data
            fetchData();
        }
        progressBar.setVisibility(View.GONE); // Hide progress bar
    }
}
