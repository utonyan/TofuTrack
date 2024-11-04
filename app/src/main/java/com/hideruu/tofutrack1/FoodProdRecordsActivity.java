package com.hideruu.tofutrack1;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FoodProdRecordsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FoodProdRecordAdapter adapter;
    private List<ProductionRecord> productionRecordList;
    private List<ProductionRecord> filteredList; // To hold the filtered production records
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_prod_records);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.receiptRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Firestore and the lists
        db = FirebaseFirestore.getInstance();
        productionRecordList = new ArrayList<>();
        filteredList = new ArrayList<>();

        // Load production records from Firestore
        loadProductionRecords();

        // Set up the FAB to show the date picker
        findViewById(R.id.fab).setOnClickListener(view -> showDatePickerDialog());
    }

    private void loadProductionRecords() {
        db.collection("production_records") // Ensure collection name matches the save method
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        productionRecordList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ProductionRecord productionRecord = document.toObject(ProductionRecord.class);
                            productionRecordList.add(productionRecord);
                        }

                        sortProductionRecordsByDate(productionRecordList);
                        updateRecyclerView(productionRecordList);
                    } else {
                        Toast.makeText(FoodProdRecordsActivity.this, "Error loading production records.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sortProductionRecordsByDate(List<ProductionRecord> list) {
        Collections.sort(list, (r1, r2) -> r2.getTimestamp().compareTo(r1.getTimestamp()));
    }

    private void updateRecyclerView(List<ProductionRecord> list) {
        adapter = new FoodProdRecordAdapter(list);
        recyclerView.setAdapter(adapter);
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            String selectedDate = selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay; // Format the selected date
            filterProductionRecordsByDate(selectedDate);
        }, year, month, day);

        datePickerDialog.show();
    }

    private void filterProductionRecordsByDate(String selectedDate) {
        filteredList.clear();

        // Convert selectedDate String to Date object
        try {
            Date selectedDateObject = dateFormat.parse(selectedDate);
            for (ProductionRecord productionRecord : productionRecordList) {
                // Compare only the date parts
                if (isSameDate(productionRecord.getTimestamp(), selectedDateObject)) {
                    filteredList.add(productionRecord);
                }
            }

            // Sort the filtered list by timestamp in descending order
            sortProductionRecordsByDate(filteredList);

            if (filteredList.isEmpty()) {
                Toast.makeText(this, "No production records found for the selected date.", Toast.LENGTH_SHORT).show();
            } else {
                updateRecyclerView(filteredList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing the selected date.", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to compare only date parts of two Date objects
    private boolean isSameDate(Date date1, Date date2) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(date1).equals(dateFormat.format(date2));
    }
}
