package com.hideruu.tofutrack1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    private TextView prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice;
    private ImageView prodImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Initialize views
        prodName = findViewById(R.id.prodName);
        prodDesc = findViewById(R.id.prodDesc);
        prodGroup = findViewById(R.id.prodGroup);
        prodQty = findViewById(R.id.prodQty);
        prodCost = findViewById(R.id.prodCost);
        prodTotalPrice = findViewById(R.id.prodTotalPrice);
        prodImage = findViewById(R.id.prodImage);

        // Get the data passed from the adapter
        Intent intent = getIntent();

        if (intent != null) {
            String name = intent.getStringExtra("prodName");
            String desc = intent.getStringExtra("prodDesc");
            String group = intent.getStringExtra("prodGroup");
            int qty = intent.getIntExtra("prodQty", 0);
            double cost = intent.getDoubleExtra("prodCost", 0.0);
            double totalPrice = intent.getDoubleExtra("prodTotalPrice", 0.0);
            String imageUrl = intent.getStringExtra("prodImage");

            // Set data to views, handling potential null values
            prodName.setText(name != null ? name : "No Name");
            prodDesc.setText("Description: " + (desc != null ? desc : "No Description"));
            prodGroup.setText("Group: " + (group != null ? group : "No Group"));
            prodQty.setText(String.format(Locale.getDefault(), "Quantity: %d", qty));
            prodCost.setText(String.format(Locale.getDefault(), "Cost per unit: ₱%.2f", cost));
            prodTotalPrice.setText(String.format(Locale.getDefault(), "Total Price: ₱%.2f", totalPrice));

            // Load product image using Glide, handling potential missing image URL
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this).load(imageUrl).into(prodImage);
            } else {
                prodImage.setImageResource(R.drawable.kitalogo);  // Placeholder image if no URL
            }
        }
    }
}
