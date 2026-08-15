package com.example.gc_gymcare;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

public class EditMemberActivity extends AppCompatActivity {

    private long originalEndDate;
    private EditText editUpdateName, editUpdatePhone, editNewDeposit;
    private RadioGroup rgExtendPlans;

    // Original Data variables
    private String originalPhone;
    private int originalAmountPaid;
    private int originalBalance;
    private int originalPlanPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_member);

        // catch the date from intent
        originalEndDate = getIntent().getLongExtra("END_DATE", 0);

        // 1. Link UI elements
        editUpdateName = findViewById(R.id.editUpdateName);
        editUpdatePhone = findViewById(R.id.editUpdatePhone);
        editNewDeposit = findViewById(R.id.editNewDeposit);

        TextView textCurrentPlan = findViewById(R.id.textCurrentPlan);
        TextView textTotalPaid = findViewById(R.id.textTotalPaid);
        TextView textCurrentDue = findViewById(R.id.textCurrentDue);

        rgExtendPlans = findViewById(R.id.rgExtendPlans);
        Button btnSaveChanges = findViewById(R.id.btnSaveChanges);

        // 2. Catch the data sent from the Adapter
        String name = getIntent().getStringExtra("NAME");
        originalPhone = getIntent().getStringExtra("PHONE");
        originalAmountPaid = getIntent().getIntExtra("PAID", 0);
        originalBalance = getIntent().getIntExtra("BALANCE", 0);
        originalPlanPrice = originalAmountPaid + originalBalance;

        // 3. Pre-fill the Editable Personal Details
        editUpdateName.setText(name);
        editUpdatePhone.setText(originalPhone);

        // 4. Fill the Read-Only "Current Status" Block
        textTotalPaid.setText(String.format(Locale.getDefault(), "Paid: ₹%d", originalAmountPaid));
        textCurrentDue.setText(String.format(Locale.getDefault(), "Due: ₹%d", originalBalance));

        if (originalPlanPrice == 800) textCurrentPlan.setText("Current Plan: 1 Month (₹800)");
        else if (originalPlanPrice == 2000) textCurrentPlan.setText("Current Plan: 3 Months (₹2000)");
        else if (originalPlanPrice == 4000) textCurrentPlan.setText("Current Plan: 6 Months (₹4000)");
        else if (originalPlanPrice == 8000) textCurrentPlan.setText("Current Plan: 1 Year (₹8000)");
        else textCurrentPlan.setText(String.format(Locale.getDefault(), "Current Plan: Custom (₹%d)", originalPlanPrice));

        // 5. Handle Save Button Click
        btnSaveChanges.setOnClickListener(v -> saveUpdatedData());

        // 1. Identify Status
        try (DatabaseHelper dbHelper = new DatabaseHelper(this)) {
            boolean isExpired = dbHelper.isMemberExpired(originalEndDate);
            boolean hasBalance = originalBalance > 0;

            if (isExpired) {
                // EXPIRATION MODE: They MUST pick a new plan
                textCurrentPlan.setText("Status: EXPIRED (Must Renew)");
                textCurrentPlan.setTextColor(Color.RED);
                // Hide the "No Extension" radio button
                findViewById(R.id.rbExtendNone).setVisibility(View.GONE);
            } else if (hasBalance) {
                // RUNNING PLAN WITH BALANCE: They can choose to pay balance OR extend
                textCurrentPlan.setText(String.format(Locale.getDefault(), "Status: ACTIVE (Due: ₹%d)", originalBalance));
                findViewById(R.id.rbExtendNone).setVisibility(View.VISIBLE);
            } else {
                // FULLY PAID: They can choose to extend early
                textCurrentPlan.setText("Status: ACTIVE (Fully Paid)");
                findViewById(R.id.rbExtendNone).setVisibility(View.VISIBLE);
            }
        }

        Button btnViewHistory = findViewById(R.id.btnViewHistory);
        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(EditMemberActivity.this, HistoryActivity.class);
            intent.putExtra("PHONE", originalPhone);
            intent.putExtra("NAME", editUpdateName.getText().toString());
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void saveUpdatedData() {
        String newName = editUpdateName.getText().toString().trim();
        String newPhone = editUpdatePhone.getText().toString().trim();

        // Basic Validation
        if (newName.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(this, "Name and Phone cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPhone.length() != 10) {
            editUpdatePhone.setError("Phone must be 10 digits");
            return;
        }

        // 1. Calculate Plan Extension Cost & Time
        int extensionCost = 0;
        int monthsToAdd = 0;
        String planName = "None";
        int selectedPlanId = rgExtendPlans.getCheckedRadioButtonId();

        if (selectedPlanId == R.id.rbExtendPlan1) { extensionCost = 800; monthsToAdd = 1; planName = "1 Month"; }
        else if (selectedPlanId == R.id.rbExtendPlan3) { extensionCost = 2000; monthsToAdd = 3; planName = "3 Months"; }
        else if (selectedPlanId == R.id.rbExtendPlan6) { extensionCost = 4000; monthsToAdd = 6; planName = "6 Months"; }
        else if (selectedPlanId == R.id.rbExtendPlan12) { extensionCost = 8000; monthsToAdd = 12; planName = "1 Year"; }

        // --- SMART TIME STACKING ---
        long finalEndDate = originalEndDate;
        long startDateInMillis = System.currentTimeMillis();

        if (monthsToAdd > 0) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            // If their plan already expired, start the clock from TODAY.
            // If they are renewing early, stack the new time on top of their remaining time!
            startDateInMillis = Math.max(originalEndDate, System.currentTimeMillis());
            cal.setTimeInMillis(startDateInMillis);

            cal.add(java.util.Calendar.MONTH, monthsToAdd);
//            cal.add(Calendar.SECOND, 60);
            finalEndDate = cal.getTimeInMillis();
        }

        // --- 1. Grabbing the new inputs ---
        String depositStr = editNewDeposit.getText().toString().trim();
        int newDeposit = depositStr.isEmpty() ? 0 : Integer.parseInt(depositStr);

        // --- 2. THE BULLETPROOF MASTER MATH ---
        // Final Balance = (What they already owed) + (Cost of new plan) - (What they just paid)
        int finalBalance = originalBalance + extensionCost - newDeposit;

        // Update the visual totals for the database record
        int finalPlanPrice = originalPlanPrice + extensionCost;
        int finalPaidTotal = originalAmountPaid + newDeposit;

        // --- 3. Save to Database ---
        DatabaseHelper db = null;
        try {
            db = new DatabaseHelper(this);

            // Update Main Table
            db.updateFullMemberDetails(originalPhone, newName, newPhone, finalPlanPrice, finalPaidTotal, finalBalance, finalEndDate);
            db.backupDatabase(this);

            // BULLETPROOF ROUTING LOGIC
            if (monthsToAdd > 0) {
                // PATH 1: RENEWAL
                db.logPlanPurchase(originalPhone, planName, startDateInMillis, finalEndDate, newDeposit);
                Toast.makeText(this, "Processing Renewal...", Toast.LENGTH_SHORT).show();
                sendRenewalSMS(newPhone, newName, planName, newDeposit, finalBalance);
            }
            else if (newDeposit > 0) {
                // PATH 2: DUE PAYMENT
                // Use the proven logPlanPurchase method to log the due payment safely
                db.logPlanPurchase(originalPhone, "Due Payment", System.currentTimeMillis(), finalEndDate, newDeposit);

                Toast.makeText(this, "Processing Due Payment...", Toast.LENGTH_SHORT).show();
                sendDuePaymentSMS(newPhone, newName, newDeposit, finalBalance);
            }
            else {
                // PATH 3: NOTHING CHANGED BUT NAME/PHONE
                Toast.makeText(this, "Profile Saved! (No Money Added)", Toast.LENGTH_SHORT).show();
            }

        } catch (Throwable t) {
            Toast.makeText(this, "CRITICAL ERROR: " + t.getMessage(), Toast.LENGTH_LONG).show();
            t.printStackTrace();
        } finally {
            if (db != null) {
                db.close();
            }
        }

        // Hold the screen open for transmission
        new android.os.Handler().postDelayed(() -> {
            finish();
        }, 1500);
    }

    // ==========================================
    // WHATSAPP ONE-TAP ENGINE
    // ==========================================
    private void sendWhatsAppReceipt(String phone, String name, String planName, int amountPaid, int remainingBalance) {
        try {
            String cleanPhone = phone.replaceAll("\\D+", "");
            if (cleanPhone.length() == 10) {
                cleanPhone = "91" + cleanPhone;
            }

            String message = "🏋️ *Welcome to GymCare, " + name + "!* 🏋️\n\n" +
                    "Your membership has been successfully activated.\n\n" +
                    "🧾 *Receipt Details:*\n" +
                    "🔹 Plan: " + planName + "\n" +
                    "🔹 Amount Paid: ₹" + amountPaid + "\n" +
                    "🔹 Remaining Due: ₹" + remainingBalance + "\n\n" +
                    "Let's crush those fitness goals! 💪";

            String encodedMessage = java.net.URLEncoder.encode(message, "UTF-8");
            String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + encodedMessage;

            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(url));
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(this, "Could not open WhatsApp. Is it installed?", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // ==========================================
    // RENEWAL SMS AUTOMATION ENGINE
    // ==========================================
    private void sendRenewalSMS(String phone, String name, String planName, int amountPaid, int remainingBalance) {
        try {
            String cleanPhone = phone.replaceAll("\\D+", "");
            if (cleanPhone.length() < 10) return;

            String message = "Thank you for choosing GymCare again, " + name + "!\nRenewed: " + planName + "\nPaid: Rs." + amountPaid + "\nDue: Rs." + remainingBalance + "\nLet's get back to work!";

            android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
            if (smsManager != null) {
                smsManager.sendTextMessage(cleanPhone, null, message, null, null);
                Toast.makeText(this, "✅ Renewal Text Transmitted!", Toast.LENGTH_SHORT).show();
            }

        } catch (Throwable t) {
            Toast.makeText(this, "SMS Engine Blocked: " + t.getMessage(), Toast.LENGTH_LONG).show();
            t.printStackTrace();
        }
    }

    // ==========================================
    // CUSTOM DUE PAYMENT SMS ENGINE
    // ==========================================
    private void sendDuePaymentSMS(String phone, String name, int amountPaid, int remainingBalance) {
        try {
            String cleanPhone = phone.replaceAll("\\D+", "");
            if (cleanPhone.length() < 10) return;

            String message = "Payment Received, " + name + "!\nYou submitted Rupees " + amountPaid + " in your remaining due.\nNew remaining due is Rupees " + remainingBalance + ".\nThank you! - GymCare";

            android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
            if (smsManager != null) {
                smsManager.sendTextMessage(cleanPhone, null, message, null, null);
                Toast.makeText(this, "✅ Due Payment Text Transmitted!", Toast.LENGTH_SHORT).show();
            }

        } catch (Throwable t) {
            Toast.makeText(this, "SMS Engine Blocked: " + t.getMessage(), Toast.LENGTH_LONG).show();
            t.printStackTrace();
        }
    }
}