package com.hideruu.tofutrack1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class UpdateRecordAdapter extends RecyclerView.Adapter<UpdateRecordAdapter.UpdateRecordViewHolder> {

    private List<UpdateRecord> updateRecords;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public UpdateRecordAdapter(List<UpdateRecord> updateRecords) {
        this.updateRecords = updateRecords;
        sortRecordsByMostRecent();
    }

    // Sort records by the most recent timestamp
    private void sortRecordsByMostRecent() {
        Collections.sort(updateRecords, new Comparator<UpdateRecord>() {
            @Override
            public int compare(UpdateRecord r1, UpdateRecord r2) {
                try {
                    Date date1 = dateFormat.parse(r1.getTimestamp());
                    Date date2 = dateFormat.parse(r2.getTimestamp());
                    return date2.compareTo(date1); // Sort in descending order
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        });
    }

    @NonNull
    @Override
    public UpdateRecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_update_record, parent, false);
        return new UpdateRecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UpdateRecordViewHolder holder, int position) {
        UpdateRecord record = updateRecords.get(position);
        holder.tvProdName.setText(record.getProdName());
        holder.tvProdQtyUnitType.setText(String.format("Quantity: %d %s", record.getProdQty(), record.getProdUnitType()));
        holder.tvProdCost.setText(String.format("Cost: %.2f", record.getProdCost()));
        holder.tvProdGroup.setText(record.getProdGroup());
        holder.tvTimestamp.setText(record.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return updateRecords.size();
    }

    public static class UpdateRecordViewHolder extends RecyclerView.ViewHolder {
        TextView tvProdName, tvProdQtyUnitType, tvProdCost, tvProdGroup, tvTimestamp;

        public UpdateRecordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProdName = itemView.findViewById(R.id.tvProdName);
            tvProdQtyUnitType = itemView.findViewById(R.id.tvProdQtyUnitType); // Updated reference
            tvProdCost = itemView.findViewById(R.id.tvProdCost);
            tvProdGroup = itemView.findViewById(R.id.tvProdGroup);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
    public void updateRecords(List<UpdateRecord> newRecords) {
        this.updateRecords = newRecords;
        notifyDataSetChanged(); // Notify the adapter to refresh the view
    }


}
