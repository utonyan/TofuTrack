package com.hideruu.tofutrack1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Inventory button setup
        CardView inventoryButton = findViewById(R.id.inventory_button);
        inventoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open InventoryActivity
                startActivity(new Intent(DashboardActivity.this, InventoryActivity.class));
            }
        });

        // Records button setup
        CardView recordsButton = findViewById(R.id.Records_button);
        recordsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open UpdateRecords activity
                startActivity(new Intent(DashboardActivity.this, UpdateRecordsActivity.class));
            }
        });

        // Production button setup
        CardView productionButton = findViewById(R.id.Production_button);
        productionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open ProductionActivity
                startActivity(new Intent(DashboardActivity.this, ProductionActivity.class));
            }
        });
        // POS button setup
        CardView posButton = findViewById(R.id.POS_button);
        posButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open ProductionActivity
                startActivity(new Intent(DashboardActivity.this, posActivity.class));
            }
        });
        // Receipt button setup
        CardView receiptButton = findViewById(R.id.Receipts_button);
        receiptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open ProductionActivity
                startActivity(new Intent(DashboardActivity.this, ReceiptActivity.class));
            }
        });
        // Receipt button setup
        CardView DashboardButton = findViewById(R.id.Dashboard_button);
        DashboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open ProductionActivity
                startActivity(new Intent(DashboardActivity.this, SalesChartActivity.class));
            }
        });
        // Receipt button setup
        CardView FoodProdButton = findViewById(R.id.FoodProd_button);
        FoodProdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open ProductionActivity
                startActivity(new Intent(DashboardActivity.this, FoodProdActivity.class));
            }
        });
        // Receipt button setup
        CardView FoodProdRecordButton = findViewById(R.id.FoodProdRecord_button);
        FoodProdRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open ProductionActivity
                startActivity(new Intent(DashboardActivity.this, FoodProdRecordsActivity.class));
            }
        });
    }
}
