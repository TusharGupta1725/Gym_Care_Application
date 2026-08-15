package com.example.gc_gymcare;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        String phone = getIntent().getStringExtra("PHONE");
        String name = getIntent().getStringExtra("NAME");

        TextView txtTitle = findViewById(R.id.txtHistoryTitle);
        txtTitle.setText(name + "'s Ledger");

        LinearLayout container = findViewById(R.id.historyContainer);
        DatabaseHelper db = new DatabaseHelper(this);

        // Fetch all plans AND partial payments
        ArrayList<String[]> historyList = db.getMemberHistoryFull(phone);

        if (historyList.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No transaction history found.");
            emptyText.setTextColor(Color.GRAY);
            emptyText.setTextSize(16f);
            container.addView(emptyText);
            return;
        }

        // Dynamically build a beautiful card for every transaction
        for (String[] item : historyList) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.rounded_search_bg);
            card.setPadding(40, 40, 40, 40);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 30);
            card.setLayoutParams(params);

            // Row 1: Title (e.g. "1 Month Plan" or "Due Payment")
            TextView title = new TextView(this);
            title.setText(item[0]);
            title.setTextColor(Color.WHITE);
            title.setTextSize(18f);
            title.setTypeface(null, android.graphics.Typeface.BOLD);

            // Row 2: Amount Paid (Green)
            TextView amount = new TextView(this);
            amount.setText("Paid: " + item[1]);
            amount.setTextColor(Color.parseColor("#00E676"));
            amount.setTextSize(16f);
            amount.setPadding(0, 8, 0, 8);

            // Row 3: Subtitle (Dates)
            TextView dates = new TextView(this);
            dates.setText(item[2]);
            dates.setTextColor(Color.GRAY);
            dates.setTextSize(14f);

            card.addView(title);
            card.addView(amount);
            card.addView(dates);
            container.addView(card);
        }
    }
}