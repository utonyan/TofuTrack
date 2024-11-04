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

public class FoodProdRecordAdapter extends RecyclerView.Adapter<FoodProdRecordAdapter.FoodProdRecordViewHolder> {

    private List<ProductionRecord> foodProdRecordList; // Updated to ProductionRecord
    private SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a, MMM dd, yyyy", Locale.getDefault());

    public FoodProdRecordAdapter(List<ProductionRecord> foodProdRecordList) { // Updated constructor
        this.foodProdRecordList = foodProdRecordList;
    }

    @NonNull
    @Override
    public FoodProdRecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food_prod_record, parent, false);
        return new FoodProdRecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodProdRecordViewHolder holder, int position) {
        ProductionRecord record = foodProdRecordList.get(position); // Updated to ProductionRecord

        // Set the product name and quantity produced
        holder.recordId.setText("Product: " + record.getProductName());
        holder.totalProduced.setText(String.format("Total Produced: %d", record.getQuantityProduced()));
        holder.dateTime.setText("Date: " + dateFormat.format(record.getTimestamp())); // Corrected to use getTimestamp()

        // Clear existing views and add new views for raw materials and packaging
        holder.productsLayout.removeAllViews();

        // Display raw materials
        for (Map.Entry<String, Integer> entry : record.getRawMaterials().entrySet()) {
            TextView productTextView = new TextView(holder.itemView.getContext());
            productTextView.setText(String.format("Raw Material: %s (x%d)", entry.getKey(), entry.getValue()));
            productTextView.setTextSize(14);
            holder.productsLayout.addView(productTextView);
        }

        // Display packaging
        for (Map.Entry<String, Integer> entry : record.getPackaging().entrySet()) {
            TextView packagingTextView = new TextView(holder.itemView.getContext());
            packagingTextView.setText(String.format("Packaging: %s (x%d)", entry.getKey(), entry.getValue()));
            packagingTextView.setTextSize(14);
            holder.productsLayout.addView(packagingTextView);
        }
    }


    @Override
    public int getItemCount() {
        return foodProdRecordList.size();
    }

    public void updateFoodProdRecords(List<ProductionRecord> updatedList) { // Updated method signature
        this.foodProdRecordList = updatedList;
        notifyDataSetChanged();
    }

    static class FoodProdRecordViewHolder extends RecyclerView.ViewHolder {
        TextView totalProduced, dateTime, recordId;
        LinearLayout productsLayout;

        public FoodProdRecordViewHolder(@NonNull View itemView) {
            super(itemView);
            recordId = itemView.findViewById(R.id.recordIdTextView);
            totalProduced = itemView.findViewById(R.id.totalProducedTextView);
            dateTime = itemView.findViewById(R.id.dateTimeTextView);
            productsLayout = itemView.findViewById(R.id.productsLayout);
        }
    }
}
