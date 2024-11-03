package com.hideruu.tofutrack1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class MaterialAdapter extends RecyclerView.Adapter<MaterialAdapter.ViewHolder> {
    private Context context;
    private List<DataClass> materialList;
    private RecyclerView recyclerView; // Reference to the RecyclerView

    public MaterialAdapter(Context context, List<DataClass> materialList, RecyclerView recyclerView) {
        this.context = context;
        this.materialList = materialList;
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_material, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataClass material = materialList.get(position);
        holder.nameTextView.setText(material.getProdName());
        holder.currentQuantityTextView.setText(String.valueOf(material.getProdQty()));
        holder.quantityEditText.setHint("Enter quantity");
        holder.quantityEditText.setText(""); // Reset quantity field
    }

    @Override
    public int getItemCount() {
        return materialList.size();
    }

    public int getSelectedQuantity(int position) {
        ViewHolder holder = (ViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
        if (holder != null) {
            String input = holder.quantityEditText.getText().toString();
            return input.isEmpty() ? 0 : Integer.parseInt(input);
        }
        return 0; // Return 0 if holder is null
    }

    public boolean hasSufficientQuantities() {
        for (int i = 0; i < materialList.size(); i++) {
            if (materialList.get(i).getProdQty() < getSelectedQuantity(i)) {
                return false;
            }
        }
        return true;
    }

    // New method to check if any items are selected
    public boolean hasSelectedItems() {
        for (int i = 0; i < materialList.size(); i++) {
            if (getSelectedQuantity(i) > 0) {
                return true; // At least one item is selected
            }
        }
        return false; // No items selected
    }

    public void deductQuantitiesInFirestore(FirebaseFirestore db) {
        for (int i = 0; i < materialList.size(); i++) {
            DataClass material = materialList.get(i);
            int requiredQuantity = getSelectedQuantity(i);
            int newQuantity = material.getProdQty() - requiredQuantity;

            if (newQuantity < 0) {
                Toast.makeText(context, "Insufficient quantity for " + material.getProdName(), Toast.LENGTH_SHORT).show();
                continue; // Skip to the next material
            }

            db.collection("products")
                    .whereEqualTo("prodName", material.getProdName())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            String documentId = queryDocumentSnapshots.getDocuments().get(0).getId();
                            db.collection("products").document(documentId)
                                    .update("prodQty", newQuantity)
                                    //.addOnSuccessListener(aVoid -> Toast.makeText(context, "Quantity updated for " + material.getProdName(), Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to update quantity for " + material.getProdName(), Toast.LENGTH_SHORT).show());
                        }
                    });
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, currentQuantityTextView;
        EditText quantityEditText;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            currentQuantityTextView = itemView.findViewById(R.id.currentQuantityTextView);
            quantityEditText = itemView.findViewById(R.id.quantityEditText);
        }
    }
}
