package com.example.gc_gymcare;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ExpiryWorker extends Worker {

    public ExpiryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // This code runs entirely in the background!

        try (DatabaseHelper db = new DatabaseHelper(getApplicationContext())) {

            // 1. Get the current time
            long currentTime = System.currentTimeMillis();

            // 2. You will need a simple method in your DatabaseHelper to count how many people are expired.
            // (Assuming you have or can make a quick db.getExpiredMembersCount() method)
            int expiredCount = db.getExpiredMembersCount(currentTime);

            // 3. If people are expired, fire the notification!
            if (expiredCount > 0) {
                NotificationHelper.showNotification(
                        getApplicationContext(),
                        "⚠️ GymCare Alert",
                        "You have " + expiredCount + " members whose plans have expired. Tap to check them!"
                );
            }

            return Result.success();

        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }
}