package com.hideruu.tofutrack1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FoodProdRecordAdapter extends RecyclerView.Adapter<FoodProdRecordAdapter.ProductionRecordViewHolder> {

    private List<ProductionRecord> productionRecordList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a, MMM dd, yyyy", Locale.getDefault());

    public FoodProdRecordAdapter(List<ProductionRecord> productionRecordList) {
        this.productionRecordList = productionRecordList;
    }

    @NonNull
    @Override
    public ProductionRecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food_prod_record, parent, false);
        return new ProductionRecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductionRecordViewHolder holder, int position) {
        ProductionRecord productionRecord = productionRecordList.get(position);

        holder.productName.setText("Product: " + productionRecord.getProductName());
        holder.quantityProduced.setText("Quantity Produced: " + productionRecord.getQuantityProduced());
        holder.totalPrice.setText(String.format("Total Price: ₱%.2f", productionRecord.getTotalPrice()));
        holder.timestamp.setText("Date: " + dateFormat.format(productionRecord.getTimestamp()));

        // Clear previous views
        holder.rawMaterialsLayout.removeAllViews();
        holder.packagingLayout.removeAllViews();

        // Display selected raw materials
        for (Map.Entry<String, Integer> entry : productionRecord.getRawMaterials().entrySet()) {
            TextView rawMaterialTextView = new TextView(holder.itemView.getContext());
            rawMaterialTextView.setText(String.format("%s (x%d)", entry.getKey(), entry.getValue()));
            rawMaterialTextView.setTextSize(14);
            holder.rawMaterialsLayout.addView(rawMaterialTextView);
        }

        // Display selected packaging
        for (Map.Entry<String, Integer> entry : productionRecord.getPackaging().entrySet()) {
            TextView packagingTextView = new TextView(holder.itemView.getContext());
            packagingTextView.setText(String.format("%s (x%d)", entry.getKey(), entry.getValue()));
            packagingTextView.setTextSize(14);
            holder.packagingLayout.addView(packagingTextView);
        }
    }

    @Override
    public int getItemCount() {
        return productionRecordList.size();
    }

    static class ProductionRecordViewHolder extends RecyclerView.ViewHolder {
        TextView productName, quantityProduced, totalPrice, timestamp;
        LinearLayout rawMaterialsLayout, packagingLayout;

        public ProductionRecordViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productNameTextView);
            quantityProduced = itemView.findViewById(R.id.quantityProducedTextView);
            totalPrice = itemView.findViewById(R.id.totalPriceTextView);
            timestamp = itemView.findViewById(R.id.timestampTextView);
            rawMaterialsLayout = itemView.findViewById(R.id.rawMaterialsLayout); // Use LinearLayout for raw materials
            packagingLayout = itemView.findViewById(R.id.packagingLayout); // Use LinearLayout for packaging
        }
    }
}
