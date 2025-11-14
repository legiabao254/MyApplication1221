package com.example.myapplication;

import com.google.firebase.Timestamp;

public class Transaction {
    // Tên các trường phải khớp với tên trong Firestore
    private String user_uid;
    private long amount;
    private String type;
    private String status;
    private String payment_code;
    private Timestamp timestamp;

    // Cần một constructor rỗng cho Firestore
    public Transaction() {}

    // Getters
    public String getUser_uid() { return user_uid; }
    public long getAmount() { return amount; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public String getPayment_code() { return payment_code; }
    public Timestamp getTimestamp() { return timestamp; }
}
