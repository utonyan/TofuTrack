package com.hideruu.tofutrack1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private ShoppingCart shoppingCart; // Shopping cart instance
    private TextView cartItemCount; // TextView to show cart item count
    private FloatingActionButton fab; // FAB to show cart items

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pos); // Ensure you have an appropriate layout for POS

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        cartItemCount = findViewById(R.id.cartItemCount); // Initialize cart item count TextView
        fab = findViewById(R.id.fab); // Initialize the FAB
        shoppingCart = new ShoppingCart(); // Create a new shopping cart instance

        // Initialize product list and adapter with OnItemClickListener
        productList = new ArrayList<>();
        adapter = new posAdapter(productList, this::onProductClick);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        // Set FAB click listener
        fab.setOnClickListener(view -> showCartItems());

        // Fetch data from Firestore
        fetchData();
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
        // Show dialog to input quantity
        showQuantityDialog(product);
    }

    private void showQuantityDialog(DataClass product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Quantity");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String value = input.getText().toString();
            int quantity = Integer.parseInt(value);
            shoppingCart.addItem(product, quantity); // Add item to cart
            updateCartItemCount(); // Update cart item count
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Show the shopping cart items
    private void showCartItems() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Shopping Cart Items");

        // Create a StringBuilder to hold the cart item details
        StringBuilder cartDetails = new StringBuilder();

        // Get the cart items from ShoppingCart
        for (ShoppingCart.CartItem item : shoppingCart.getCartItems().values()) {
            cartDetails.append(item.getProduct().getProdName())
                    .append(": ")
                    .append(item.getQuantity())
                    .append("\n");
        }

        // Check if the cart is empty
        if (cartDetails.length() == 0) {
            cartDetails.append("Your cart is empty.");
        }

        builder.setMessage(cartDetails.toString()); // Set the message to display cart details

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        // Show the dialog
        builder.show();
    }

    // Update cart item count TextView
    private void updateCartItemCount() {
        int totalCount = 0;
        for (ShoppingCart.CartItem item : shoppingCart.getCartItems().values()) {
            totalCount += item.getQuantity();
        }
        cartItemCount.setText(String.valueOf(totalCount)); // Update the TextView
    }
}
