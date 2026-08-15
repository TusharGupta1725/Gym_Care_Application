package com.example.gc_gymcare;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.content.SharedPreferences;

public class WelcomeActivity extends AppCompatActivity {

    private static final String PREF_NAME = "GymProfileData";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user has already set up the profile
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (sharedPreferences.contains("GYM_NAME")) {
            Intent intent = new Intent(WelcomeActivity.this, LoadingActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_welcome);

        FloatingActionButton buttonNext = findViewById(R.id.buttonNext);

        buttonNext.setOnClickListener(v -> {
            // 1. Disable the button so the user can't spam-click it while it spins
            buttonNext.setEnabled(false);

            // 2. Animate the button (spin 360 degrees over 800 milliseconds)
            buttonNext.animate()
                    .rotationBy(360f) // One full revolution
                    .setDuration(800) // Time in milliseconds (0.8 seconds)
                    .withEndAction(() -> {
                        // 3. This code runs EXACTLY when the animation finishes
                        Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                        startActivity(intent);

                        // Close Welcome screen so they can't go back to it
                        finish();
                    })
                    .start();
        });
    }
}