package com.hideruu.tofutrack1; // Use your project's package name

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.DatePickerDialog;

public class SalesChartActivity extends AppCompatActivity {

    private BarChart barChart;
    private FirebaseFirestore db;
    private Spinner productSpinner, timePeriodSpinner;
    private List<Receipt> receiptList = new ArrayList<>();
    private String selectedProductName = "";
    private String selectedTimePeriod = "Daily";
    private Calendar selectedDate = Calendar.getInstance(); // For storing the selected date

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_chart);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Find the views
        barChart = findViewById(R.id.barChart);
        productSpinner = findViewById(R.id.productSpinner);
        timePeriodSpinner = findViewById(R.id.timePeriodSpinner);
        FloatingActionButton fab = findViewById(R.id.fab); // Make sure to add the FAB in your XML layout

        // Enable scaling and pinch zoom
        barChart.setPinchZoom(true);
        barChart.setScaleEnabled(true);
        barChart.setDoubleTapToZoomEnabled(true);

        // Customize the Y-axis
        YAxis yAxis = barChart.getAxisLeft();
        yAxis.setAxisMaximum(100f); // Set the maximum value for the Y-axis
        yAxis.setAxisLineWidth(2f);
        yAxis.setAxisLineColor(Color.BLACK);
        yAxis.setLabelCount(10); // Adjust label count for Y-axis

        // Hide the right Y-axis
        barChart.getAxisRight().setDrawLabels(false);

        // Load sales data from Firestore
        loadSalesData();

        // Set up the time period spinner
        ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(this,
                R.array.time_periods, android.R.layout.simple_spinner_item);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timePeriodSpinner.setAdapter(timeAdapter);
        timePeriodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTimePeriod = (String) parent.getItemAtPosition(position);
                updateChart();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Set up the FloatingActionButton click listener
        fab.setOnClickListener(view -> showDatePicker());
    }

    private void loadSalesData() {
        db.collection("receipts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Receipt receipt = document.toObject(Receipt.class);
                            receiptList.add(receipt);
                        }

                        // Populate product names in the spinner
                        populateProductSpinner();
                        // Process sales data for the chart
                        updateChart();
                    }
                });
    }

    private void populateProductSpinner() {
        List<String> productNames = new ArrayList<>();
        Map<String, Boolean> productMap = new HashMap<>();

        for (Receipt receipt : receiptList) {
            for (ReceiptItem item : receipt.getItems()) {
                String productName = item.getProdName();
                if (!productMap.containsKey(productName)) {
                    productNames.add(productName);
                    productMap.put(productName, true);
                }
            }
        }

        ArrayAdapter<String> productAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, productNames);
        productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        productSpinner.setAdapter(productAdapter);
        productSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedProductName = (String) parent.getItemAtPosition(position);
                updateChart();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateChart() {
        if (selectedProductName.isEmpty()) return;

        // Initialize sales counts
        int[] dailySales = new int[7]; // For days of the week
        int[] weeklySales = new int[5]; // For weeks in a month (assumes maximum 4 weeks)
        int[] monthlySales = new int[12]; // For months in a year
        int[] yearlySales = new int[10]; // For years

        // Set all weekly sales to zero initially
        for (int i = 0; i < weeklySales.length; i++) {
            weeklySales[i] = 0; // Explicitly set to zero
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        int currentYear = selectedDate.get(Calendar.YEAR);
        int currentMonth = selectedDate.get(Calendar.MONTH);
        int currentWeek = selectedDate.get(Calendar.WEEK_OF_MONTH); // Get the current week of the month

        for (Receipt receipt : receiptList) {
            Calendar receiptCalendar = Calendar.getInstance();
            receiptCalendar.setTime(receipt.getDateTime());

            for (ReceiptItem item : receipt.getItems()) {
                if (item.getProdName().equals(selectedProductName)) {
                    // Daily Sales (for the current week)
                    if (receiptCalendar.get(Calendar.YEAR) == currentYear &&
                            receiptCalendar.get(Calendar.WEEK_OF_MONTH) == currentWeek &&
                            receiptCalendar.get(Calendar.MONTH) == currentMonth) {
                        int dayOfWeek = receiptCalendar.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Sunday, 6 = Saturday
                        dailySales[dayOfWeek] += item.getQuantity();
                    }

                    // Weekly Sales - only fill for weeks of the current month
                    if (receiptCalendar.get(Calendar.YEAR) == currentYear &&
                            receiptCalendar.get(Calendar.MONTH) == currentMonth) {
                        int receiptWeek = receiptCalendar.get(Calendar.WEEK_OF_MONTH);
                        if (receiptWeek <= 4) { // Ensure we're within the maximum 4 weeks
                            weeklySales[receiptWeek - 1] += item.getQuantity(); // -1 for zero-indexing
                        }
                    }

                    // Monthly Sales
                    if (receiptCalendar.get(Calendar.YEAR) == currentYear) {
                        monthlySales[receiptCalendar.get(Calendar.MONTH)] += item.getQuantity();
                    }

                    // Yearly Sales
                    int startYear = currentYear - yearlySales.length + 1;
                    int receiptYear = receiptCalendar.get(Calendar.YEAR);
                    if (receiptYear >= startYear && receiptYear <= currentYear) {
                        int index = currentYear - receiptYear;
                        yearlySales[index] += item.getQuantity();
                    }
                }
            }
        }

        // Create bar chart entries based on the selected time period
        ArrayList<BarEntry> entries = new ArrayList<>();
        float maxValue = 0f; // To find the maximum value for Y-axis

        switch (selectedTimePeriod) {
            case "Daily":
                for (int i = 0; i < 7; i++) { // Only fill for days of the week
                    entries.add(new BarEntry(i, dailySales[i]));
                    if (dailySales[i] > maxValue) {
                        maxValue = dailySales[i];
                    }
                }
                break;
            case "Weekly":
                // Show only the weeks that have sales data
                for (int i = 0; i < currentWeek; i++) {
                    entries.add(new BarEntry(i, weeklySales[i]));
                    if (weeklySales[i] > maxValue) {
                        maxValue = weeklySales[i];
                    }
                }
                break;
            case "Monthly":
                for (int i = 0; i < monthlySales.length; i++) {
                    entries.add(new BarEntry(i, monthlySales[i]));
                    if (monthlySales[i] > maxValue) {
                        maxValue = monthlySales[i];
                    }
                }
                break;
            case "Yearly":
                for (int i = yearlySales.length - 1; i >= 0; i--) {
                    entries.add(new BarEntry(yearlySales.length - 1 - i, yearlySales[i]));
                    if (yearlySales[i] > maxValue) {
                        maxValue = yearlySales[i];
                    }
                }
                break;
        }

        // Set up the bar data set and labels
        BarDataSet dataSet = new BarDataSet(entries, selectedProductName + " Sold Items");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(getXAxisLabels()));
        barChart.getAxisLeft().setAxisMaximum(maxValue + 10f); // Add some padding to the max value
        barChart.invalidate(); // Refresh the chart
    }


    private String[] getXAxisLabels() {
        switch (selectedTimePeriod) {
            case "Daily":
                return new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            case "Weekly":
                return new String[]{"Week 1", "Week 2", "Week 3", "Week 4"};
            case "Monthly":
                return new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            case "Yearly":
                return new String[]{"Year 1", "Year 2", "Year 3", "Year 4", "Year 5", "Year 6", "Year 7", "Year 8", "Year 9", "Year 10"};
        }
        return new String[]{};
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    updateChart(); // Update the chart after selecting a date
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }
}
