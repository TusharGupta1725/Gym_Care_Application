package com.example.gc_gymcare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editGymName, editOwnerName, editPhoneNumber;
    private ImageView imageOwnerPhoto;

    // This is the name of the file saved on the phone
    private static final String PREF_NAME = "GymProfileData";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Link Java to XML
        editGymName = findViewById(R.id.editGymName);
        editOwnerName = findViewById(R.id.editOwnerName);
        editPhoneNumber = findViewById(R.id.editPhoneNumber);
        imageOwnerPhoto = findViewById(R.id.imageOwnerPhoto);
        Button buttonSaveProfile = findViewById(R.id.buttonSaveProfile);

        // 1. Load saved data automatically when screen opens
        loadSavedData();

        // 2. Handle Photo Click (Placeholder for now)
        imageOwnerPhoto.setOnClickListener(v -> Toast.makeText(LoginActivity.this, "Opening Gallery...", Toast.LENGTH_SHORT).show());

        // 3. Handle Save Button Click
        buttonSaveProfile.setOnClickListener(v -> saveDataToPhone());
    }

    private void loadSavedData() {
        // Open the secure file on the phone
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Retrieve data (the second parameter is what shows if no data exists yet)
        String savedGymName = sharedPreferences.getString("GYM_NAME", "");
        String savedOwnerName = sharedPreferences.getString("OWNER_NAME", "");
        String savedPhone = sharedPreferences.getString("PHONE_NUMBER", "");

        // Put the saved data into the text boxes
        editGymName.setText(savedGymName);
        editOwnerName.setText(savedOwnerName);
        editPhoneNumber.setText(savedPhone);
    }

    private void saveDataToPhone() {
        // Open the secure file and prepare to edit it
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save what the user typed
        editor.putString("GYM_NAME", editGymName.getText().toString());
        editor.putString("OWNER_NAME", editOwnerName.getText().toString());
        editor.putString("PHONE_NUMBER", editPhoneNumber.getText().toString());

        // Apply saves it permanently
        editor.apply();

        Toast.makeText(this, "Profile Saved Securely!", Toast.LENGTH_SHORT).show();

        // After saving, go to Logo Setup Screen
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}