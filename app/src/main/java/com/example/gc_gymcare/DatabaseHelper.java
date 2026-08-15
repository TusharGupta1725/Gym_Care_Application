package com.example.gc_gymcare;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;


public class DatabaseHelper extends SQLiteOpenHelper {


// Inside your DatabaseHelper class:

    // ==========================================
// BULLETPROOF BACKUP ENGINE (Saves to External Storage)
// ==========================================
    // ==========================================
    // BULLETPROOF BACKUP ENGINE (With WAL Checkpoint)
    // ==========================================
    public void backupDatabase(Context context) {
        // 1. CRITICAL: Force the hidden -wal file to merge into the main .db file instantly
        try (android.database.sqlite.SQLiteDatabase db = this.getWritableDatabase()) {
            db.rawQuery("PRAGMA wal_checkpoint(FULL);", null).close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        File backupDir = new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "GymCare_Backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        File currentDB = context.getDatabasePath(DATABASE_NAME);
        File backupDB = new File(backupDir, DATABASE_NAME + "_backup.db");

        if (currentDB.exists()) {
            try (java.nio.channels.FileChannel src = new java.io.FileInputStream(currentDB).getChannel();
                 java.nio.channels.FileChannel dst = new java.io.FileOutputStream(backupDB).getChannel()) {
                dst.transferFrom(src, 0, src.size());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ==========================================
// AUTO-RESTORE ENGINE (Pulls back data on Reinstall)
// ==========================================
    // ==========================================
    // AUTO-RESTORE ENGINE (Clears WAL Corruption)
    // ==========================================
    public boolean restoreDatabase(Context context) {
        File currentDB = context.getDatabasePath(DATABASE_NAME);

        // 1. Look in both possible backup locations
        File backupDir1 = new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "GymCare_Backups");
        File backupDB1 = new File(backupDir1, DATABASE_NAME + "_backup.db");

        File backupDir2 = new File(context.getExternalFilesDir(null).getParentFile().getParentFile().getParentFile().getParentFile(), "Documents/GymCare_Backups");
        File backupDB2 = new File(backupDir2, DATABASE_NAME + "_backup.db");

        File backupToUse = null;
        if (backupDB1.exists()) backupToUse = backupDB1;
        else if (backupDB2.exists()) backupToUse = backupDB2;

        if (backupToUse != null) {
            try {
                // 2. CRITICAL: Delete the temporary WAL/SHM files to prevent corruption!
                File walFile = new File(currentDB.getPath() + "-wal");
                File shmFile = new File(currentDB.getPath() + "-shm");
                if (walFile.exists()) walFile.delete();
                if (shmFile.exists()) shmFile.delete();

                // 3. Overwrite the main database file
                try (FileChannel src = new FileInputStream(backupToUse).getChannel();
                     FileChannel dst = new FileOutputStream(currentDB).getChannel()) {
                    dst.transferFrom(src, 0, src.size());
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static final String DATABASE_NAME = "GymCareDB";
    private static final int DATABASE_VERSION = 7; // UPGRADED FOR SUPPLEMENTS CRM

    // Member Tables
    private static final String TABLE_MEMBERS = "members";
    // Supplement Tables
    private static final String TABLE_SUPPLEMENTS = "supplements_customers";
    private static final String TABLE_SUPP_HISTORY = "supplements_history";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Members
        db.execSQL("CREATE TABLE " + TABLE_MEMBERS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phone TEXT, planPrice INTEGER, amountPaid INTEGER, balance INTEGER, endDate INTEGER, expireLogged INTEGER DEFAULT 0)");

        // 2. Member Financial Ledger
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, phone TEXT, isPlan INTEGER, title TEXT, amount INTEGER, startDate INTEGER, endDate INTEGER, timestamp INTEGER)");

        // 3. NEW: Supplement Customers (Only tracks current Debt/Advance)
        db.execSQL("CREATE TABLE " + TABLE_SUPPLEMENTS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phone TEXT, amountPaid INTEGER, balance INTEGER)");

        // 4. NEW: Supplement Purchase Ledger (Tracks exactly what they bought)
        db.execSQL("CREATE TABLE " + TABLE_SUPP_HISTORY + " (id INTEGER PRIMARY KEY AUTOINCREMENT, phone TEXT, itemName TEXT, itemPrice INTEGER, amountPaid INTEGER, timestamp INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEMBERS);
        db.execSQL("DROP TABLE IF EXISTS transactions");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUPPLEMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUPP_HISTORY);
        onCreate(db);
    }

    // ==========================================
    // CORE MEMBER METHODS (UNCHANGED)
    // ==========================================
    public void addMember(Member member) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", member.getName());
        values.put("phone", member.getPhone());
        values.put("planPrice", member.getPlanPrice());
        values.put("amountPaid", member.getAmountPaid());
        values.put("balance", member.getRemainingBalance());
        values.put("endDate", member.getEndDate());
        values.put("expireLogged", 0);
        db.insert(TABLE_MEMBERS, null, values);
    }

    public List<Member> getMembers(boolean isPaidTab) {
        List<Member> memberList = new ArrayList<>();
        String query = isPaidTab ? "SELECT * FROM " + TABLE_MEMBERS + " WHERE balance <= 0" : "SELECT * FROM " + TABLE_MEMBERS + " WHERE balance > 0";
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(query, null)) {
            if (cursor.moveToFirst()) {
                do {
                    memberList.add(new Member(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3), cursor.getInt(4), cursor.getInt(5), cursor.getLong(6)));
                } while (cursor.moveToNext());
            }
        }
        return memberList;
    }

    public void updateFullMemberDetails(String originalPhone, String newName, String newPhone, int newPlan, int newPaid, int newBalance, long newEndDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", newName);
        values.put("phone", newPhone);
        values.put("planPrice", newPlan);
        values.put("amountPaid", newPaid);
        values.put("balance", newBalance);
        values.put("endDate", newEndDate);
        values.put("expireLogged", 0);
        db.update(TABLE_MEMBERS, values, "phone=?", new String[]{originalPhone});
    }

    public void updateMemberPayment(String phone, int newAmountPaid, int newBalance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("amountPaid", newAmountPaid);
        values.put("balance", newBalance);
        db.update(TABLE_MEMBERS, values, "phone=?", new String[]{phone});
    }

    public void updateMemberName(String phone, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", newName);
        db.update(TABLE_MEMBERS, values, "phone=?", new String[]{phone});
    }

    public boolean isMemberExpired(long endDate) {
        return endDate != 0 && endDate < System.currentTimeMillis();
    }

    public ArrayList<String> getMemberHistory(String phone) {
        ArrayList<String> history = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT title, amount, timestamp FROM transactions WHERE phone=? ORDER BY timestamp DESC", new String[]{phone})) {
            if (cursor.moveToFirst()) {
                do {
                    String title = cursor.getString(0);
                    int amount = cursor.getInt(1);
                    long timestamp = cursor.getLong(2);
                    String date = sdf.format(new Date(timestamp));
                    history.add(title + " - ₹" + amount + " (" + date + ")");
                } while (cursor.moveToNext());
            }
        }
        return history;
    }

    public void deleteMember(String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        // 1. Delete their active profile so they disappear from the app lists
        db.delete(TABLE_MEMBERS, "phone=?", new String[]{phone});

        // ❌ WE REMOVED THE LINE THAT DELETED THEIR TRANSACTIONS!
        // Now, their historical payments stay permanently in the master ledger,
        // meaning your Gym Dashboard revenue will never drop.
    }

    // --- DASHBOARD MATH (UNCHANGED) ---
    public int getTotalMembersCount() {
        long currentTime = System.currentTimeMillis();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_MEMBERS + " WHERE endDate > ?", new String[]{String.valueOf(currentTime)})) {
            if (cursor.moveToFirst()) return cursor.getInt(0);
        }
        return 0;
    }

    public int getTotalAmountReceived() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        long startOfMonth = cal.getTimeInMillis();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM transactions WHERE timestamp >= ?", new String[]{String.valueOf(startOfMonth)})) {
            if (cursor.moveToFirst()) return cursor.getInt(0);
        }
        return 0;
    }

    public int getTotalAmountDue() {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT SUM(balance) FROM " + TABLE_MEMBERS + " WHERE balance > 0", null)) {
            if (cursor.moveToFirst()) return cursor.getInt(0);
        }
        return 0;
    }

    public ArrayList<String> processExpirations() {
        ArrayList<String> expiredNames = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        String query = "SELECT name, phone, planPrice, balance FROM " + TABLE_MEMBERS + " WHERE endDate < ? AND endDate != 0 AND expireLogged = 0";

        SQLiteDatabase db = this.getWritableDatabase();
        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(currentTime)})) {
            if (cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(0);
                    String phone = cursor.getString(1);
                    int planPrice = cursor.getInt(2);
                    int balance = cursor.getInt(3);

                    expiredNames.add(name);

                    String logDesc;
                    if (balance < 0) logDesc = "Plan Expired (Extra Paid: ₹" + Math.abs(balance) + ")";
                    else if (balance > 0) logDesc = "Plan Expired (Due: ₹" + balance + ")";
                    else logDesc = "Plan Expired (No Due)";

                    logSystemEvent(phone, logDesc);

                    ContentValues values = new ContentValues();
                    values.put("expireLogged", 1);
                    if (balance == 0) {
                        values.put("balance", planPrice);
                        values.put("amountPaid", 0);
                    }
                    db.update(TABLE_MEMBERS, values, "phone=?", new String[]{phone});
                } while (cursor.moveToNext());
            }
        }
        return expiredNames;
    }

    // --- FINANCIAL TRANSACTIONS (UNCHANGED) ---
    public void logPlanPurchase(String phone, String planName, long startDate, long endDate, int pricePaid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("phone", phone);
        values.put("isPlan", 1);
        values.put("title", planName);
        values.put("amount", pricePaid);
        values.put("startDate", startDate);
        values.put("endDate", endDate);
        values.put("timestamp", System.currentTimeMillis());
        db.insert("transactions", null, values);
    }

    public void logQuickPayment(String phone, int amountPaid, String paymentTitle) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("phone", phone);
        values.put("isPlan", 0);
        values.put("title", paymentTitle);
        values.put("amount", amountPaid);
        values.put("startDate", 0);
        values.put("endDate", 0);
        values.put("timestamp", System.currentTimeMillis());
        db.insert("transactions", null, values);
    }

    public void logSystemEvent(String phone, String eventTitle) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("phone", phone);
        values.put("isPlan", 0);
        values.put("title", eventTitle);
        values.put("amount", 0);
        values.put("startDate", 0);
        values.put("endDate", 0);
        values.put("timestamp", System.currentTimeMillis());
        db.insert("transactions", null, values);
    }

    public ArrayList<String[]> getMemberHistoryFull(String phone) {
        ArrayList<String[]> list = new ArrayList<>();
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT isPlan, title, amount, startDate, endDate, timestamp FROM transactions WHERE phone=? ORDER BY timestamp DESC", new String[]{phone})) {
            if (cursor.moveToFirst()) {
                do {
                    int isPlan = cursor.getInt(0);
                    String title = cursor.getString(1);
                    int amount = cursor.getInt(2);
                    String subtitle = (isPlan == 1) ? "Valid: " + dateFmt.format(new Date(cursor.getLong(3))) + " - " + dateFmt.format(new Date(cursor.getLong(4))) : "Recorded on: " + timeFmt.format(new Date(cursor.getLong(5)));
                    String displayAmount = (amount > 0) ? "₹" + amount : "";
                    list.add(new String[]{title, displayAmount, subtitle});
                } while (cursor.moveToNext());
            }
        }
        return list;
    }

    public ArrayList<String[]> getMonthlyGymStats() {
        ArrayList<String[]> monthlyStats = new ArrayList<>();
        java.util.LinkedHashMap<String, int[]> monthMap = new java.util.LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT timestamp, amount, isPlan FROM transactions ORDER BY timestamp DESC", null)) {
            if (cursor.moveToFirst()) {
                do {
                    long timestamp = cursor.getLong(0);
                    int amount = cursor.getInt(1);
                    int isPlan = cursor.getInt(2);
                    String monthYear = sdf.format(new Date(timestamp));
                    if (!monthMap.containsKey(monthYear)) monthMap.put(monthYear, new int[]{0, 0});
                    int[] stats = monthMap.get(monthYear);
                    stats[0] += amount;
                    if (isPlan == 1) stats[1] += 1;
                } while (cursor.moveToNext());
            }
        }
        for (java.util.Map.Entry<String, int[]> entry : monthMap.entrySet()) {
            monthlyStats.add(new String[]{entry.getKey(), "Revenue: ₹" + entry.getValue()[0], "Plans Sold: " + entry.getValue()[1]});
        }
        return monthlyStats;
    }

    // ==========================================
    // NEW: SUPPLEMENTS CRM ENGINE
    // ==========================================

    // Check if customer exists, otherwise add them. Then log the purchase.
    public void processSupplementSale(String name, String phone, String itemName, int itemPrice, int amountPaid) {
        SQLiteDatabase db = this.getWritableDatabase();
        // 1. Math
        int balanceCreated = itemPrice - amountPaid;

        // 2. Check if they already exist in Supplements Table
        try (Cursor cursor = db.rawQuery("SELECT amountPaid, balance FROM " + TABLE_SUPPLEMENTS + " WHERE phone=?", new String[]{phone})) {
            if (cursor.moveToFirst()) {
                // They exist! Update their running totals
                int oldPaid = cursor.getInt(0);
                int oldBalance = cursor.getInt(1);

                ContentValues values = new ContentValues();
                values.put("amountPaid", oldPaid + amountPaid);
                values.put("balance", oldBalance + balanceCreated);
                db.update(TABLE_SUPPLEMENTS, values, "phone=?", new String[]{phone});
            } else {
                // New Customer!
                ContentValues values = new ContentValues();
                values.put("name", name);
                values.put("phone", phone);
                values.put("amountPaid", amountPaid);
                values.put("balance", balanceCreated);
                db.insert(TABLE_SUPPLEMENTS, null, values);
            }
        }

        // 3. Log it in the Supplement History Ledger
        ContentValues historyValues = new ContentValues();
        historyValues.put("phone", phone);
        historyValues.put("itemName", itemName);
        historyValues.put("itemPrice", itemPrice);
        historyValues.put("amountPaid", amountPaid);
        historyValues.put("timestamp", System.currentTimeMillis());
        db.insert(TABLE_SUPP_HISTORY, null, historyValues);

        // 4. (Optional) Log the money into the Master Gym Revenue Ledger too!
        if (amountPaid > 0) {
            ContentValues gymLedger = new ContentValues();
            gymLedger.put("phone", phone);
            gymLedger.put("isPlan", 0); // 0 because it's not a gym membership
            gymLedger.put("title", "Supplement: " + itemName);
            gymLedger.put("amount", amountPaid);
            gymLedger.put("timestamp", System.currentTimeMillis());
            db.insert("transactions", null, gymLedger);
        }
    }

    public List<SupplementCustomer> getSupplementCustomers(boolean isPaidTab) {
        List<SupplementCustomer> list = new ArrayList<>();
        // Negative balance = Advance. So <= 0 goes to Paid list!
        String query = isPaidTab ? "SELECT name, phone, amountPaid, balance FROM " + TABLE_SUPPLEMENTS + " WHERE balance <= 0" :
                "SELECT name, phone, amountPaid, balance FROM " + TABLE_SUPPLEMENTS + " WHERE balance > 0";

        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(query, null)) {
            if (cursor.moveToFirst()) {
                do {
                    list.add(new SupplementCustomer(cursor.getString(0), cursor.getString(1), cursor.getInt(2), cursor.getInt(3)));
                } while (cursor.moveToNext());
            }
        }
        return list;
    }
    // Updates a customer's supplement debt when they make a partial payment
    public void updateSupplementPayment(String phone, int amountReceived) {
        SQLiteDatabase db = this.getWritableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT amountPaid, balance FROM " + TABLE_SUPPLEMENTS + " WHERE phone=?", new String[]{phone})) {
            if (cursor.moveToFirst()) {
                int oldPaid = cursor.getInt(0);
                int oldBalance = cursor.getInt(1);

                ContentValues values = new ContentValues();
                values.put("amountPaid", oldPaid + amountReceived);
                values.put("balance", oldBalance - amountReceived);
                db.update(TABLE_SUPPLEMENTS, values, "phone=?", new String[]{phone});
            }
        }
    }
    // ==========================================
    // SUPPLEMENT HISTORY TIMELINE
    // ==========================================
    // ==========================================
    // SUPPLEMENT HISTORY (WITH RUNNING DUE CALCULATION)
    // ==========================================
    public ArrayList<String[]> getSupplementPurchasesAndPayments(String phone) {
        // A temporary object just to hold the raw timeline data
        class SuppTx {
            boolean isPurchase; String title; int price; int paid; long timestamp;
            public SuppTx(boolean isPur, String t, int pr, int pa, long ts) {
                isPurchase = isPur; title = t; price = pr; paid = pa; timestamp = ts;
            }
        }
        ArrayList<SuppTx> rawList = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        // 1. Grab all Purchases
        try (Cursor c1 = db.rawQuery("SELECT itemName, itemPrice, amountPaid, timestamp FROM " + TABLE_SUPP_HISTORY + " WHERE phone=?", new String[]{phone})) {
            if (c1.moveToFirst()) do {
                rawList.add(new SuppTx(true, c1.getString(0), c1.getInt(1), c1.getInt(2), c1.getLong(3)));
            } while (c1.moveToNext());
        }

        // 2. Grab all Piece Payments
        try (Cursor c2 = db.rawQuery("SELECT title, amount, timestamp FROM transactions WHERE phone=? AND (title='Supplements Due Paid' OR title='Supplements Advance')", new String[]{phone})) {
            if (c2.moveToFirst()) do {
                rawList.add(new SuppTx(false, c2.getString(0), 0, c2.getInt(1), c2.getLong(2)));
            } while (c2.moveToNext());
        }

        // 3. Sort from OLDEST to NEWEST (so we can calculate the math in real-time)
        java.util.Collections.sort(rawList, (a, b) -> Long.compare(a.timestamp, b.timestamp));

        ArrayList<String[]> finalHistory = new ArrayList<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault());
        int runningDue = 0; // Starts at 0 for every customer

        // 4. Calculate the Running Ledger
        for (SuppTx tx : rawList) {
            String timeStr = sdf.format(new java.util.Date(tx.timestamp));

            if (tx.isPurchase) {
                // Bought something: Add to their debt, minus what they paid today
                runningDue += (tx.price - tx.paid);
                String dueText = (runningDue <= 0) ? "No Due" : "Due Remaining: ₹" + runningDue;

                finalHistory.add(new String[]{
                        "Bought: " + tx.title,
                        "Price: ₹" + tx.price + " | Paid Now: ₹" + tx.paid + "\n" + dueText,
                        timeStr
                });
            } else {
                // Piece Payment: Subtract from their debt
                runningDue -= tx.paid;
                String dueText = (runningDue <= 0) ? "No Due" : "Due Remaining: ₹" + runningDue;

                finalHistory.add(new String[]{
                        "Piece Payment Received",
                        "Amount Paid: ₹" + tx.paid + "\n" + dueText,
                        timeStr
                });
            }
        }

        // 5. Flip the list so the NEWEST transaction is always at the top of the screen!
        java.util.Collections.reverse(finalHistory);
        return finalHistory;
    }

    // ==========================================
    // NOTIFICATION WORKER HELPER METHOD
    // ==========================================
    public int getExpiredMembersCount(long currentTime) {
        int count = 0;
        android.database.sqlite.SQLiteDatabase db = this.getReadableDatabase();

        // ⚠️ IMPORTANT: Change "members" and "endDate" below to match the EXACT
        // table name and column name you defined at the top of your DatabaseHelper!
        String tableName = "members";       // e.g., TABLE_NAME or "members_table"
        String endDateColumn = "endDate";   // e.g., COLUMN_END_DATE or "end_date"

        String query = "SELECT COUNT(*) FROM " + tableName +
                " WHERE " + endDateColumn + " > 0 AND " + endDateColumn + " < ?";

        try (android.database.Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(currentTime)})) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0); // Gets the total count from the query
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return count;
    }
}