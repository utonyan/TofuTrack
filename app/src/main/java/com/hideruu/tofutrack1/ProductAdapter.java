package com.hideruu.tofutrack1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import java.util.List;

public class ProductAdapter extends ArrayAdapter<DataClass> {

    public ProductAdapter(Context context, List<DataClass> products) {
        super(context, 0, products);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_product_spinner, parent, false);
        }

        DataClass product = getItem(position);

        TextView nameTextView = convertView.findViewById(R.id.productName);
        nameTextView.setText(product != null ? product.getProdName() : "N/A");

        return convertView;
    }
}
