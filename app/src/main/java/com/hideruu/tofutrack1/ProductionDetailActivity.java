package com.hideruu.tofutrack1;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;

public class ProductionDetailActivity extends AppCompatActivity {

    private static final String EXTRA_PRODUCT_ID = "productId";
    private static final String EXTRA_PROD_NAME = "prodName";
    private static final String EXTRA_PROD_DESC = "prodDesc";
    private static final String EXTRA_PROD_GROUP = "prodGroup";
    private static final String EXTRA_PROD_QTY = "prodQty";
    private static final String EXTRA_PROD_COST = "prodCost";
    private static final String EXTRA_PROD_TOTAL_PRICE = "prodTotalPrice";

    public static final String PREFS_NAME = "ProductionPrefs";
    public static final String KEY_ALARM_SET = "alarm_set";
    public static final String KEY_ALARM_TIME = "alarm_time";
    public static final String KEY_ALARM_QUANTITY = "alarm_quantity";

    private TextView alarmTimeDisplay, SoyTimer, prodName, prodDesc, prodGroup, prodQty, prodCost, prodTotalPrice;
    private Button setAlarmButton, cancelAlarmButton;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    private FirebaseFirestore db;
    private String productId;
    private int currentProdQty;
    private double productCost;
    private int selectedHour = 8;
    private int selectedMinute = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_production_detail);

        initializeViews();
        initializeFirestore();
        getProductDetailsFromIntent();
        checkForExistingAlarm(); // Check if there is an existing alarm

        setAlarmButton.setOnClickListener(v -> showQuantityInputDialog());
        cancelAlarmButton.setOnClickListener(v -> cancelAlarm());
    }

    private void initializeViews() {
        alarmTimeDisplay = findViewById(R.id.alarmTimeDisplay);
        SoyTimer = findViewById(R.id.SoyTimer);
        setAlarmButton = findViewById(R.id.setAlarmButton);
        cancelAlarmButton = findViewById(R.id.cancelAlarmButton);
        prodName = findViewById(R.id.prodName);
        prodDesc = findViewById(R.id.prodDesc);
        prodGroup = findViewById(R.id.prodGroup);
        prodQty = findViewById(R.id.prodQty);
        prodCost = findViewById(R.id.prodCost);
        prodTotalPrice = findViewById(R.id.prodTotalPrice);
    }

    private void initializeFirestore() {
        db = FirebaseFirestore.getInstance();
    }

    private void getProductDetailsFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            productId = intent.getStringExtra(EXTRA_PRODUCT_ID);
            prodName.setText(intent.getStringExtra(EXTRA_PROD_NAME));
            prodDesc.setText(intent.getStringExtra(EXTRA_PROD_DESC));
            prodGroup.setText(intent.getStringExtra(EXTRA_PROD_GROUP));
            currentProdQty = intent.getIntExtra(EXTRA_PROD_QTY, 0);
            productCost = intent.getDoubleExtra(EXTRA_PROD_COST, 0.0);
            prodQty.setText("Current Quantity: " + currentProdQty);
            prodCost.setText("Cost: ₱" + String.format("%.2f", productCost));
            prodTotalPrice.setText("Total Price: ₱" + String.format("%.2f", intent.getDoubleExtra(EXTRA_PROD_TOTAL_PRICE, 0.0)));
        }
    }

    private void checkForExistingAlarm() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isAlarmSet = prefs.getBoolean(KEY_ALARM_SET, false);
        if (isAlarmSet) {
            String alarmTime = prefs.getString(KEY_ALARM_TIME, "Not Set");
            int alarmQuantity = prefs.getInt(KEY_ALARM_QUANTITY, 0);
            //alarmTimeDisplay.setText("Current Production: " + alarmTime + " for quantity: " + alarmQuantity);
            alarmTimeDisplay.setText("Current Production for quantity: " + alarmQuantity);
            SoyTimer.setText(alarmTime);
        } else {
            alarmTimeDisplay.setText("No active production set");
            SoyTimer.setText("--:--");
        }
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void showQuantityInputDialog() {
        if (!isInternetAvailable()) {
            Toast.makeText(this, "No internet connection! Cannot set Production.", Toast.LENGTH_SHORT).show();
            return; // Exit if no internet
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Quantity to Produce");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            int quantityToSubtract;
            try {
                quantityToSubtract = Integer.parseInt(input.getText().toString());
                if (quantityToSubtract > currentProdQty) {
                    Toast.makeText(this, "Cannot subtract more than current quantity!", Toast.LENGTH_SHORT).show();
                } else {
                    showTimePickerDialog(quantityToSubtract);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showTimePickerDialog(int quantityToSubtract) {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            selectedHour = hourOfDay;
            selectedMinute = minute;

            // Check if the selected time is in the past
            Calendar selectedTime = Calendar.getInstance();
            selectedTime.set(Calendar.HOUR_OF_DAY, selectedHour);
            selectedTime.set(Calendar.MINUTE, selectedMinute);
            selectedTime.set(Calendar.SECOND, 0);

            if (selectedTime.before(Calendar.getInstance())) {
                Toast.makeText(this, "Selected time is in the past! Please choose a future time.", Toast.LENGTH_SHORT).show();
            } else {
                setAlarm(quantityToSubtract);
            }
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);

        timePickerDialog.setTitle("Select Alarm Time");
        timePickerDialog.show();
    }

    private void setAlarm(int quantityToSubtract) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
        calendar.set(Calendar.MINUTE, selectedMinute);
        calendar.set(Calendar.SECOND, 0);

        // No need to check again here, as the check has been moved to the showTimePickerDialog method

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Check if the app can schedule exact alarms
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                scheduleAlarm(quantityToSubtract, calendar);
            } else {
                Toast.makeText(this, "Please allow this app to schedule exact alarms in settings.", Toast.LENGTH_SHORT).show();
            }
        } else {
            scheduleAlarm(quantityToSubtract, calendar);
        }
    }


    private void scheduleAlarm(int quantityToSubtract, Calendar calendar) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra(EXTRA_PRODUCT_ID, productId);
        intent.putExtra("quantityToSubtract", quantityToSubtract);
        intent.putExtra(EXTRA_PROD_NAME, prodName.getText().toString());
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

        alarmTimeDisplay.setText("Production confirmed");
        SoyTimer.setText(selectedHour + ":" + String.format("%02d", selectedMinute));
        Toast.makeText(this, "Production set!", Toast.LENGTH_SHORT).show();

        // Save alarm state
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_ALARM_SET, true);
        editor.putString(KEY_ALARM_TIME, selectedHour + ":" + String.format("%02d", selectedMinute));
        editor.putInt(KEY_ALARM_QUANTITY, quantityToSubtract);
        editor.apply();

        // Update product quantity in Firestore
        updateProductQuantity(quantityToSubtract);
    }

    private void cancelAlarm() {
        if (!isInternetAvailable()) {
            Toast.makeText(this, "No internet connection! Cannot cancel Production.", Toast.LENGTH_SHORT).show();
            return; // Exit if no internet
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isAlarmSet = prefs.getBoolean(KEY_ALARM_SET, false);

        if (isAlarmSet) {
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                alarmTimeDisplay.setText("Production canceled");
                SoyTimer.setText("--:--");

                // Clear the saved alarm state
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(KEY_ALARM_SET, false);
                editor.putString(KEY_ALARM_TIME, null);
                editor.putInt(KEY_ALARM_QUANTITY, 0);
                editor.apply();

                Toast.makeText(this, "Production canceled!", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("ProductionDetailActivity", "PendingIntent is null, unable to cancel alarm.");
                Toast.makeText(this, "Select a new Production before pressing cancel again", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Show a Toast prompting the user to select a new alarm instead
            Toast.makeText(this, "No active production set. Please select a new production.", Toast.LENGTH_SHORT).show();
        }
    }


    private void updateProductQuantity(int quantityToSubtract) {
        if (currentProdQty <= 0 || quantityToSubtract <= 0) {
            Toast.makeText(this, "Invalid quantity to Produce!", Toast.LENGTH_SHORT).show();
            return;
        }

        int newQuantity = currentProdQty - quantityToSubtract;
        double newTotalPrice = newQuantity * productCost;

        if (productId == null || productId.isEmpty()) {
            return;
        }

        DocumentReference productRef = db.collection("products").document(productId);
        productRef.update("prodQty", newQuantity, "prodTotalPrice", newTotalPrice)
                .addOnSuccessListener(aVoid -> {
                    currentProdQty = newQuantity; // Update current quantity
                    prodQty.setText("Current Quantity: " + currentProdQty);
                    prodTotalPrice.setText("Total Price: ₱" + String.format("%.2f", newTotalPrice));
                    Toast.makeText(this, "Product quantity updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Log.w("ProductionDetailActivity", "Error updating product quantity", e));
    }
}
