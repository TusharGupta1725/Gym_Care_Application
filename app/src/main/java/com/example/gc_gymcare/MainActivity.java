package com.example.gc_gymcare;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_NAME = "GymProfileData";
    private DatabaseHelper dbHelper;

    // State Trackers
    private boolean showingPaidMembers = true;
    private boolean showingPaidSupplements = true;
    private int currentTab = 0; // 0 = Members, 1 = Supplements, 2 = Gym

    // Search Master Lists (holds the full data so we can filter it safely)
    private List<Member> currentMembersList = new ArrayList<>();
    private List<SupplementCustomer> currentSuppList = new ArrayList<>();

    // UI Elements
    private EditText searchField;
    private View layoutMembersSection, layoutSupplementsSection, layoutGymSection;
    private RecyclerView recyclerMembers, recyclerSupplements, recyclerGymDueMembers;
    private TextView tabPaid, tabUnpaid, tabSuppPaid, tabSuppUnpaid;
    private FloatingActionButton fabAddMember;
    private TextView navMembers, navSupplements, navGym, textOwnerName, textTotalMembers, textTotalReceived, textTotalDueSum;
    private ImageView imgGymLogo;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
/*
        Intent intent = new Intent(this, ComposeDashboardActivity.class);
        startActivity(intent);
        finish();
*/
        // 1 - Hide the status bar and navigation bar for true fullscreen
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController =
                new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars());
        windowInsetsController.setSystemBarsBehavior(
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        //2 - Draw the layout
        setContentView(R.layout.activity_main);
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.SEND_SMS}, 101);
        }

            // 👇 ASK FOR SMS PERMISSION WHEN OPENING THE EDIT SCREEN 👇
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.SEND_SMS}, 101);
            }

            // ... rest of your onCreate code

        // Ask for permissions on startup
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.POST_NOTIFICATIONS,
                        // ... your other permissions can go here if you bundled them
                }, 102);
            }
        }

        // 3. SMART PERMISSION CHECK (Handles Old & New Phones)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // --- FOR ANDROID 11 AND HIGHER ---
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    android.content.Intent permissionIntent = new android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    permissionIntent.addCategory("android.intent.category.DEFAULT");
                    permissionIntent.setData(android.net.Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                    startActivity(permissionIntent);
                } catch (Exception e) {
                    android.content.Intent permissionIntent = new android.content.Intent();
                    permissionIntent.setAction(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(permissionIntent);
                }
            } else {
                runBackupCheck(); // Permission already granted
            }
        } else {
            // --- FOR ANDROID 10 AND LOWER ---
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.SEND_SMS
                }, 1);
            } else {
                runBackupCheck(); // Permission already granted
            }
        }

        // --- EMERGENCY DATA AUTO-RECOVERY ---
        DatabaseHelper startupCheck = new DatabaseHelper(this);


        dbHelper = new DatabaseHelper(this);

        // 1. Link Core Layouts & Search
        searchField = findViewById(R.id.edit_search);
        layoutMembersSection = findViewById(R.id.layoutMembersSection);
        layoutSupplementsSection = findViewById(R.id.layoutSupplementsSection);
        layoutGymSection = findViewById(R.id.layoutGymSection);
        fabAddMember = findViewById(R.id.fabAddMember);

        // 2. Link Recyclers & Tabs
        tabPaid = findViewById(R.id.tabPaid);
        tabUnpaid = findViewById(R.id.tabUnpaid);
        recyclerMembers = findViewById(R.id.recyclerMembers);
        recyclerMembers.setLayoutManager(new LinearLayoutManager(this));

        tabSuppPaid = findViewById(R.id.tabSuppPaid);
        tabSuppUnpaid = findViewById(R.id.tabSuppUnpaid);
        recyclerSupplements = findViewById(R.id.recyclerSupplements);
        recyclerSupplements.setLayoutManager(new LinearLayoutManager(this));

        // 3. Link Dashboard & Nav
        textOwnerName = findViewById(R.id.textOwnerName);
        textTotalMembers = findViewById(R.id.textTotalMembers);
        textTotalReceived = findViewById(R.id.textTotalReceived);
        textTotalDueSum = findViewById(R.id.textTotalDueSum);
        recyclerGymDueMembers = findViewById(R.id.recyclerGymDueMembers);
        recyclerGymDueMembers.setLayoutManager(new LinearLayoutManager(this));
        Button btnViewGymHistory = findViewById(R.id.btnViewGymHistory);
        navMembers = findViewById(R.id.navMembers);
        navSupplements = findViewById(R.id.navSupplements);
        navGym = findViewById(R.id.navGym);
        imgGymLogo = findViewById(R.id.imgGymLogo);

        // --- SETUP LOGO & HISTORY BUTTON ---
        imgGymLogo.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, GymProfileActivity.class)));
        loadCustomLogo(imgGymLogo);
        if (btnViewGymHistory != null) {
            btnViewGymHistory.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, GymHistoryActivity.class)));
        }

        // --- AUTO-UPDATE EXPIRATIONS ---
        ArrayList<String> expired = dbHelper.processExpirations();
        if (!expired.isEmpty()) {
            android.widget.Toast.makeText(this, expired.size() + " plans expired today!", android.widget.Toast.LENGTH_LONG).show();
        }

        // --- CONTEXT-AWARE SEARCH BAR LOGIC ---
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                if (currentTab == 0) {
                    filterMembers(query);
                } else if (currentTab == 1) {
                    filterSupplements(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // --- DYNAMIC FAB ---
        fabAddMember.setOnClickListener(v -> {
            if (currentTab == 0) startActivity(new Intent(MainActivity.this, AddMemberActivity.class));
            else if (currentTab == 1) startActivity(new Intent(MainActivity.this, AddSupplementActivity.class));
        });

        // --- MEMBERS TABS ---
        tabPaid.setOnClickListener(v -> {
            showingPaidMembers = true;
            updateSubTabs(tabPaid, tabUnpaid);
            loadMembersList(true);
        });

        tabUnpaid.setOnClickListener(v -> {
            showingPaidMembers = false;
            updateSubTabs(tabUnpaid, tabPaid);
            loadMembersList(false);
        });

        // --- SUPPLEMENTS TABS ---
        tabSuppPaid.setOnClickListener(v -> {
            showingPaidSupplements = true;
            updateSubTabs(tabSuppPaid, tabSuppUnpaid);
            loadSupplementsList(true);
        });

        tabSuppUnpaid.setOnClickListener(v -> {
            showingPaidSupplements = false;
            updateSubTabs(tabSuppUnpaid, tabSuppPaid);
            loadSupplementsList(false);
        });

        // --- BOTTOM NAVIGATION LOGIC ---
        navMembers.setOnClickListener(v -> {
            currentTab = 0;
            updateNavTabs(navMembers, navSupplements, navGym);
            layoutMembersSection.setVisibility(View.VISIBLE);
            layoutSupplementsSection.setVisibility(View.GONE);
            layoutGymSection.setVisibility(View.GONE);
            fabAddMember.setVisibility(View.VISIBLE);
            searchField.setVisibility(View.VISIBLE); // Show Search
            searchField.setHint("Search members...");
            loadMembersList(showingPaidMembers);
        });

        navSupplements.setOnClickListener(v -> {
            currentTab = 1;
            updateNavTabs(navSupplements, navMembers, navGym);
            layoutMembersSection.setVisibility(View.GONE);
            layoutSupplementsSection.setVisibility(View.VISIBLE);
            layoutGymSection.setVisibility(View.GONE);
            fabAddMember.setVisibility(View.VISIBLE);
            searchField.setVisibility(View.VISIBLE); // Show Search
            searchField.setHint("Search supplements...");
            loadSupplementsList(showingPaidSupplements);
        });

        navGym.setOnClickListener(v -> {
            currentTab = 2;
            updateNavTabs(navGym, navMembers, navSupplements);
            layoutMembersSection.setVisibility(View.GONE);
            layoutSupplementsSection.setVisibility(View.GONE);
            layoutGymSection.setVisibility(View.VISIBLE);
            fabAddMember.setVisibility(View.GONE);
            searchField.setVisibility(View.GONE); // Hide Search for Dashboard
            loadGymAnalytics();
        });

        // Initialize First View
        loadMembersList(true);

        // ==========================================
        // START THE DAILY BACKGROUND SWEEPER
        // ==========================================
        // 1. REQUEST NOTIFICATION PERMISSIONS (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {

                androidx.core.app.ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 102);
            }
        }

        // 2. START THE DAILY BACKGROUND SWEEPER (Every 24 Hours)
        androidx.work.PeriodicWorkRequest expiryWorkRequest =
                new androidx.work.PeriodicWorkRequest.Builder(ExpiryWorker.class, 2, java.util.concurrent.TimeUnit.HOURS)
                        .build();

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyExpiryCheck",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                expiryWorkRequest
        );

        // 3. TEMPORARY TEST: Trigger an instant notification after 2 seconds to prove it works!
        new android.os.Handler().postDelayed(() -> {
            NotificationHelper.showNotification(this, "GymCare Activated! 🏋️‍♂️", "Background monitoring engine is live.");
        }, 2000);

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            runBackupCheck();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        // 1. Check if we just got permission from the Settings menu
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (android.os.Environment.isExternalStorageManager()) {
                runBackupCheck();
            }
        }

        // 2. Reload the UI normally
        loadCustomLogo(imgGymLogo);
        if (dbHelper != null) {
            // (Assuming you have currentTab logic here. If not, just ignore this comment)
        }
    }

    // The Bulletproof Restore Engine (White-Screen Fix)
    // The Complete, Loop-Proof Recovery Engine
    private void runBackupCheck() {
        android.content.SharedPreferences prefs = getSharedPreferences("GymCarePrefs", MODE_PRIVATE);
        DatabaseHelper checkDb = new DatabaseHelper(this);

        int currentMembers = checkDb.getTotalMembersCount();

        // SMART RESET: If we have members, the app is working perfectly.
        // We safely reset the flag in the background so it's ready for the NEXT time you uninstall.
        if (currentMembers > 0) {
            prefs.edit().putBoolean("hasAttemptedRestore", false).apply();
            checkDb.close();
            return;
        }

        // If the database is empty (0 members), check if we already tried to restore
        if (prefs.getBoolean("hasAttemptedRestore", false)) {
            checkDb.close();
            return; // Stop here to prevent the white-screen loop!
        }

        // Instantly lock the flag so it cannot loop
        prefs.edit().putBoolean("hasAttemptedRestore", true).commit();

        // Close databases to prevent corruption
        checkDb.close();
        if (dbHelper != null) {
            dbHelper.close();
        }

        // Run the restore
        DatabaseHelper restorer = new DatabaseHelper(this);
        boolean recovered = restorer.restoreDatabase(this);
        restorer.close();

        if (recovered) {
            android.widget.Toast.makeText(this, "Backup Restored Successfully!", android.widget.Toast.LENGTH_LONG).show();

            // Smoothly refresh the UI
            android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    // --- SEARCH FILTERING METHODS ---
    private void filterMembers(String query) {
        List<Member> filteredList = new ArrayList<>();
        for (Member m : currentMembersList) {
            if (m.getName().toLowerCase().contains(query) || m.getPhone().contains(query)) {
                filteredList.add(m);
            }
        }
        recyclerMembers.setAdapter(new MemberAdapter(filteredList));
    }

    private void filterSupplements(String query) {
        List<SupplementCustomer> filteredList = new ArrayList<>();
        for (SupplementCustomer s : currentSuppList) {
            if (s.getName().toLowerCase().contains(query) || s.getPhone().contains(query)) {
                filteredList.add(s);
            }
        }
        recyclerSupplements.setAdapter(new SupplementAdapter(filteredList));
    }

    // --- DATA LOADING METHODS ---
    private void loadMembersList(boolean showPaid) {
        if (dbHelper == null) return;
        currentMembersList = dbHelper.getMembers(showPaid);
        // Automatically apply the search if the user was already typing
        filterMembers(searchField.getText().toString().trim().toLowerCase());
    }

    private void loadSupplementsList(boolean showPaid) {
        if (dbHelper == null) return;
        currentSuppList = dbHelper.getSupplementCustomers(showPaid);
        // Automatically apply the search if the user was already typing
        filterSupplements(searchField.getText().toString().trim().toLowerCase());
    }

    private void loadGymAnalytics() {
        if (dbHelper == null) return;
        textTotalMembers.setText(String.format(java.util.Locale.getDefault(), "%d", dbHelper.getTotalMembersCount()));
        textTotalReceived.setText(String.format(java.util.Locale.getDefault(), "₹%d", dbHelper.getTotalAmountReceived()));
        textTotalDueSum.setText(String.format(java.util.Locale.getDefault(), "₹%d", dbHelper.getTotalAmountDue()));

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        textOwnerName.setText(String.format(java.util.Locale.getDefault(), "Owner: %s", prefs.getString("OWNER_NAME", "Gym Owner")));
        recyclerGymDueMembers.setAdapter(new MemberAdapter(dbHelper.getMembers(false)));
    }

    // --- UI HELPER METHODS ---
    // --- UI HELPER METHODS ---
    private void updateNavTabs(TextView active, TextView inactive1, TextView inactive2) {
        // Active State: Solid White Pill, Black Bold Text
        active.setBackgroundResource(R.drawable.nav_item_active);
        active.setTextColor(Color.parseColor("#000000")); // Black text

        // Inactive State 1: Transparent background, Faded White text
        inactive1.setBackgroundResource(android.R.color.transparent);
        inactive1.setTextColor(Color.parseColor("#BBFFFFFF"));

        // Inactive State 2: Transparent background, Faded White text
        inactive2.setBackgroundResource(android.R.color.transparent);
        inactive2.setTextColor(Color.parseColor("#BBFFFFFF"));
    }

    private void updateSubTabs(TextView active, TextView inactive) {
        active.setBackgroundResource(R.drawable.tab_active_glow);
        active.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        inactive.setBackgroundResource(android.R.color.transparent);
        inactive.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
    }

    private void loadCustomLogo(ImageView imageView) {
        boolean hasLogo = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getBoolean("HAS_OWNER_PHOTO", false);
        if (hasLogo) {
            try {
                File file = new File(getFilesDir(), "owner_photo.png");
                if (file.exists()) {
                    imageView.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
                    return;
                }
            } catch (Exception e) {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        }
        imageView.setImageResource(R.drawable.ic_default_profile);
    }

}