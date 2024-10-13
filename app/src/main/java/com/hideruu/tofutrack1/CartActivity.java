package com.hideruu.tofutrack1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class CartActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems;
    private FirebaseFirestore db; // Firestore instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

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
        Button checkoutButton = findViewById(R.id.checkoutButton);
        checkoutButton.setOnClickListener(v -> checkout());
    }

    // Method to handle checkout process
    private void checkout() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

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

                            // Calculate new quantity and total price
                            int newQuantity = currentQuantity - quantityToSubtract;
                            double totalCost = newQuantity * product.getProdCost();

                            // Update quantity and total price in Firestore
                            db.collection("products").document(documentId)
                                    .update("prodQty", newQuantity, "prodTotalPrice", totalCost)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(CartActivity.this, "Checkout successful for " + product.getProdName(), Toast.LENGTH_SHORT).show();

                                        // Send broadcast to update cart item count in posActivity
                                        updateCartItemCountInPOS();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(CartActivity.this, "Failed to update product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(CartActivity.this, "Product not found: " + product.getProdName(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        // Clear the cart after processing all items
        ShoppingCart.clearCart();
        cartAdapter.notifyDataSetChanged(); // Refresh the RecyclerView

        // Close the CartActivity
        finish();
    }


    // Method to reset item count in posActivity
    private void updateCartItemCountInPOS() {
        Intent intent = new Intent("com.hideruu.tofutrack1.UPDATE_CART_ITEM_COUNT");
        sendBroadcast(intent);
    }
}
