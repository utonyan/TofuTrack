package com.hideruu.tofutrack1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class CartActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems;
    private FirebaseFirestore db; // Firestore instance
    private Button checkoutButton;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "TofuTrackPrefs";
    private static final String KEY_RECEIPT_ID = "lastReceiptId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        recyclerView = findViewById(R.id.recyclerViewCart);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get cart items from the ShoppingCart
        cartItems = ShoppingCart.getCartItems();
        cartAdapter = new CartAdapter(cartItems);
        recyclerView.setAdapter(cartAdapter);

        // Setup Clear Cart button
        Button clearCartButton = findViewById(R.id.clearCartButton);
        clearCartButton.setOnClickListener(v -> {
            ShoppingCart.clearCart();
            cartAdapter.notifyDataSetChanged(); // Notify the adapter to refresh the RecyclerView
            Toast.makeText(CartActivity.this, "Cart cleared", Toast.LENGTH_SHORT).show();

            // Reset the cart item count in posActivity
            updateCartItemCountInPOS();
        });

        // Setup Checkout button
        checkoutButton = findViewById(R.id.checkoutButton);
        checkoutButton.setEnabled(isNetworkAvailable()); // Disable if no network
        checkoutButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                checkout();
            } else {
                Toast.makeText(CartActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to check network connectivity
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Method to handle checkout process
    private void checkout() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        AtomicReference<Double> totalCost = new AtomicReference<>(0.0);
        List<ReceiptItem> receiptItems = new ArrayList<>();
        int totalItems = cartItems.size(); // Total number of items to checkout
        AtomicReference<Integer> completedCount = new AtomicReference<>(0); // Track completed Firestore queries

        for (CartItem cartItem : cartItems) {
            DataClass product = cartItem.getProduct();
            int quantityToSubtract = cartItem.getQuantity();

            // Query Firestore for the product by name
            db.collection("products")
                    .whereEqualTo("prodName", product.getProdName())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            String documentId = querySnapshot.getDocuments().get(0).getId();
                            int currentQuantity = product.getProdQty();

                            // Calculate new quantity and item total price
                            int newQuantity = currentQuantity - quantityToSubtract;
                            double itemCost = quantityToSubtract * product.getProdCost();
                            totalCost.updateAndGet(v -> v + itemCost); // Accumulate total cost

                            // Create a ReceiptItem to add to the receipt
                            ReceiptItem receiptItem = new ReceiptItem(product.getProdName(), product.getProdCost(), product.getProdUnitType(), quantityToSubtract);
                            receiptItems.add(receiptItem); // Add the item to the receipt list

                            // Update quantity and total price in Firestore
                            db.collection("products").document(documentId)
                                    .update("prodQty", newQuantity, "prodTotalPrice", newQuantity * product.getProdCost())
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(CartActivity.this, "Checkout successful for " + product.getProdName(), Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(CartActivity.this, "Failed to update product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(CartActivity.this, "Product not found: " + product.getProdName(), Toast.LENGTH_SHORT).show();
                        }

                        // Increment the completed count and check if all queries are done
                        completedCount.updateAndGet(v -> v + 1);
                        if (completedCount.get() == totalItems) {
                            // After processing all items, create and save the receipt
                            String documentName = "SalesRecord_" + String.format("%02d", getNextReceiptId()); // Format as SalesRecord_01
                            saveReceipt(receiptItems, totalCost.get(), documentName); // Pass the document name to the save method

                            // Clear the cart after processing all items
                            ShoppingCart.clearCart();
                            cartAdapter.notifyDataSetChanged(); // Refresh the RecyclerView

                            // Notify posActivity to refresh its product list
                            updateCartItemCountInPOS();

                            // Close the CartActivity
                            finish();
                        }
                    });
        }
    }

    // Method to get the next receipt ID
    private int getNextReceiptId() {
        int lastId = sharedPreferences.getInt(KEY_RECEIPT_ID, -1); // Get the last receipt ID, default to -1
        int nextId = (lastId + 1) % Integer.MAX_VALUE; // Increment and wrap around if necessary
        sharedPreferences.edit().putInt(KEY_RECEIPT_ID, nextId).apply(); // Save the new last ID
        return nextId;
    }

    // Method to save the receipt to Firestore
    private void saveReceipt(List<ReceiptItem> receiptItems, double totalCost, String documentName) {
        Receipt receipt = new Receipt(receiptItems, totalCost, new Date(), documentName); // Pass the document name

        db.collection("receipts")
                .add(receipt)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(CartActivity.this, "Receipt saved: " + documentReference.getId(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CartActivity.this, "Failed to save receipt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Method to reset item count in posActivity
    private void updateCartItemCountInPOS() {
        Intent intent = new Intent("com.hideruu.tofutrack1.UPDATE_CART_ITEM_COUNT");
        sendBroadcast(intent);
    }
}
