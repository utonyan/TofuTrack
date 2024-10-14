package com.hideruu.tofutrack1;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class UpdateRecordsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UpdateRecordAdapter adapter;
    private List<UpdateRecord> updateRecords;
    private List<UpdateRecord> filteredRecords; // To hold records filtered by the selected date
    private FirebaseFirestore db;

    // Declare a FAB for the calendar
    private FloatingActionButton fab;

    // Store the selected date
    private String selectedDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_records);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.updateRecordsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize SearchView
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setEnabled(false); // Disable the SearchView initially

        // Set a listener for the SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!selectedDate.isEmpty()) {
                    filterRecords(query); // Filter products within the selected date
                } else {
                    Toast.makeText(UpdateRecordsActivity.this, "Please select a date first.", Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!selectedDate.isEmpty()) {
                    filterRecords(newText); // Filter products as the text changes
                } else {
                    Toast.makeText(UpdateRecordsActivity.this, "Please select a date first.", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        // Initialize the FAB for date selection
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showDatePickerDialog());

        // Initialize Firestore and the list of records
        db = FirebaseFirestore.getInstance();
        updateRecords = new ArrayList<>();

        // Load data from Firestore
        loadUpdateRecords();
    }

    private void loadUpdateRecords() {
        db.collection("product_updates")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            UpdateRecord record = document.toObject(UpdateRecord.class);
                            updateRecords.add(record);
                        }
                        filteredRecords = new ArrayList<>(updateRecords); // Initialize filtered records
                        adapter = new UpdateRecordAdapter(filteredRecords);
                        recyclerView.setAdapter(adapter);
                    } else {
                        // Handle errors here
                        Toast.makeText(UpdateRecordsActivity.this, "Error loading records.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterRecords(String query) {
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a date first.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<UpdateRecord> filteredList = new ArrayList<>();
        for (UpdateRecord record : updateRecords) {
            // Check if the record's timestamp matches the selected date
            if (record.getTimestamp().startsWith(selectedDate)) {
                // Check if the query matches any product fields (case-insensitive)
                if (record.getProdName().toLowerCase().contains(query.toLowerCase()) ||
                        record.getProdGroup().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(record);
                }
            }
        }

        // Check if there are any records for the selected date
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No records found for the selected date.", Toast.LENGTH_SHORT).show();
        }

        // Sort the filtered list in descending order by timestamp
        filteredList.sort((record1, record2) -> record2.getTimestamp().compareTo(record1.getTimestamp())); // Latest first

        filteredRecords = filteredList; // Update filtered records
        adapter.updateRecords(filteredRecords); // Notify adapter about the filtered data
    }

    // Method to show the date picker dialog
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay); // Store the selected date
            // Enable the SearchView after a date is selected
            SearchView searchView = findViewById(R.id.searchView);
            searchView.setEnabled(true); // Enable the SearchView
            filterRecords(""); // Filter records for the selected date with no query initially
        }, year, month, day);

        datePickerDialog.show();
    }
}
