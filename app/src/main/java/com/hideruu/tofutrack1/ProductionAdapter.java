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

public class ProductionAdapter extends RecyclerView.Adapter<ProductionAdapter.ProductionViewHolder> {

    private static final String TAG = "ProductionAdapter";
    private final List<DataClass> productList;
    private final OnItemClickListener listener;

    public ProductionAdapter(List<DataClass> productList, OnItemClickListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductionViewHolder holder, int position) {
        DataClass product = productList.get(position);

        // Log product details
        Log.d(TAG, "Binding product at position: " + position + ", ID: " + product.getProductId());

        // Set product details to views
        holder.prodName.setText(product.getProdName());
        holder.prodGroup.setText("Group: " + product.getProdGroup());
        holder.prodQty.setText("Quantity: " + product.getProdQty() + " " + product.getProdUnitType());
        holder.prodCost.setText("Cost per unit: ₱" + String.format("%.2f", product.getProdCost()));
        holder.prodTotalPrice.setText("Total: ₱" + String.format("%.2f", product.getProdTotalPrice()));

        // Load image using Glide with error handling
        Glide.with(holder.itemView.getContext())
                .load(product.getDataImage())
                .error(R.drawable.kitalogo) // Placeholder image
                .into(holder.prodImage);

        // Handle item click
        holder.itemView.setOnClickListener(v -> listener.onItemClick(product));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductionViewHolder extends RecyclerView.ViewHolder {

        TextView prodName, prodGroup, prodQty, prodCost, prodTotalPrice;
        ImageView prodImage;

        public ProductionViewHolder(@NonNull View itemView) {
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

    public void updateProductList(List<DataClass> newProductList) {
        this.productList.clear();
        this.productList.addAll(newProductList);
        notifyDataSetChanged(); // Notify the adapter of data changes
    }

    // Listener interface for item clicks
    public interface OnItemClickListener {
        void onItemClick(DataClass product);
    }
}
