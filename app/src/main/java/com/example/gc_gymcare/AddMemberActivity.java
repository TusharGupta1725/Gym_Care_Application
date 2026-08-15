package com.example.gc_gymcare;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddMemberActivity extends AppCompatActivity {

    // --- MANUALLY ASSIGN YOUR DESTINATION UPI ID HERE ---
    private static final String GYM_UPI_ID = "YOUR_REAL_UPI_ID_HERE@bank";
    private static final String GYM_MERCHANT_NAME = "GymCare";
    // ----------------------------------------------------

    private ImageView imgMemberPhoto;
    private EditText editMemberName, editMemberPhone, editMemberAddress, editAmountPaid;
    private RadioGroup rgPlans, rgPaymentMode;
    private TextView btnSelectDate; // NEW: For the date picker

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    private Calendar selectedStartDate; // NEW: Tracks the custom date

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);

        // Link UI Elements
        imgMemberPhoto = findViewById(R.id.imgMemberPhoto);
        editMemberName = findViewById(R.id.editMemberName);
        editMemberPhone = findViewById(R.id.editMemberPhone);
        editMemberAddress = findViewById(R.id.editMemberAddress);
        editAmountPaid = findViewById(R.id.editAmountPaid);
        rgPlans = findViewById(R.id.rgPlans);
        rgPaymentMode = findViewById(R.id.rgPaymentMode);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        Button btnSaveMember = findViewById(R.id.btnSaveMember);

        // Initialize default start date to RIGHT NOW
        selectedStartDate = Calendar.getInstance();

        // Setup Date Picker Click
        btnSelectDate.setOnClickListener(v -> showDatePicker());

        // Setup Camera Capture Result
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null && extras.containsKey("data")) {
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            imgMemberPhoto.setImageBitmap(imageBitmap);
                            imgMemberPhoto.setImageTintList(null);
                        }
                    }
                }
        );

        // Setup Permission Result
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) openCamera();
                    else Toast.makeText(this, "Camera permission required.", Toast.LENGTH_SHORT).show();
                }
        );

        // Click Listeners
        imgMemberPhoto.setOnClickListener(v -> checkPermissionAndOpenCamera());
        btnSaveMember.setOnClickListener(v -> validateCalculateAndSave());
    }

    // --- NEW: DATE PICKER METHOD ---
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedStartDate.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    btnSelectDate.setText("Starts: " + sdf.format(selectedStartDate.getTime()));
                },
                selectedStartDate.get(Calendar.YEAR),
                selectedStartDate.get(Calendar.MONTH),
                selectedStartDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void checkPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private void validateCalculateAndSave() {
        String name = editMemberName.getText().toString().trim();
        String phone = editMemberPhone.getText().toString().trim();
        String address = editMemberAddress.getText().toString().trim();

        // 1. Validation
        if (name.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill in Name and Address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.length() != 10) {
            editMemberPhone.setError("Must be exactly 10 digits");
            return;
        }

        int selectedPlanId = rgPlans.getCheckedRadioButtonId();
        if (selectedPlanId == -1) {
            Toast.makeText(this, "You MUST select a membership plan", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Pricing & Time Math
        int planPrice;
        int monthsToAdd;
        String planName = ""; // Track the name for the history ledger

        if (selectedPlanId == R.id.rbPlan1) { planPrice = 800; monthsToAdd = 1; planName = "1 Month"; }
        else if (selectedPlanId == R.id.rbPlan3) { planPrice = 2000; monthsToAdd = 3; planName = "3 Months"; }
        else if (selectedPlanId == R.id.rbPlan6) { planPrice = 4000; monthsToAdd = 6; planName = "6 Months"; }
        else if (selectedPlanId == R.id.rbPlan12) { planPrice = 8000; monthsToAdd = 12; planName = "1 Year"; }
        else { planPrice = 0; monthsToAdd = 0; planName = "Custom"; }

        // --- AUTOMATED TIME CALCULATION (USING SELECTED DATE) ---
        long startDateInMillis = selectedStartDate.getTimeInMillis();
        long endDate = 0;

        if (monthsToAdd > 0) {
            Calendar endCal = (Calendar) selectedStartDate.clone(); // Clone so we don't alter the start date!

            // 🛑 REMEMBER TO SWITCH THIS BACK AFTER YOUR-15-SECOND TEST!
//            endCal.add(Calendar.SECOND, 60); // Testing-15-seconds
             endCal.add(Calendar.MONTH, monthsToAdd); // Real logic

            endDate = endCal.getTimeInMillis();
        }

        // Get amount paid
        String amountPaidStr = editAmountPaid.getText().toString().trim();
        int amountPaid = 0;
        if (!amountPaidStr.isEmpty()) {
            amountPaid = Integer.parseInt(amountPaidStr);
        }
        int remainingBalance = planPrice - amountPaid;
        int selectedPaymentModeId = rgPaymentMode.getCheckedRadioButtonId();

        // 3. Routing (Online QR vs Cash)
        // Notice we are passing the new startDateInMillis and planName to these methods now!
        if (selectedPaymentModeId == R.id.rbOnline && amountPaid > 0) {
            showManualPaymentDialog(name, phone, planPrice, amountPaid, remainingBalance, startDateInMillis, endDate, planName);
        } else {
            saveMemberToDatabase(name, phone, planPrice, amountPaid, remainingBalance, startDateInMillis, endDate, planName);
        }
    }

    // Updated Signature to include start date and plan name
    private void showManualPaymentDialog(String name, String phone, int planPrice, int amountPaid, int remainingBalance, long startDateInMillis, long endDate, String planName) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_manual_payment, null);
        builder.setView(dialogView);

        ImageView imgQrCode = dialogView.findViewById(R.id.imgQrCode);
        TextView txtAmount = dialogView.findViewById(R.id.txtDialogAmount);
        Button btnConfirmPayment = dialogView.findViewById(R.id.btnConfirmPayment);

        txtAmount.setText(String.format(Locale.getDefault(), "Amount: ₹%d", amountPaid));

        String formattedAmount = amountPaid + ".00";
        String cleanUpiId = GYM_UPI_ID.trim();
        String cleanMerchantName = GYM_MERCHANT_NAME.trim().replace(" ", "");

        String upiPayload = "upi://pay?pa=" + cleanUpiId +
                "&pn=" + cleanMerchantName +
                "&tn=GymMembership" +
                "&am=" + formattedAmount +
                "&cu=INR";

        try {
            com.google.zxing.MultiFormatWriter multiFormatWriter = new com.google.zxing.MultiFormatWriter();
            com.google.zxing.common.BitMatrix bitMatrix = multiFormatWriter.encode(
                    upiPayload,
                    com.google.zxing.BarcodeFormat.QR_CODE,
                    500,
                    500
            );

            com.journeyapps.barcodescanner.BarcodeEncoder barcodeEncoder = new com.journeyapps.barcodescanner.BarcodeEncoder();
            Bitmap qrBitmap = barcodeEncoder.createBitmap(bitMatrix);
            imgQrCode.setImageBitmap(qrBitmap);

        } catch (Exception e) {
            android.util.Log.e("AddMemberActivity", "Failed to generate QR Code", e);
            Toast.makeText(this, "Failed to generate QR Code.", Toast.LENGTH_SHORT).show();
        }

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setCancelable(true);

        btnConfirmPayment.setOnClickListener(v -> {
            dialog.dismiss();
            saveMemberToDatabase(name, phone, planPrice, amountPaid, remainingBalance, startDateInMillis, endDate, planName);
        });

        dialog.show();
    }

    // Updated Signature to log to the new history table!
    // Updated Signature to log to the new history table AND trigger auto-backup!
    private void saveMemberToDatabase(String name, String phone, int planPrice, int amountPaid, int remainingBalance, long startDateInMillis, long endDate, String planName) {
        try (DatabaseHelper dbHelper = new DatabaseHelper(this)) {
            // 1. Save main member profile
            Member newMember = new Member(name, phone, planPrice, amountPaid, remainingBalance, endDate);
            dbHelper.addMember(newMember);

            // 2. LOG PURCHASE TO THE NEW HISTORY TABLE
            dbHelper.logPlanPurchase(phone, planName, startDateInMillis, endDate, amountPaid);

            // 3. THE MISSING LINK: INSTANTLY BACKUP TO DOWNLOADS FOLDER
            dbHelper.backupDatabase(this);

            // 👇 THE NEW MAGIC: JUMP TO WHATSAPP 👇
            sendWhatsAppReceipt(phone, name, planName, amountPaid, remainingBalance);

            // THE SMS SENDER MAGIC
            sendWelcomeSMS(phone, name, planName, amountPaid, remainingBalance);
            android.widget.Toast.makeText(this, name + " saved successfully & backed up!", android.widget.Toast.LENGTH_SHORT).show();

            // Trigger the Instant Notification Panel Alert!
            NotificationHelper.showNotification(this, "New Member Joined! 🎉", name + " has been added to GymCare.");
            finish();
        }

    }
    // ==========================================
    // WHATSAPP ONE-TAP ENGINE
    // ==========================================
    private void sendWhatsAppReceipt(String phone, String name, String planName, int amountPaid, int remainingBalance) {
        try {
            // WhatsApp requires a country code to find the contact.
            // If the number is exactly 10 digits, we automatically add "91" to the front.
            String cleanPhone = phone.replaceAll("\\D+", "");
            if (cleanPhone.length() == 10) {
                cleanPhone = "91" + cleanPhone;
            }

            // Draft a beautiful, professional receipt message
            String message = "🏋️ *Welcome to GymCare, " + name + "!* 🏋️\n\n" +
                    "Your membership has been successfully activated.\n\n" +
                    "🧾 *Receipt Details:*\n" +
                    "🔹 Plan: " + planName + "\n" +
                    "🔹 Amount Paid: ₹" + amountPaid + "\n" +
                    "🔹 Remaining Due: ₹" + remainingBalance + "\n\n" +
                    "Let's crush those fitness goals! 💪";

            // Encode the message so spaces and emojis don't break the web link
            String encodedMessage = java.net.URLEncoder.encode(message, "UTF-8");
            String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + encodedMessage;

            // Trigger the jump to WhatsApp
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(url));
            startActivity(intent);

        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Could not open WhatsApp. Is it installed?", android.widget.Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    // ==========================================
    // SILENT SMS AUTOMATION ENGINE
    // ==========================================
    private void sendWelcomeSMS(String phone, String name, String planName, int amountPaid, int remainingBalance) {
        try {
            // Check if we have permission before trying to send
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                android.widget.Toast.makeText(this, "SMS Permission Denied. Message not sent.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            // Clean the phone number (remove spaces/dashes)
            String cleanPhone = phone.replaceAll("\\D+", "");

            // Draft the message (Keep it concise for SMS character limits)
            String message = "Welcome to GymCare, " + name + "!\n" +
                    "Plan: " + planName + "\n" +
                    "Paid: Rs." + amountPaid + "\n" +
                    "Due: Rs." + remainingBalance + "\n" +
                    "Let's crush those goals!";

            // Get the default Android SMS Manager
            android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();

            // If the message is longer than 160 characters, this safely splits it up
            java.util.ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null);

        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Failed to send SMS.", android.widget.Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}