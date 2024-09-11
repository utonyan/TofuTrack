package com.hideruu.tofutrack1;

import android.graphics.Color;
import android.graphics.PorterDuff;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import android.util.Log;
import android.widget.ProgressBar;
import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_UPLOAD = 1;

    private FloatingActionButton fab;
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<DataClass> productList;
    private ProgressBar progressBar;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        fab = findViewById(R.id.fab);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        fab.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

        // Initialize product list and adapter
        productList = new ArrayList<>();
        adapter = new InventoryAdapter(productList);

        // Set layout manager to GridLayoutManager
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        // FloatingActionButton to open UploadActivity for adding a new product
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InventoryActivity.this, UploadActivity.class);
                startActivityForResult(intent, REQUEST_CODE_UPLOAD);
            }
        });

        // Fetch data from Firestore
        fetchData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_UPLOAD && resultCode == RESULT_OK) {
            // Reload data when coming back from UploadActivity
            fetchData();
        }
    }

    private void fetchData() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("products")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        productList.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            DataClass product = document.toObject(DataClass.class);
                            productList.add(product);
                        }
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Firestore", "Error getting documents", e);
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }
}
