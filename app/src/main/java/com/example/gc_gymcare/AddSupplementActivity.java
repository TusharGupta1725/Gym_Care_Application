package com.example.gc_gymcare;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AddSupplementActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // You will need to create a layout for this! We can use a clone of activity_add_member without the plans.
        setContentView(R.layout.activity_add_supplement);

        EditText editName = findViewById(R.id.editSuppName);
        EditText editPhone = findViewById(R.id.editSuppPhone);
        EditText editItemName = findViewById(R.id.editSuppItemName); // e.g. "Creatine"
        EditText editItemPrice = findViewById(R.id.editSuppItemPrice); // e.g. "1500"
        EditText editAmountPaid = findViewById(R.id.editSuppAmountPaid); // e.g. "1000"
        Button btnSave = findViewById(R.id.btnSaveSupplement);

        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String itemName = editItemName.getText().toString().trim();
            String priceStr = editItemPrice.getText().toString().trim();
            String paidStr = editAmountPaid.getText().toString().trim();

            if (name.isEmpty()  || itemName.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (phone.length() != 10) {
                editPhone.setError("Must be exactly 10 digits");
                return;
            }

            int itemPrice = Integer.parseInt(priceStr);
            int amountPaid = paidStr.isEmpty() ? 0 : Integer.parseInt(paidStr);

            DatabaseHelper db = new DatabaseHelper(this);
            db.processSupplementSale(name, phone, itemName, itemPrice, amountPaid);

            // 👇 THIS IS YOUR STEP 3: THE AUTO-BACKUP TRIGGER 👇
            db.backupDatabase(this);

            sendWhatsAppReceipt(phone, name, itemName, itemPrice, amountPaid);

            Toast.makeText(this, "Supplement Sale Recorded & Backed Up!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
    private void sendWhatsAppReceipt(String phone, String name, String itemName, int amountPaid, int remainingBalance) {
        try {
            // WhatsApp requires a country code to find the contact.
            // If the number is exactly 10 digits, we automatically add "91" to the front.
            String cleanPhone = phone.replaceAll("\\D+", "");
            if (cleanPhone.length() == 10) {
                cleanPhone = "91" + cleanPhone;
            }

            // Draft a beautiful, professional receipt message
            String message = "🏋️ *Welcome to GymCare, " + name + "!* 🏋️\n\n" +
                    "You have brought the supplement.\n\n" +
                    "🧾 *Supplement Details:*\n" +
                    "🔹 Item Name: " + itemName + "\n" +
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

}