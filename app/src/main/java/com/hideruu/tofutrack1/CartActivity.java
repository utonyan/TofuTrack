package com.hideruu.tofutrack1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.security.SecureRandom;
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
            if (cartItems.isEmpty()) {
                Toast.makeText(CartActivity.this, "Your cart is already empty", Toast.LENGTH_SHORT).show();
            } else {
                new AlertDialog.Builder(CartActivity.this)
                        .setTitle("Confirm Clear Cart")
                        .setMessage("Are you sure you want to clear your cart?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            ShoppingCart.clearCart();
                            cartAdapter.notifyDataSetChanged();
                            Toast.makeText(CartActivity.this, "Cart cleared", Toast.LENGTH_SHORT).show();
                            updateCartItemCountInPOS();
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        });

        // Setup Checkout button
        checkoutButton = findViewById(R.id.checkoutButton);
        checkoutButton.setEnabled(isNetworkAvailable());
        checkoutButton.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Toast.makeText(CartActivity.this, "Your cart is empty. Please add items to the cart before checking out.", Toast.LENGTH_SHORT).show();
            } else if (isNetworkAvailable()) {
                new AlertDialog.Builder(CartActivity.this)
                        .setTitle("Confirm Checkout")
                        .setMessage("Are you sure you want to proceed to checkout?")
                        .setPositiveButton("Yes", (dialog, which) -> checkout())
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();
            } else {
                Toast.makeText(CartActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void checkout() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        AtomicReference<Double> totalCost = new AtomicReference<>(0.0);
        List<ReceiptItem> receiptItems = new ArrayList<>();
        int totalItems = cartItems.size();
        AtomicReference<Integer> completedCount = new AtomicReference<>(0);

        for (CartItem cartItem : cartItems) {
            DataClass product = cartItem.getProduct();
            int quantityToSubtract = cartItem.getQuantity();

            db.collection("products")
                    .whereEqualTo("prodName", product.getProdName())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            String documentId = querySnapshot.getDocuments().get(0).getId();
                            int currentQuantity = product.getProdQty();

                            int newQuantity = currentQuantity - quantityToSubtract;
                            double itemCost = quantityToSubtract * product.getProdCost();
                            totalCost.updateAndGet(v -> v + itemCost);

                            ReceiptItem receiptItem = new ReceiptItem(product.getProdName(), product.getProdCost(), product.getProdUnitType(), quantityToSubtract);
                            receiptItems.add(receiptItem);

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

                        completedCount.updateAndGet(v -> v + 1);
                        if (completedCount.get() == totalItems) {
                            checkAndSaveReceipt(receiptItems, totalCost.get());
                        }
                    });
        }
    }

    private void checkAndSaveReceipt(List<ReceiptItem> receiptItems, double totalCost) {
        String documentName = "SalesRecord_" + generateRandomReceiptId();

        db.collection("receipts")
                .whereEqualTo("documentName", documentName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String existingReceiptId = task.getResult().getDocuments().get(0).getId();
                        db.collection("receipts").document(existingReceiptId)
                                .update("count", FieldValue.increment(1))
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(CartActivity.this, "Receipt updated: " + documentName, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(CartActivity.this, "Failed to update receipt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        saveReceipt(receiptItems, totalCost, documentName);
                    }
                    ShoppingCart.clearCart();
                    cartAdapter.notifyDataSetChanged();
                    updateCartItemCountInPOS();
                    finish();
                });
    }

    private String generateRandomReceiptId() {
        SecureRandom random = new SecureRandom();
        StringBuilder receiptId = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int randomChar = random.nextInt(36);
            receiptId.append((char) (randomChar < 10 ? '0' + randomChar : 'A' + (randomChar - 10)));
        }
        return receiptId.toString();
    }

    private void saveReceipt(List<ReceiptItem> receiptItems, double totalCost, String documentName) {
        Receipt receipt = new Receipt(receiptItems, totalCost, new Date(), documentName);

        db.collection("receipts")
                .add(receipt)
                .addOnSuccessListener(documentReference -> {
                    // Toast.makeText(CartActivity.this, "Receipt saved: " + documentReference.getId(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CartActivity.this, "Failed to save receipt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateCartItemCountInPOS() {
        Intent intent = new Intent("com.hideruu.tofutrack1.UPDATE_CART_ITEM_COUNT");
        sendBroadcast(intent);
    }
}
