package com.hideruu.tofutrack1;

import android.util.Log;
import android.content.Intent;
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

    private List<DataClass> productList;

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
        Log.d("InventoryAdapter", "Product ID: " + product.getProductId());

        // Set product details to views
        holder.prodName.setText(product.getProdName());
        holder.prodDesc.setText("Description: " + product.getProdDesc());
        holder.prodGroup.setText("Group: " + product.getProdGroup());
        holder.prodQty.setText("Quantity: " + product.getProdQty());
        holder.prodCost.setText("Cost per unit: ₱" + String.format("%.2f", product.getProdCost()));
        holder.prodTotalPrice.setText("Total: ₱" + String.format("%.2f", product.getProdTotalPrice()));

        // Load image using Glide
        Glide.with(holder.itemView.getContext())
                .load(product.getDataImage())
                .into(holder.prodImage);

        // Handle item click to open DetailActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), DetailActivity.class);
            intent.putExtra("productId", product.getProductId()); // Pass productId
            intent.putExtra("prodName", product.getProdName());
            intent.putExtra("prodDesc", product.getProdDesc());
            intent.putExtra("prodGroup", product.getProdGroup());
            intent.putExtra("prodQty", product.getProdQty());
            intent.putExtra("prodCost", product.getProdCost());
            intent.putExtra("prodTotalPrice", product.getProdTotalPrice());
            intent.putExtra("prodImage", product.getDataImage());
            holder.itemView.getContext().startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class InventoryViewHolder extends RecyclerView.ViewHolder {

        TextView prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice;
        ImageView prodImage;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views
            prodName = itemView.findViewById(R.id.prodName);
            prodDesc = itemView.findViewById(R.id.prodDesc);
            prodGroup = itemView.findViewById(R.id.prodGroup);
            prodQty = itemView.findViewById(R.id.prodQty);
            prodCost = itemView.findViewById(R.id.prodCost);
            prodTotalPrice = itemView.findViewById(R.id.prodTotalPrice);
            prodImage = itemView.findViewById(R.id.prodImage);
        }
    }
}
