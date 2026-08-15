package com.example.gc_gymcare;

import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;

public class LogoActivity extends AppCompatActivity {
    // This class is currently not in use. See LogoSetupActivity.
    private void saveLogoUri(String uriString) {
        SharedPreferences prefs = getSharedPreferences("GymProfileData", MODE_PRIVATE);
        prefs.edit().putString("GYM_LOGO_URI", uriString).apply();
    }
}