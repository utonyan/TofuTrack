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
    // Date formatter for a readable date and time format (12-hour format)
    private SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a, MMM dd, yyyy", Locale.getDefault());

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

        holder.documentName.setText("ID:" + receipt.getDocumentName());
        holder.totalCost.setText(String.format("Total: " +"₱%.2f", receipt.getTotalCost()));
        holder.dateTime.setText("Date: " + dateFormat.format(receipt.getDateTime()));

        // Show payment and change
        holder.payment.setText(String.format("Payment: ₱%.2f", receipt.getPayment()));
        holder.change.setText(String.format("Change: ₱%.2f", receipt.getChange()));

        holder.productsLayout.removeAllViews();
        for (ReceiptItem item : receipt.getItems()) {
            double itemTotalCost = item.getProdCost() * item.getQuantity();
            TextView productTextView = new TextView(holder.itemView.getContext());
            productTextView.setText(String.format("%s (x%d) - ₱%.2f", item.getProdName(), item.getQuantity(), itemTotalCost));
            productTextView.setTextSize(14);
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
        TextView totalCost, dateTime, documentName, payment, change; // Added payment and change
        LinearLayout productsLayout;

        public ReceiptViewHolder(@NonNull View itemView) {
            super(itemView);
            documentName = itemView.findViewById(R.id.documentNameTextView);
            totalCost = itemView.findViewById(R.id.totalCostTextView);
            dateTime = itemView.findViewById(R.id.dateTimeTextView);
            payment = itemView.findViewById(R.id.paymentTextView); // Initialize payment
            change = itemView.findViewById(R.id.changeTextView); // Initialize change
            productsLayout = itemView.findViewById(R.id.productsLayout);
        }
    }

}
