package com.example.gc_gymcare;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import java.util.List;

public class SupplementAdapter extends RecyclerView.Adapter<SupplementAdapter.SuppViewHolder> {

    private final List<SupplementCustomer> customerList;
    private int expandedPosition = -1;

    public SupplementAdapter(List<SupplementCustomer> customerList) {
        this.customerList = customerList;
    }

    @NonNull
    @Override
    public SuppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_supplement, parent, false);
        return new SuppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuppViewHolder holder, int position) {
        SupplementCustomer customer = customerList.get(position);

        holder.textSuppName.setText(customer.getName());
        holder.textSuppPhone.setText(customer.getPhone());

        int bal = customer.getBalance();

        Context context = holder.itemView.getContext();

        // Main Visible Row UI
        if (bal < 0) {
            holder.textSuppStatus.setText(context.getString(R.string.advance_label, Math.abs(bal)));
            holder.textSuppStatus.setTextColor(Color.parseColor("#00E676"));
            holder.textSuppExpandedDue.setText(context.getString(R.string.expanded_advance, Math.abs(bal)));
        } else if (bal == 0) {
            holder.textSuppStatus.setText(context.getString(R.string.paid_in_full));
            holder.textSuppStatus.setTextColor(Color.parseColor("#00E676"));
            holder.textSuppExpandedDue.setText(context.getString(R.string.all_dues_clear));
        } else {
            holder.textSuppStatus.setText(context.getString(R.string.due_label, bal));
            holder.textSuppStatus.setTextColor(Color.parseColor("#FF5722"));
            holder.textSuppExpandedDue.setText(context.getString(R.string.remaining_amount, bal));
        }

        // --- EXPANDABLE MENU ANIMATION LOGIC ---
        final boolean isExpanded = position == expandedPosition;
        holder.layoutSuppExpandableMenu.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.itemView.setActivated(isExpanded);

        holder.layoutSuppMainRow.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView.getParent(), new AutoTransition());
            int oldExpanded = expandedPosition;
            expandedPosition = isExpanded ? -1 : currentPos;
            if (oldExpanded != -1) notifyItemChanged(oldExpanded);
            if (expandedPosition != -1) notifyItemChanged(expandedPosition);
        });
        // --- NEW ACTION: BUY ANOTHER ITEM ---
        holder.btnSuppBuy.setOnClickListener(v -> {
            // Build a fast input form programmatically
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(60, 40, 60, 40);

            EditText editItem = new EditText(context);
            editItem.setHint("Item Name (e.g. Creatine)");

            EditText editPrice = new EditText(context);
            editPrice.setHint("Total Price (₹)");
            editPrice.setInputType(InputType.TYPE_CLASS_NUMBER);

            EditText editPaid = new EditText(context);
            editPaid.setHint("Amount Paid Now (₹)");
            editPaid.setInputType(InputType.TYPE_CLASS_NUMBER);

            layout.addView(editItem);
            layout.addView(editPrice);
            layout.addView(editPaid);

            new AlertDialog.Builder(context)
                    .setTitle("Buy New Item for " + customer.getName())
                    .setView(layout)
                    .setPositiveButton("Save Sale", (dialog, which) -> {
                        String itemName = editItem.getText().toString().trim();
                        String priceStr = editPrice.getText().toString().trim();
                        String paidStr = editPaid.getText().toString().trim();

                        if (!itemName.isEmpty() && !priceStr.isEmpty()) {
                            int price = Integer.parseInt(priceStr);
                            int paid = paidStr.isEmpty() ? 0 : Integer.parseInt(paidStr);

                            // The DB engine we built automatically updates their running totals!
                            try (DatabaseHelper db = new DatabaseHelper(context)) {
                                db.processSupplementSale(customer.getName(), customer.getPhone(), itemName, price, paid);
                            }

                            // Update local math so the screen refreshes instantly
                            customer.setBalance(customer.getBalance() + (price - paid));
                            notifyItemChanged(position);
                            Toast.makeText(context, "New Purchase Logged!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Item Name and Price are required", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // --- ACTION 1: RECEIVE PIECE PAYMENT ---
        holder.btnSuppPayment.setOnClickListener(v -> {
            final EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setHint("Enter amount received in ₹");

            new AlertDialog.Builder(context)
                    .setTitle("Receive Supplement Payment")
                    .setMessage("Current Due: ₹" + Math.max(bal, 0))
                    .setView(input)
                    .setPositiveButton("Save Payment", (dialog, which) -> {
                        String amountStr = input.getText().toString().trim();
                        if (!amountStr.isEmpty()) {
                            int paid = Integer.parseInt(amountStr);

                            try (DatabaseHelper db = new DatabaseHelper(context)) {
                                db.updateSupplementPayment(customer.getPhone(), paid);

                                String title = (bal - paid < 0) ? "Supplements Advance" : "Supplements Due Paid";
                                db.logQuickPayment(customer.getPhone(), paid, title);
                            }

                            customer.setBalance(bal - paid);
                            notifyItemChanged(position);
                            Toast.makeText(context, "Piece Payment Logged!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // --- ACTION 2: VIEW HISTORY ---
        holder.btnSuppHistory.setOnClickListener(v -> {
            Intent intent = new Intent(context, SuppHistoryActivity.class);
            intent.putExtra("PHONE", customer.getPhone());
            intent.putExtra("NAME", customer.getName());
            context.startActivity(intent);
        });

        // --- ACTION 3: DELETE CONTACT ---
        holder.btnSuppDelete.setOnClickListener(v -> {
            try (DatabaseHelper db = new DatabaseHelper(v.getContext())) {
                db.backupDatabase(v.getContext());
            }
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete " + customer.getName() + "?")
                    .setMessage("Are you sure? Their payment history will still be safe in the gym ledger.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        try (DatabaseHelper db = new DatabaseHelper(v.getContext())) {
                            db.getWritableDatabase().delete("supplements_customers", "phone=?", new String[]{customer.getPhone()});
                        }
                        customerList.remove(currentPos);
                        int oldExpanded = expandedPosition;
                        expandedPosition = -1;
                        notifyItemRemoved(currentPos);
                        if (oldExpanded != -1 && oldExpanded != currentPos) {
                            notifyItemChanged(oldExpanded);
                        }
                        Toast.makeText(v.getContext(), "Contact deleted.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() { return customerList.size(); }

    public static class SuppViewHolder extends RecyclerView.ViewHolder {
        TextView textSuppName, textSuppPhone, textSuppStatus, textSuppExpandedDue;
        LinearLayout layoutSuppMainRow, layoutSuppExpandableMenu;
        Button btnSuppBuy, btnSuppPayment, btnSuppHistory, btnSuppDelete;
        public SuppViewHolder(@NonNull View itemView) {
            super(itemView);
            textSuppName = itemView.findViewById(R.id.textSuppName);
            textSuppPhone = itemView.findViewById(R.id.textSuppPhone);
            textSuppStatus = itemView.findViewById(R.id.textSuppStatus);
            textSuppExpandedDue = itemView.findViewById(R.id.textSuppExpandedDue);
            layoutSuppMainRow = itemView.findViewById(R.id.layoutSuppMainRow);
            layoutSuppExpandableMenu = itemView.findViewById(R.id.layoutSuppExpandableMenu);
            btnSuppPayment = itemView.findViewById(R.id.btnSuppPayment);
            btnSuppHistory = itemView.findViewById(R.id.btnSuppHistory);
            btnSuppDelete = itemView.findViewById(R.id.btnSuppDelete);
            btnSuppBuy = itemView.findViewById(R.id.btnSuppBuy);
        }

    }

}
