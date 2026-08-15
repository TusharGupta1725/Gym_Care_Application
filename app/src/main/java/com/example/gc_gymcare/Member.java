package com.example.gc_gymcare;

public class Member {
    private int id;
    private String name;
    private String phone;
    private int planPrice;
    private int amountPaid;
    private int remainingBalance;
    private long endDate;

    // Constructor for creating a new member
    public Member(String name, String phone, int planPrice, int amountPaid, int remainingBalance, long endDate) {
        this.name = name;
        this.phone = phone;
        this.planPrice = planPrice;
        this.amountPaid = amountPaid;
        this.remainingBalance = remainingBalance;
        this.endDate = endDate;
    }

    // Constructor for reading from the database
    public Member(int id, String name, String phone, int planPrice, int amountPaid, int remainingBalance, long endDate) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.planPrice = planPrice;
        this.amountPaid = amountPaid;
        this.remainingBalance = remainingBalance;
        this.endDate = endDate;
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public int getPlanPrice() { return planPrice; }
    public void setPlanPrice(int planPrice) { this.planPrice = planPrice; }
    public int getAmountPaid() { return amountPaid; }
    public void setAmountPaid(int amountPaid) { this.amountPaid = amountPaid; }
    public int getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(int remainingBalance) { this.remainingBalance = remainingBalance; }
    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }
}