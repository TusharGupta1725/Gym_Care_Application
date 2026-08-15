package com.example.gc_gymcare;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

// Import uCrop library
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;

public class GymProfileActivity extends AppCompatActivity {

    private static final String PREF_NAME = "GymProfileData";
    private ImageView imgProfileLogoPreview;

    // We now have TWO launchers. One for the gallery, one for the cropper.
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym_profile);

        imgProfileLogoPreview = findViewById(R.id.imgProfileLogoPreview);
        Button btnChangeLogo = findViewById(R.id.btnChangeLogo);
        Button btnRemoveLogo = findViewById(R.id.btnRemoveLogo);

        loadCurrentLogo();

        // -------------------------------------------------------------
        // LAUNCHER 2: Receives the CROPPED photo from uCrop and saves it
        // -------------------------------------------------------------
        cropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Get the final edited image
                        Uri croppedUri = UCrop.getOutput(result.getData());
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), croppedUri);
                            imgProfileLogoPreview.setImageBitmap(bitmap);
                            saveLogoToInternalStorage(bitmap);
                        } catch (Exception e) {
                            android.util.Log.e("GymProfileActivity", "Error loading cropped image", e);
                            Toast.makeText(this, "Failed to load cropped image", Toast.LENGTH_SHORT).show();
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
                        Throwable cropError = UCrop.getError(result.getData());
                        String errorMessage = (cropError != null) ? cropError.getMessage() : "Unknown error";
                        Toast.makeText(this, "Crop Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
        );

        // -------------------------------------------------------------
        // LAUNCHER 1: Receives the ORIGINAL photo from the gallery
        // -------------------------------------------------------------
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri sourceUri = result.getData().getData();

                        if (sourceUri != null) {
                            // Create a temporary file to hold the cropped result
                            Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "temp_cropped_profile.png"));

                            // Setup the uCrop interface (Force 1:1 Square aspect ratio for circles)
                            Intent uCropIntent = UCrop.of(sourceUri, destinationUri)
                                    .withAspectRatio(1, 1)
                                    .withMaxResultSize(800, 800)
                                    .getIntent(GymProfileActivity.this);

                            // Launch the cropper screen
                            cropLauncher.launch(uCropIntent);
                        }
                    }
                }
        );

        // Click Listeners
        btnChangeLogo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        btnRemoveLogo.setOnClickListener(v -> removeCustomLogo());
    }

    // --- Helper Methods ---

    private void loadCurrentLogo() {
        boolean hasLogo = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getBoolean("HAS_OWNER_PHOTO", false);
        if (hasLogo) {
            try {
                File file = new File(getFilesDir(), "owner_photo.png");
                if (file.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    imgProfileLogoPreview.setImageBitmap(bitmap);
                }
            } catch (Exception e) {
                android.util.Log.e("GymProfileActivity", "Error loading logo", e);
            }
        } else {
            imgProfileLogoPreview.setImageResource(R.drawable.ic_default_profile);
        }
    }

    private void saveLogoToInternalStorage(Bitmap bitmap) {
        File file = new File(getFilesDir(), "owner_photo.png");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);

            getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit().putBoolean("HAS_OWNER_PHOTO", true).apply();
            Toast.makeText(this, "Profile Photo Updated!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("GymProfileActivity", "Error saving logo", e);
        }
    }

    private void removeCustomLogo() {
        try {
            File file = new File(getFilesDir(), "owner_photo.png");
            if (file.exists()) {
                if (!file.delete()) {
                    android.util.Log.w("GymProfileActivity", "Failed to delete owner_photo.png");
                }
            }

            getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit().putBoolean("HAS_OWNER_PHOTO", false).apply();
            imgProfileLogoPreview.setImageResource(R.drawable.ic_default_profile);

            Toast.makeText(this, "Photo removed.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("GymProfileActivity", "Error removing photo", e);
        }
    }
}