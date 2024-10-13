package com.hideruu.tofutrack1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class posActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_POS = 1;
    private RecyclerView recyclerView;
    private posAdapter adapter; // Changed to posAdapter
    private List<DataClass> productList;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private static final String PRODUCT_GROUP = "Product"; // Group to filter for Product

    // BroadcastReceiver for cart item count updates
    private BroadcastReceiver cartItemCountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateCartItemCount(); // Update item count when cart is cleared
            fetchData(); // Optionally refresh the product list if needed
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pos); // Ensure you have an appropriate layout for POS

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);

        // Initialize product list and adapter with OnItemClickListener
        productList = new ArrayList<>();
        adapter = new posAdapter(productList, this::onProductClick); // Use posAdapter
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        // Fetch data from Firestore
        fetchData();

        // Open cart activity when FAB is clicked
        findViewById(R.id.fab).setOnClickListener(v -> {
            Intent intent = new Intent(posActivity.this, CartActivity.class);
            startActivity(intent);
        });

        // Register the receiver for cart item count updates
        IntentFilter filter = new IntentFilter("com.hideruu.tofutrack1.UPDATE_CART_ITEM_COUNT");
        registerReceiver(cartItemCountReceiver, filter);
    }

    private void fetchData() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("products")
                .whereEqualTo("prodGroup", PRODUCT_GROUP) // Filter for Product group
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
                        Log.d("Firestore", "No Product found in cache, fetching from server...");
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
                .whereEqualTo("prodGroup", PRODUCT_GROUP) // Filter for Product group
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
        // Inflate the custom dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_quantity, null);

        // Create an AlertDialog for quantity input
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        // Get references to the UI elements in the dialog
        EditText input = dialogView.findViewById(R.id.inputQuantity);
        TextView quantityPrompt = dialogView.findViewById(R.id.quantityPrompt);

        // Set up the buttons
        builder.setPositiveButton("Add to Cart", (dialog, which) -> {
            String quantityStr = input.getText().toString().trim();
            if (!quantityStr.isEmpty()) {
                int quantityToAdd = Integer.parseInt(quantityStr);

                // Get current quantity in the cart for this product
                int currentQuantityInCart = 0;
                for (CartItem item : ShoppingCart.getCartItems()) {
                    if (item.getProduct().getProdName().equals(product.getProdName())) {
                        currentQuantityInCart = item.getQuantity();
                        break;
                    }
                }

                // Check if requested quantity plus current quantity exceeds available stock
                if (currentQuantityInCart + quantityToAdd > product.getProdQty()) {
                    Toast.makeText(this, "Cannot add more than available stock (" + product.getProdQty() + ")", Toast.LENGTH_SHORT).show();
                } else {
                    // Add product to cart or update its quantity if it already exists
                    ShoppingCart.addItemToCart(product, quantityToAdd);
                    updateCartItemCount();
                    Toast.makeText(this, "Added to Cart!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateCartItemCount() {
        TextView cartItemCount = findViewById(R.id.cartItemCount);
        int itemCount = ShoppingCart.getItemCount(); // Implement getItemCount in ShoppingCart class
        cartItemCount.setText(String.valueOf(itemCount));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(cartItemCountReceiver); // Unregister the receiver
        // Clear the cart when posActivity is closed
        ShoppingCart.clearCart();
    }
}
