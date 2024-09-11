package com.hideruu.tofutrack1;

import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.text.DateFormat;
import java.util.Calendar;

public class UploadActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    ImageView uploadImage;
    Button saveButton;
    EditText uploadTopic, uploadDesc, uploadLang;
    String imageURL;
    Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState){
       super.onCreate(savedInstanceState);
       setContentView(R.layout.activity_upload);

        db = FirebaseFirestore.getInstance();

        uploadImage = findViewById(R.id.uploadImage);
        uploadDesc = findViewById(R.id.uploadDesc);
        uploadTopic = findViewById(R.id.uploadTopic);
        uploadLang = findViewById(R.id.uploadLang);
        saveButton = findViewById(R.id.saveButton);

        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK){
                            Intent data = result.getData();
                            uri = data.getData();
                            uploadImage.setImageURI(uri);
                        } else {
                            Toast.makeText(UploadActivity.this, "No Image Selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoPicker = new Intent(Intent.ACTION_PICK);
                photoPicker.setType("image/*");
                activityResultLauncher.launch(photoPicker);
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveData();
            }
        });
    }

    public void saveData() {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Android Images")
                .child(uri.getLastPathSegment());

        AlertDialog.Builder builder = new AlertDialog.Builder(UploadActivity.this);
        builder.setCancelable(false);
        builder.setView(R.layout.progress_layout);
        AlertDialog dialog = builder.create();
        dialog.show();

        storageReference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isComplete());
                Uri urlImage = uriTask.getResult();
                imageURL = urlImage.toString();

                // Call addData to add Firestore data
                addData();

                // Dismiss the dialog
                dialog.dismiss();

                // Show a toast indicating success
                Toast.makeText(UploadActivity.this, "Saved Successfully", Toast.LENGTH_SHORT).show();

                // Finish the activity or close the current window
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Dismiss the dialog
                dialog.dismiss();

                // Optionally show a failure message
                Toast.makeText(UploadActivity.this, "Failed to save data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void addData() {
        // Retrieve text from EditText fields
        String title = uploadTopic.getText().toString();
        String desc = uploadDesc.getText().toString();
        String lang = uploadLang.getText().toString();

        // Ensure fields are not empty
        if (title.isEmpty() || desc.isEmpty() || lang.isEmpty() || imageURL == null) {
            Toast.makeText(UploadActivity.this, "Please fill all fields and select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create an instance of DataClass with the retrieved text and the image URL
        DataClass data = new DataClass(title, desc, lang, imageURL);

        // Add a new document with a generated ID
        db.collection("data")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "DocumentSnapshot added with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error adding document", e);
                });
    }


}