package com.hideruu.tofutrack1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private static final String TAG = "InventoryAdapter";
    private final List<DataClass> productList;

    public InventoryAdapter(List<DataClass> productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        DataClass product = productList.get(position);

        // Log product ID
        Log.d(TAG, "Product ID: " + product.getProductId());

        // Set product details to views
        holder.prodName.setText(product.getProdName());
        holder.prodGroup.setText("Group: " + product.getProdGroup());
        holder.prodQty.setText("Quantity: " + product.getProdQty() + " " + product.getProdUnitType()); // Add unit type
        holder.prodCost.setText("Cost per unit: ₱" + String.format("%.2f", product.getProdCost()));
        holder.prodTotalPrice.setText("Total: ₱" + String.format("%.2f", product.getProdTotalPrice()));

        // Load image using Glide
        Glide.with(holder.itemView.getContext())
                .load(product.getDataImage())
                .into(holder.prodImage);

        // Handle item click to open DetailActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), DetailActivity.class);
            intent.putExtra("productId", product.getProductId());
            intent.putExtra("prodName", product.getProdName());
            intent.putExtra("prodDesc", product.getProdDesc());
            intent.putExtra("prodGroup", product.getProdGroup());
            intent.putExtra("prodQty", product.getProdQty());
            intent.putExtra("prodUnitType", product.getProdUnitType()); // Pass unit type to DetailActivity
            intent.putExtra("prodCost", product.getProdCost());
            intent.putExtra("prodTotalPrice", product.getProdTotalPrice());
            intent.putExtra("prodImage", product.getDataImage());

            // Safely start DetailActivity for result
            Context context = holder.itemView.getContext();
            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(intent, InventoryActivity.REQUEST_CODE_DELETE);
            } else {
                Log.e(TAG, "Context is not an Activity");
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class InventoryViewHolder extends RecyclerView.ViewHolder {

        TextView prodName, prodGroup, prodQty, prodCost, prodTotalPrice;
        ImageView prodImage;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views
            prodName = itemView.findViewById(R.id.prodName);
            prodGroup = itemView.findViewById(R.id.prodGroup);
            prodQty = itemView.findViewById(R.id.prodQty);
            prodCost = itemView.findViewById(R.id.prodCost);
            prodTotalPrice = itemView.findViewById(R.id.prodTotalPrice);
            prodImage = itemView.findViewById(R.id.prodImage);
        }
    }
}
