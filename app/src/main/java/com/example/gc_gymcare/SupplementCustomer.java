package com.example.gc_gymcare;

public class SupplementCustomer {
    private String name;
    private String phone;
    private int amountPaid;
    private int balance;

    public SupplementCustomer(String name, String phone, int amountPaid, int balance) {
        this.name = name;
        this.phone = phone;
        this.amountPaid = amountPaid;
        this.balance = balance;
    }

    public String getName() { return name; }
    public String getPhone() { return phone; }
    public int getAmountPaid() { return amountPaid; }
    public int getBalance() { return balance; }

    public void setAmountPaid(int amountPaid) { this.amountPaid = amountPaid; }
    public void setBalance(int balance) { this.balance = balance; }
}