package com.example.gc_gymcare;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class GymHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym_history);

        LinearLayout container = findViewById(R.id.gymHistoryContainer);
        DatabaseHelper db = new DatabaseHelper(this);

        // Fetch the monthly grouped data
        ArrayList<String[]> historyList = db.getMonthlyGymStats();

        if (historyList.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No revenue data found yet.");
            emptyText.setTextColor(Color.GRAY);
            emptyText.setTextSize(16f);
            container.addView(emptyText);
            return;
        }

        // Dynamically build a beautiful card for every month
        for (String[] monthData : historyList) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.rounded_search_bg); // Reusing your beautiful rounded bg!
            card.setPadding(50, 40, 50, 40);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 30);
            card.setLayoutParams(params);

            // Row 1: Month & Year (e.g., "July 2026")
            TextView title = new TextView(this);
            title.setText(monthData[0]);
            title.setTextColor(Color.WHITE);
            title.setTextSize(20f);
            title.setTypeface(null, android.graphics.Typeface.BOLD);

            // Row 2: Total Revenue (Green)
            TextView revenue = new TextView(this);
            revenue.setText(monthData[1]);
            revenue.setTextColor(Color.parseColor("#00E676"));
            revenue.setTextSize(18f);
            revenue.setPadding(0, 12, 0, 4);
            revenue.setTypeface(null, android.graphics.Typeface.BOLD);

            // Row 3: Plans Sold (Gray)
            TextView plans = new TextView(this);
            plans.setText(monthData[2]);
            plans.setTextColor(Color.LTGRAY);
            plans.setTextSize(14f);

            card.addView(title);
            card.addView(revenue);
            card.addView(plans);
            container.addView(card);
        }
    }
}