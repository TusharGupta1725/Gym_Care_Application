package com.example.gc_gymcare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class LogoSetupActivity extends AppCompatActivity {

    private ImageView imgGymLogo;
    private ActivityResultLauncher<String[]> mGetContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo_setup);

        imgGymLogo = findViewById(R.id.imgGymLogo);
        Button btnSelectLogo = findViewById(R.id.btnSelectLogo);
        Button btnSkip = findViewById(R.id.btnSkip);

        // Initialize Image Picker (using OpenDocument for persistable permissions)
        mGetContent = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                // Grant persistent access to the URI so it loads after restarts
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException e) {
                    // Handle case where permission cannot be granted
                }

                imgGymLogo.setImageURI(uri);
                saveLogoToPrefs(uri.toString());
            }
        });

        btnSelectLogo.setOnClickListener(v -> mGetContent.launch(new String[]{"image/*"}));

        // Skip moves to main without saving a custom URI
        btnSkip.setOnClickListener(v -> moveToDashboard());
    }

    private void saveLogoToPrefs(String uriString) {
        getSharedPreferences("GymProfileData", MODE_PRIVATE)
                .edit()
                .putString("GYM_LOGO_URI", uriString)
                .apply();
        moveToDashboard();
    }

    private void moveToDashboard() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}