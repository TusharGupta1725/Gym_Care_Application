package com.example.gc_gymcare;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MemberAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_MEMBER = 1;

    // Split Lists
    private final List<Member> activeMembers = new ArrayList<>();
    private final List<Member> expiredMembers = new ArrayList<>();

    // Section Expand/Collapse State
    private boolean isActiveExpanded = true;
    private boolean isExpiredExpanded = true;

    // Item Expand/Edit State
    private int expandedPosition = -1;
    private int editingPosition = -1;

    public MemberAdapter(List<Member> fullList) {
        long currentTime = System.currentTimeMillis();
        // Automatically sort members when the list loads!
        for (Member m : fullList) {
            if (m.getEndDate() > 0 && m.getEndDate() < currentTime) {
                expiredMembers.add(m);
            } else {
                activeMembers.add(m);
            }
        }
    }

    // ==========================================
    // SECTION MATH: Figure out what goes where
    // ==========================================
    @Override
    public int getItemCount() {
        int count = 2; // 2 Headers (Active and Expired)
        if (isActiveExpanded) count += activeMembers.size();
        if (isExpiredExpanded) count += expiredMembers.size();
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_HEADER; // First item is always Active Header
        int expiredHeaderPos = 1 + (isActiveExpanded ? activeMembers.size() : 0);
        if (position == expiredHeaderPos) return TYPE_HEADER; // Second Header
        return TYPE_MEMBER;
    }

    private Member getMemberAt(int position) {
        int expiredHeaderPos = 1 + (isActiveExpanded ? activeMembers.size() : 0);
        if (position < expiredHeaderPos) {
            return activeMembers.get(position - 1);
        } else {
            return expiredMembers.get(position - expiredHeaderPos - 1);
        }
    }

    // ==========================================
    // VIEW HOLDERS
    // ==========================================
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            // Dynamically build a beautiful Header Card
            LinearLayout header = new LinearLayout(parent.getContext());
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setBackgroundResource(R.drawable.rounded_search_bg);

            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 16, 0, 16);
            header.setLayoutParams(params);
            header.setPadding(40, 30, 40, 30);

            TextView title = new TextView(parent.getContext());
            title.setTextColor(Color.parseColor("#FF5722")); // Orange Header Title
            title.setTextSize(16f);
            title.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            title.setLayoutParams(titleParams);

            TextView icon = new TextView(parent.getContext());
            icon.setText("▼");
            icon.setTextColor(Color.WHITE);
            icon.setTextSize(16f);

            header.addView(title);
            header.addView(icon);

            return new HeaderViewHolder(header, title, icon);

        } else {
            // Standard Member Card
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false);
            return new MemberViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        // ==========================================
        // 1. IF IT IS A HEADER
        // ==========================================
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            int expiredHeaderPos = 1 + (isActiveExpanded ? activeMembers.size() : 0);

            if (position == 0) {
                // ACTIVE PLANS HEADER
                headerHolder.title.setText("Active Plans (" + activeMembers.size() + ")");
                headerHolder.icon.setRotation(isActiveExpanded ? 180 : 0); // Flip arrow up if open
                headerHolder.itemView.setOnClickListener(v -> {
                    isActiveExpanded = !isActiveExpanded;
                    expandedPosition = -1; // Close any open member menus
                    editingPosition = -1;
                    notifyDataSetChanged();
                });
            } else if (position == expiredHeaderPos) {
                // EXPIRED PLANS HEADER
                headerHolder.title.setText("Expired Plans (" + expiredMembers.size() + ")");
                headerHolder.icon.setRotation(isExpiredExpanded ? 180 : 0);
                headerHolder.itemView.setOnClickListener(v -> {
                    isExpiredExpanded = !isExpiredExpanded;
                    expandedPosition = -1;
                    editingPosition = -1;
                    notifyDataSetChanged();
                });
            }
            return;
        }

        // ==========================================
        // 2. IF IT IS A MEMBER CARD
        // ==========================================
        MemberViewHolder memberHolder = (MemberViewHolder) holder;
        Member member = getMemberAt(position);
        if (member == null) return;

        memberHolder.textMemberName.setText(member.getName());
        memberHolder.textMemberPhone.setText(member.getPhone());

        // --- Check Plan Expiry & Sweeper ---
        long currentTime = System.currentTimeMillis();
        boolean isExpired = member.getEndDate() > 0 && member.getEndDate() < currentTime;
        boolean isAutoMovedBySweeper = (member.getAmountPaid() == 0 && member.getRemainingBalance() == member.getPlanPrice());
        int currentBalance = member.getRemainingBalance();

        // --- ADVANCED STATUS UI ---
        if (currentBalance < 0) {
            int extraPaid = Math.abs(currentBalance);
            if (isExpired) {
                memberHolder.textPaymentStatus.setText("EXPIRED | EXTRA PAID: ₹" + extraPaid);
                memberHolder.textPaymentStatus.setTextColor(Color.parseColor("#F44336"));
                memberHolder.btnPayment.setVisibility(View.VISIBLE);
            } else {
                memberHolder.textPaymentStatus.setText("PAID | EXTRA PAID: ₹" + extraPaid);
                memberHolder.textPaymentStatus.setTextColor(Color.parseColor("#00E676"));
                memberHolder.btnPayment.setVisibility(View.GONE);
            }
        } else if (currentBalance == 0) {
            if (isExpired) {
                memberHolder.textPaymentStatus.setText("PLAN EXPIRED");
                memberHolder.textPaymentStatus.setTextColor(Color.parseColor("#F44336"));
                memberHolder.btnPayment.setVisibility(View.VISIBLE);
            } else {
                memberHolder.textPaymentStatus.setText("PAID");
                memberHolder.textPaymentStatus.setTextColor(Color.parseColor("#00E676"));
                memberHolder.btnPayment.setVisibility(View.GONE);
            }
        } else {
            if (isExpired) {
                if (isAutoMovedBySweeper) memberHolder.textPaymentStatus.setText("PLAN EXPIRED");
                else memberHolder.textPaymentStatus.setText("EXPIRED | DUE: ₹" + currentBalance);
                memberHolder.textPaymentStatus.setTextColor(Color.parseColor("#F44336"));
            } else {
                memberHolder.textPaymentStatus.setText("DUE: ₹" + currentBalance);
                memberHolder.textPaymentStatus.setTextColor(Color.parseColor("#FF5722"));
            }
            memberHolder.btnPayment.setVisibility(View.VISIBLE);
        }

        // --- EXPANDED MENU MORE INFO TEXT ---
        if (memberHolder.textExpandedDue != null) {
            if (currentBalance < 0) {
                memberHolder.textExpandedDue.setText("Extra Paid: ₹" + Math.abs(currentBalance));
                memberHolder.textExpandedDue.setTextColor(Color.parseColor("#00E676"));
            } else if (currentBalance > 0) {
                memberHolder.textExpandedDue.setText("Due: ₹" + currentBalance);
                memberHolder.textExpandedDue.setTextColor(Color.parseColor("#FF5722"));
            } else {
                memberHolder.textExpandedDue.setText("Due: ₹0");
                memberHolder.textExpandedDue.setTextColor(Color.GRAY);
            }
        }

        // --- EXPANDABLE MENU ANIMATION LOGIC ---
        final boolean isExpanded = position == expandedPosition;
        memberHolder.layoutExpandableMenu.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        memberHolder.itemView.setActivated(isExpanded);

        memberHolder.layoutMainRow.setOnClickListener(v -> {
            int currentPos = memberHolder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            if (editingPosition != -1) {
                editingPosition = -1;
                hideKeyboard(v);
            }
            TransitionManager.beginDelayedTransition((ViewGroup) memberHolder.itemView.getParent(), new AutoTransition());
            expandedPosition = isExpanded ? -1 : currentPos;
            notifyDataSetChanged(); // Ensures safe refresh across sections
        });

        // --- ACTION 1: INLINE QUICK RENAME ---
        final boolean isEditing = position == editingPosition;
        if (isEditing) {
            memberHolder.textMemberName.setVisibility(View.GONE);
            memberHolder.editMemberNameRow.setVisibility(View.VISIBLE);
            memberHolder.btnRename.setText("Save Name");
            memberHolder.btnRename.setTextColor(Color.parseColor("#00E676"));
        } else {
            memberHolder.textMemberName.setVisibility(View.VISIBLE);
            memberHolder.editMemberNameRow.setVisibility(View.GONE);
            memberHolder.btnRename.setText("Quick Rename");
            memberHolder.btnRename.setTextColor(Color.parseColor("#FFFFFF"));
        }

        memberHolder.btnRename.setOnClickListener(v -> {
            int currentPos = memberHolder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            if (isEditing) {
                String newName = memberHolder.editMemberNameRow.getText().toString().trim();
                if (!newName.isEmpty() && !newName.equals(member.getName())) {
                    member.setName(newName);
                    try (DatabaseHelper db = new DatabaseHelper(v.getContext())) {
                        db.updateMemberName(member.getPhone(), newName);
                    }
                    Toast.makeText(v.getContext(), "Name updated!", Toast.LENGTH_SHORT).show();
                }
                editingPosition = -1;
                hideKeyboard(v);
                notifyDataSetChanged();
            } else {
                editingPosition = currentPos;
                memberHolder.editMemberNameRow.setText(member.getName());
                notifyDataSetChanged();
                memberHolder.editMemberNameRow.requestFocus();
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(memberHolder.editMemberNameRow, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // --- ACTION 2: FULL EDIT SCREEN ---
        memberHolder.btnFullEdit.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, EditMemberActivity.class);
            intent.putExtra("PHONE", member.getPhone());
            intent.putExtra("NAME", member.getName());
            intent.putExtra("PAID", member.getAmountPaid());
            intent.putExtra("BALANCE", member.getRemainingBalance());
            intent.putExtra("END_DATE", member.getEndDate());
            context.startActivity(intent);
        });

        // --- ACTION 3: RECORD QUICK PAYMENT ---
        memberHolder.btnPayment.setOnClickListener(v -> {
            Context context = v.getContext();
            final EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setHint("Enter amount in ₹");

            new AlertDialog.Builder(context)
                    .setTitle("Receive Payment")
                    .setMessage("Current Balance: ₹" + member.getRemainingBalance())
                    .setView(input)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String amountStr = input.getText().toString().trim();
                        if (!amountStr.isEmpty()) {
                            int paymentReceived = Integer.parseInt(amountStr);
                            int newPaidTotal = member.getAmountPaid() + paymentReceived;
                            int newBalance = member.getRemainingBalance() - paymentReceived;

                            DatabaseHelper db = new DatabaseHelper(context);
                            db.updateMemberPayment(member.getPhone(), newPaidTotal, newBalance);

                            String ledgerTitle = (newBalance < 0) ? "Advance Payment Received" : "Payment Received";
                            db.logQuickPayment(member.getPhone(), paymentReceived, ledgerTitle);

                            member.setAmountPaid(newPaidTotal);
                            member.setRemainingBalance(newBalance);
                            notifyDataSetChanged(); // Safely updates their math

                            // 👇 NEW SMS TRIGGER WIRED DIRECTLY INTO THE BUTTON 👇
                            sendDuePaymentSMS(context, member.getPhone(), member.getName(), paymentReceived, newBalance);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // --- ACTION 4: DELETE CONTACT ---
        memberHolder.btnDelete.setOnClickListener(v -> {
            try (DatabaseHelper db = new DatabaseHelper(v.getContext())) {
                db.backupDatabase(v.getContext());
            }
            int currentPos = memberHolder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            new AlertDialog.Builder(v.getContext())
                    .setTitle(String.format(Locale.getDefault(), "Delete %s?", member.getName()))
                    .setMessage("Are you sure you want to completely remove this member?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        try (DatabaseHelper db = new DatabaseHelper(v.getContext())) {
                            db.deleteMember(member.getPhone());
                        }
                        // Remove from the correct list
                        if (activeMembers.contains(member)) activeMembers.remove(member);
                        else expiredMembers.remove(member);

                        expandedPosition = -1; // Reset UI
                        notifyDataSetChanged(); // Refresh entire list safely
                        Toast.makeText(v.getContext(), "Member deleted.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // ==========================================
    // VIEW HOLDER CLASSES
    // ==========================================

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView title, icon;
        public HeaderViewHolder(@NonNull View itemView, TextView title, TextView icon) {
            super(itemView);
            this.title = title;
            this.icon = icon;
        }
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView textMemberName, textMemberPhone, textPaymentStatus, textExpandedDue;
        EditText editMemberNameRow;
        ImageView imgProfilePic;
        LinearLayout layoutMainRow, layoutExpandableMenu;
        TextView btnRename, btnFullEdit, btnPayment, btnDelete;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            textMemberName = itemView.findViewById(R.id.textMemberName);
            editMemberNameRow = itemView.findViewById(R.id.editMemberNameRow);
            textMemberPhone = itemView.findViewById(R.id.textMemberPhone);
            textPaymentStatus = itemView.findViewById(R.id.textPaymentStatus);
            textExpandedDue = itemView.findViewById(R.id.textExpandedDue);
            imgProfilePic = itemView.findViewById(R.id.imgProfilePic);
            layoutMainRow = itemView.findViewById(R.id.layoutMainRow);
            layoutExpandableMenu = itemView.findViewById(R.id.layoutExpandableMenu);
            btnRename = itemView.findViewById(R.id.btnRename);
            btnFullEdit = itemView.findViewById(R.id.btnFullEdit);
            btnPayment = itemView.findViewById(R.id.btnPayment);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    // ==========================================
    // ADAPTER DUE PAYMENT SMS ENGINE
    // ==========================================
    private void sendDuePaymentSMS(android.content.Context context, String phone, String name, int amountPaid, int remainingBalance) {
        try {
            String cleanPhone = phone.replaceAll("\\D+", "");
            if (cleanPhone.length() < 10) return;

            String message = "Payment Received, " + name + "!\nYou submitted Rupees " + amountPaid + " in your remaining due.\nNew remaining due is Rupees " + remainingBalance + ".\nThank you! - GymCare";

            android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
            if (smsManager != null) {
                smsManager.sendTextMessage(cleanPhone, null, message, null, null);
                android.widget.Toast.makeText(context, "✅ Due Payment Text Transmitted!", android.widget.Toast.LENGTH_SHORT).show();
            }

        } catch (Throwable t) {
            android.widget.Toast.makeText(context, "SMS Engine Blocked: " + t.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            t.printStackTrace();
        }
    }
}