package com.hideruu.tofutrack1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        // Display selected raw materials
        holder.rawMaterials.setText("Raw Materials: " + formatRawMaterials(productionRecord.getRawMaterials()));
        holder.packaging.setText("Packaging: " + formatPackaging(productionRecord.getPackaging()));
    }

    private String formatRawMaterials(Map<String, Integer> rawMaterials) {
        StringBuilder formatted = new StringBuilder();
        for (Map.Entry<String, Integer> entry : rawMaterials.entrySet()) {
            formatted.append(entry.getKey()).append(" (").append(entry.getValue()).append("), ");
        }
        return formatted.length() > 0 ? formatted.substring(0, formatted.length() - 2) : "None";
    }

    private String formatPackaging(Map<String, Integer> packaging) {
        StringBuilder formatted = new StringBuilder();
        for (Map.Entry<String, Integer> entry : packaging.entrySet()) {
            formatted.append(entry.getKey()).append(" (").append(entry.getValue()).append("), ");
        }
        return formatted.length() > 0 ? formatted.substring(0, formatted.length() - 2) : "None";
    }

    @Override
    public int getItemCount() {
        return productionRecordList.size();
    }

    static class ProductionRecordViewHolder extends RecyclerView.ViewHolder {
        TextView productName, quantityProduced, totalPrice, timestamp, rawMaterials, packaging;

        public ProductionRecordViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productNameTextView);
            quantityProduced = itemView.findViewById(R.id.quantityProducedTextView);
            totalPrice = itemView.findViewById(R.id.totalPriceTextView);
            timestamp = itemView.findViewById(R.id.timestampTextView);
            rawMaterials = itemView.findViewById(R.id.rawMaterialsTextView); // Make sure to have this TextView in your layout
            packaging = itemView.findViewById(R.id.packagingTextView); // Make sure to have this TextView in your layout
        }
    }
}
