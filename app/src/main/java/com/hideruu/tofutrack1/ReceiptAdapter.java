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

public class ReceiptAdapter extends RecyclerView.Adapter<ReceiptAdapter.ReceiptViewHolder> {

    private List<Receipt> receiptList;
    // Date formatter for a readable date and time format (24-hour format)
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public ReceiptAdapter(List<Receipt> receiptList) {
        this.receiptList = receiptList;
    }

    @NonNull
    @Override
    public ReceiptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receipt, parent, false);
        return new ReceiptViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReceiptViewHolder holder, int position) {
        Receipt receipt = receiptList.get(position);

        // Set the total cost
        holder.totalCost.setText(String.format("₱%.2f", receipt.getTotalCost()));

        // Format the date to include time in 24-hour format
        holder.dateTime.setText(dateFormat.format(receipt.getDateTime()));

        // Clear previous product views
        holder.productsLayout.removeAllViews();

        // Add product details to the layout
        for (ReceiptItem item : receipt.getItems()) {
            // Calculate total cost for each product
            double itemTotalCost = item.getProdCost() * item.getQuantity();

            // Create a TextView for each product
            TextView productTextView = new TextView(holder.itemView.getContext());
            productTextView.setText(String.format("%s (x%d) - ₱%.2f", item.getProdName(), item.getQuantity(), itemTotalCost));
            productTextView.setTextSize(14); // Set text size as needed

            // Add the TextView to the products layout
            holder.productsLayout.addView(productTextView);
        }
    }

    @Override
    public int getItemCount() {
        return receiptList.size();
    }

    public void updateReceipts(List<Receipt> updatedList) {
        this.receiptList = updatedList;
        notifyDataSetChanged();
    }

    static class ReceiptViewHolder extends RecyclerView.ViewHolder {
        TextView totalCost, dateTime;
        LinearLayout productsLayout; // Add this line

        public ReceiptViewHolder(@NonNull View itemView) {
            super(itemView);
            totalCost = itemView.findViewById(R.id.totalCostTextView);
            dateTime = itemView.findViewById(R.id.dateTimeTextView);
            productsLayout = itemView.findViewById(R.id.productsLayout); // Initialize this
        }
    }
}
