package com.hideruu.tofutrack1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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
    private FirebaseFirestore db;
    private Button checkoutButton;
    private SharedPreferences sharedPreferences;
    private TextView totalPriceText, changeText;
    private EditText paymentInput;
    private static final String PREFS_NAME = "TofuTrackPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // Initialize Firestore and SharedPreferences
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerViewCart);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get cart items and set up adapter
        cartItems = ShoppingCart.getCartItems();
        cartAdapter = new CartAdapter(cartItems);
        recyclerView.setAdapter(cartAdapter);

        // Initialize total price and payment fields
        totalPriceText = findViewById(R.id.totalPriceText);
        paymentInput = findViewById(R.id.paymentInput);
        changeText = findViewById(R.id.changeText);

        // Update total price on activity start
        updateTotalPrice();

        // Setup Checkout button
        checkoutButton = findViewById(R.id.checkoutButton);
        checkoutButton.setEnabled(isNetworkAvailable());
        checkoutButton.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Toast.makeText(CartActivity.this, "Your cart is empty. Please add items to the cart before checking out.", Toast.LENGTH_SHORT).show();
            } else if (isNetworkAvailable()) {
                double totalAmount = calculateTotalAmount();
                try {
                    double payment = Double.parseDouble(paymentInput.getText().toString());
                    double change = payment - totalAmount;

                    if (payment >= totalAmount) {
                        showConfirmationDialog(totalAmount, payment, change);
                    } else {
                        Toast.makeText(this, "Payment is less than total amount.", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a valid payment amount.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(CartActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
            }
        });

        // Add TextWatcher to paymentInput
        paymentInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateChangeText();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void updateTotalPrice() {
        double totalPrice = calculateTotalAmount();
        totalPriceText.setText(String.format("Total: ₱%.2f", totalPrice));
    }

    private double calculateTotalAmount() {
        double total = 0.0;
        for (CartItem item : cartItems) {
            total += item.getQuantity() * item.getProduct().getProdCost();
        }
        return total;
    }

    private void showConfirmationDialog(double totalAmount, double payment, double change) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Checkout")
                .setMessage("Payment: ₱" + payment + "\nChange: ₱" + change + "\nTotal: ₱" + totalAmount + "\n " + "\nProceed to checkout?")
                .setPositiveButton("Yes", (dialog, which) -> checkout(payment, change))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void checkout(double payment, double change) {
        AtomicReference<Double> totalCost = new AtomicReference<>(calculateTotalAmount());
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
                                    .addOnSuccessListener(aVoid -> {})
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(CartActivity.this, "Failed to update product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(CartActivity.this, "Product not found: " + product.getProdName(), Toast.LENGTH_SHORT).show();
                        }

                        completedCount.updateAndGet(v -> v + 1);
                        if (completedCount.get() == totalItems) {
                            checkAndSaveReceipt(receiptItems, totalCost.get(), payment, change);
                        }
                    });
        }
    }

    private void checkAndSaveReceipt(List<ReceiptItem> receiptItems, double totalCost, double payment, double change) {
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
                        saveReceipt(receiptItems, totalCost, documentName, payment, change);
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

    private void saveReceipt(List<ReceiptItem> receiptItems, double totalCost, String documentName, double payment, double change) {
        Receipt receipt = new Receipt(receiptItems, totalCost, new Date(), documentName, payment, change);

        db.collection("receipts")
                .add(receipt)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(CartActivity.this, "Receipt saved with change: ₱" + change, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CartActivity.this, "Failed to save receipt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateChangeText() {
        try {
            double payment = Double.parseDouble(paymentInput.getText().toString());
            double totalAmount = calculateTotalAmount();
            double change = payment - totalAmount;
            changeText.setText(String.format("Change: ₱%.2f", change));
        } catch (NumberFormatException e) {
            changeText.setText("Change: ₱0.00");
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void updateCartItemCountInPOS() {
        // Assuming this method updates the item count in your POS activity or similar
    }
}
