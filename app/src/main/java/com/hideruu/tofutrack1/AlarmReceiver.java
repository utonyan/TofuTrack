package com.hideruu.tofutrack1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import com.google.firebase.firestore.FirebaseFirestore;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    private static final String CHANNEL_ID = "production_alarm_channel";

    private FirebaseFirestore db;

    @Override
    public void onReceive(Context context, Intent intent) {
        db = FirebaseFirestore.getInstance();

        String productName = intent.getStringExtra("prodName");
        int quantityToSubtract = intent.getIntExtra("quantityToSubtract", 0);

        db.collection("products")
                .whereEqualTo("prodName", productName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String documentId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        int currentQty = queryDocumentSnapshots.getDocuments().get(0).getLong("prodQty").intValue();
                        double productCost = queryDocumentSnapshots.getDocuments().get(0).getDouble("prodCost");

                        if (currentQty >= quantityToSubtract) {
                            int newQty = currentQty - quantityToSubtract;
                            double newTotalPrice = newQty * productCost;

                            db.collection("products").document(documentId)
                                    .update("prodQty", newQty, "prodTotalPrice", newTotalPrice)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(context, "Quantity updated: " + newQty, Toast.LENGTH_SHORT).show();
                                        sendNotification(context, productName, quantityToSubtract, newQty);
                                        clearAlarmState(context); // Clear the saved alarm state after updating
                                    })
                                    .addOnFailureListener(e -> Log.e(TAG, "Error updating quantity: " + e.getMessage()));
                        } else {
                            Toast.makeText(context, "Insufficient quantity to subtract.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "Product not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching product: " + e.getMessage()));
    }

    private void sendNotification(Context context, String productName, int quantityToSubtract, int newQty) {
        Intent notificationIntent = new Intent(context, ProductionDetailActivity.class);
        notificationIntent.putExtra("prodName", productName);
        notificationIntent.putExtra("quantityToSubtract", quantityToSubtract);
        notificationIntent.putExtra("newQty", newQty);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.kitachan)
                .setContentTitle("Production Update")
                .setContentText("Subtracted " + quantityToSubtract + " from " + productName + ". New Quantity: " + newQty)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        createNotificationChannel(context);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1001, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelName = "Production Alarm Notifications";
            String channelDescription = "Notifications for Production Alarm";

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(channelDescription);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void clearAlarmState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(ProductionDetailActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(ProductionDetailActivity.KEY_ALARM_SET, false);
        editor.putString(ProductionDetailActivity.KEY_ALARM_TIME, null);
        editor.putInt(ProductionDetailActivity.KEY_ALARM_QUANTITY, 0);
        editor.apply();
        Log.d(TAG, "Alarm state cleared.");
    }
}
