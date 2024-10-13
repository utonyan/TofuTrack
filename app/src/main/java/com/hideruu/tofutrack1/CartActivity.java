package com.hideruu.tofutrack1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CartActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

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
    }

    // Method to reset item count in posActivity
    private void updateCartItemCountInPOS() {
        // Assuming posActivity is the previous activity, use a broadcast or similar method
        Intent intent = new Intent("com.hideruu.tofutrack1.UPDATE_CART_ITEM_COUNT");
        sendBroadcast(intent);
    }
}
