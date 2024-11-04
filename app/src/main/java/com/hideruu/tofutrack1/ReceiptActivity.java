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

public class ReceiptActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ReceiptAdapter adapter;
    private List<Receipt> receiptList;
    private List<Receipt> filteredList; // To hold the filtered receipts
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.receiptRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Firestore and the lists
        db = FirebaseFirestore.getInstance();
        receiptList = new ArrayList<>();
        filteredList = new ArrayList<>();

        // Load receipts from Firestore
        loadReceipts();

        // Set up the FAB to show the date picker
        findViewById(R.id.fab).setOnClickListener(view -> showDatePickerDialog());
    }

    private void loadReceipts() {
        db.collection("receipts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        receiptList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Receipt receipt = document.toObject(Receipt.class);
                            receiptList.add(receipt);
                        }

                        sortReceiptsByDate(receiptList);
                        updateRecyclerView(receiptList);
                    } else {
                        Toast.makeText(ReceiptActivity.this, "Error loading receipts.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void sortReceiptsByDate(List<Receipt> list) {
        Collections.sort(list, (r1, r2) -> r2.getDateTime().compareTo(r1.getDateTime()));
    }

    private void updateRecyclerView(List<Receipt> list) {
        adapter = new ReceiptAdapter(list);
        recyclerView.setAdapter(adapter);
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            String selectedDate = selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay; // Format the selected date
            filterReceiptsByDate(selectedDate);
        }, year, month, day);

        datePickerDialog.show();
    }

    private void filterReceiptsByDate(String selectedDate) {
        filteredList.clear();

        // Convert selectedDate String to Date object
        try {
            Date selectedDateObject = dateFormat.parse(selectedDate);
            for (Receipt receipt : receiptList) {
                // Compare only the date parts
                if (isSameDate(receipt.getDateTime(), selectedDateObject)) {
                    filteredList.add(receipt);
                }
            }

            // Sort the filtered list by dateTime in descending order
            sortReceiptsByDate(filteredList);

            if (filteredList.isEmpty()) {
                Toast.makeText(this, "No receipts found for the selected date.", Toast.LENGTH_SHORT).show();
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
