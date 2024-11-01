package com.hideruu.tofutrack1;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private List<CartItem> cartItems;

    public CartAdapter(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem cartItem = cartItems.get(position);
        holder.bind(cartItem, position);
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    class CartViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productQuantity, productPrice;
        Button editButton, deleteButton;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.cartItemName);
            productQuantity = itemView.findViewById(R.id.cartItemQuantity);
            productPrice = itemView.findViewById(R.id.cartItemPrice);
            editButton = itemView.findViewById(R.id.editQuantityButton);
            deleteButton = itemView.findViewById(R.id.deleteItemButton);
        }

        public void bind(CartItem cartItem, int position) {
            productName.setText(cartItem.getProduct().getProdName());
            productQuantity.setText("Qty: " + cartItem.getQuantity());
            productPrice.setText("Price: ₱" + cartItem.getTotalPrice());

            editButton.setOnClickListener(v -> {
                editQuantity(itemView.getContext(), cartItem);
            });

            deleteButton.setOnClickListener(v -> {
                deleteItem(position);
            });
        }

        private void editQuantity(Context context, CartItem cartItem) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Edit Quantity");

            final View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_quantity, null);
            builder.setView(dialogView);

            TextView quantityInput = dialogView.findViewById(R.id.editQuantityInput);
            quantityInput.setText(String.valueOf(cartItem.getQuantity()));

            // Get the available quantity from the product
            int availableQuantity = cartItem.getProduct().getProdQty();

            builder.setPositiveButton("OK", (dialog, which) -> {
                try {
                    int newQuantity = Integer.parseInt(quantityInput.getText().toString());
                    // Check if new quantity is greater than available quantity
                    if (newQuantity > availableQuantity) {
                        Toast.makeText(context, "Cannot set quantity greater than available (" + availableQuantity + ")", Toast.LENGTH_SHORT).show();
                    } else if (newQuantity > 0) {
                        cartItem.setQuantity(newQuantity);
                    } else {
                        Toast.makeText(context, "Quantity must be greater than 0", Toast.LENGTH_SHORT).show();
                    }
                    notifyDataSetChanged(); // Refresh the adapter to reflect changes
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Invalid quantity", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        }

        private void deleteItem(int position) {
            cartItems.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, cartItems.size());
        }
    }
}
