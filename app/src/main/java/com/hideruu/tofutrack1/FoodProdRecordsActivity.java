package com.hideruu.tofutrack1;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FoodProdRecordsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FoodProdRecordAdapter adapter;
    private List<ProductionRecord> foodProdRecordList; // Use ProductionRecord here
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_prod_records);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.foodProdRecordRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Firestore and the list
        db = FirebaseFirestore.getInstance();
        foodProdRecordList = new ArrayList<>();

        // Load food production records from Firestore
        loadFoodProdRecords();
    }

    private void loadFoodProdRecords() {
        db.collection("foodProdRecords")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        foodProdRecordList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ProductionRecord record = document.toObject(ProductionRecord.class); // Use ProductionRecord here
                            foodProdRecordList.add(record);
                        }

                        sortFoodProdRecordsByDate(foodProdRecordList);
                        updateRecyclerView(foodProdRecordList);
                    } else {
                        Toast.makeText(FoodProdRecordsActivity.this, "Error loading food production records.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sortFoodProdRecordsByDate(List<ProductionRecord> list) {
        Collections.sort(list, (r1, r2) -> r2.getTimestamp().compareTo(r1.getTimestamp()));
    }

    private void updateRecyclerView(List<ProductionRecord> list) { // Correct the parameter type here
        adapter = new FoodProdRecordAdapter(list);
        recyclerView.setAdapter(adapter);
    }
}
