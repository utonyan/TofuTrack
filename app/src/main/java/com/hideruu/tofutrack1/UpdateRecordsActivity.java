package com.hideruu.tofutrack1;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Import for FAB
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class UpdateRecordsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UpdateRecordAdapter adapter;
    private List<UpdateRecord> updateRecords;
    private List<UpdateRecord> filteredRecords; // To hold filtered records
    private FirebaseFirestore db;

    // Declare a FAB for the calendar
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_records);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.updateRecordsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize SearchView
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterRecords(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterRecords(newText);
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
                        // Initialize and set the adapter
                        adapter = new UpdateRecordAdapter(filteredRecords);
                        recyclerView.setAdapter(adapter);
                    } else {
                        // Handle errors here
                    }
                });
    }

    private void filterRecords(String query) {
        // Same filtering logic as before
    }

    // Method to show the date picker dialog
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            String selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
            filterRecordsByDate(selectedDate); // Call the method to filter records by date
        }, year, month, day);

        datePickerDialog.show();
    }

    // Method to filter records by selected date
    private void filterRecordsByDate(String selectedDate) {
        List<UpdateRecord> filteredList = new ArrayList<>();
        for (UpdateRecord record : updateRecords) {
            // Assuming timestamp is in "yyyy-MM-dd" format
            if (record.getTimestamp().startsWith(selectedDate)) {
                filteredList.add(record);
            }
        }
        filteredRecords = filteredList; // Update filtered records
        adapter.updateRecords(filteredRecords); // Update the adapter

        // Optional: Show a toast message to confirm filtering
        Toast.makeText(this, "Filtered by date: " + selectedDate, Toast.LENGTH_SHORT).show();
    }
}
