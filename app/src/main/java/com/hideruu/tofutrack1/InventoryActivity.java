package com.hideruu.tofutrack1;

import android.graphics.Color;
import android.graphics.PorterDuff;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import androidx.appcompat.widget.SearchView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_UPLOAD = 1;
    public static final int REQUEST_CODE_DELETE = 2; // Add this in InventoryActivity
    private static final int REQUEST_CODE_UPDATE = 3;


    private FloatingActionButton fab;
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<DataClass> productList;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        fab = findViewById(R.id.fab);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        searchView = findViewById(R.id.searchView);
        fab.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);


        // Initialize product list and adapter
        productList = new ArrayList<>();
        adapter = new InventoryAdapter(productList);

        // Set layout manager to GridLayoutManager
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        // FloatingActionButton to open UploadActivity for adding a new product
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InventoryActivity.this, UploadActivity.class);
                startActivityForResult(intent, REQUEST_CODE_UPLOAD);
            }
        });

        // Set up SearchView
        setupSearchView();

        // Fetch data from Firestore
        fetchData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_UPLOAD && resultCode == RESULT_OK) {
            fetchData(); // Reload data when coming back from UploadActivity
        } else if (requestCode == REQUEST_CODE_DELETE && resultCode == RESULT_OK) {
            fetchData(); // Reload data when coming back after deletion
        } else if (requestCode == REQUEST_CODE_UPDATE && resultCode == RESULT_OK) {
            fetchData(); // Reload data when coming back from UpdateActivity
        }
    }



    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Trigger the search when the user submits a query
                searchProducts(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Trigger search as the user types
                if (newText.isEmpty()) {
                    fetchData(); // Load all data when no query is entered
                } else {
                    searchProducts(newText);
                }
                return true;
            }
        });
    }

    private void searchProducts(String query) {
        progressBar.setVisibility(View.VISIBLE);

        // Convert query to lowercase for case-insensitive search
        String queryLower = query.toLowerCase();

        // Fetch all products from Firestore
        db.collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear(); // Clear the current list

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        DataClass product = document.toObject(DataClass.class);

                        // Convert fields to lowercase and check if they contain the query
                        boolean matches = false;
                        if (product.getProdName().toLowerCase().contains(queryLower) ||
                                product.getProdGroup().toLowerCase().contains(queryLower) ||
                                String.valueOf(product.getProdQty()).toLowerCase().contains(queryLower) ||
                                String.valueOf(product.getProdCost()).toLowerCase().contains(queryLower) ||
                                String.valueOf(product.getProdTotalPrice()).toLowerCase().contains(queryLower)) {
                            matches = true;
                        }

                        if (matches) {
                            productList.add(product); // Add matching products
                        }
                    }

                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error searching products", e);
                    progressBar.setVisibility(View.GONE);
                });
    }


    private boolean matchesQuery(DataClass product, String query) {
        String lowerCaseQuery = query.toLowerCase();

        // Check if any field matches the query
        return product.getProdName().toLowerCase().contains(lowerCaseQuery) ||
                product.getProdGroup().toLowerCase().contains(lowerCaseQuery) ||
                String.valueOf(product.getProdQty()).contains(lowerCaseQuery) ||
                String.valueOf(product.getProdCost()).contains(lowerCaseQuery);
    }

    private void fetchData() {
        progressBar.setVisibility(View.VISIBLE);

        // Fetch all products if no search is active
        db.collection("products")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        productList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            DataClass product = document.toObject(DataClass.class);
                            productList.add(product);
                        }

                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                    } else {
                        Log.d("Firestore", "No data found in cache, fetching from server...");
                        fetchDataFromServer();
                    }
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

}
