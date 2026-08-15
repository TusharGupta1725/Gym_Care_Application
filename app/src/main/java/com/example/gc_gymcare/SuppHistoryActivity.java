package com.example.gc_gymcare;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class SuppHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supp_history);

        String phone = getIntent().getStringExtra("PHONE");
        String name = getIntent().getStringExtra("NAME");

        TextView title = findViewById(R.id.txtSuppHistoryTitle);
        title.setText(name + "'s Supplements");

        LinearLayout container = findViewById(R.id.suppHistoryContainer);
        DatabaseHelper db = new DatabaseHelper(this);

        ArrayList<String[]> historyList = db.getSupplementPurchasesAndPayments(phone);

        if (historyList.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No supplement history found.");
            emptyText.setTextColor(Color.GRAY);
            emptyText.setTextSize(16f);
            container.addView(emptyText);
            return;
        }

        // Draw the cards dynamically
        for (String[] data : historyList) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.rounded_search_bg);
            card.setPadding(40, 30, 40, 30);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 24);
            card.setLayoutParams(params);

            // Row 1: Item Name or Payment Title
            TextView txtTitle = new TextView(this);
            txtTitle.setText(data[0]);
            txtTitle.setTextColor(Color.WHITE);
            txtTitle.setTextSize(18f);
            txtTitle.setTypeface(null, android.graphics.Typeface.BOLD);

            // Row 2: Price and Payment Details
            TextView txtDetails = new TextView(this);
            txtDetails.setText(data[1]);
            txtDetails.setTextColor(Color.parseColor("#00E676")); // Green
            txtDetails.setTextSize(16f);
            txtDetails.setPadding(0, 8, 0, 8);

            // Row 3: Timestamp
            TextView txtTime = new TextView(this);
            txtTime.setText(data[2]);
            txtTime.setTextColor(Color.GRAY);
            txtTime.setTextSize(14f);

            card.addView(txtTitle);
            card.addView(txtDetails);
            card.addView(txtTime);
            container.addView(card);
        }
    }
}