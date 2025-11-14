package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.PendingTransactionAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction; // Đảm bảo import đúng class Transaction của bạn

public class AdminConfirmDepositActivity extends AppCompatActivity implements PendingTransactionAdapter.OnConfirmClickListener {

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private PendingTransactionAdapter adapter;
    private ProgressBar progressBar;

    private static final String TAG = "AdminConfirm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_confirm_deposit); // Thay bằng layout của bạn

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recycler_view_pending_transactions); // Thay bằng ID của bạn
        progressBar = findViewById(R.id.progress_bar_admin_confirm); // Thay bằng ID của bạn

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        Query query = db.collection("transactions")
                .whereEqualTo("status", "PENDING")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<com.example.myapplication.Transaction> options = new FirestoreRecyclerOptions.Builder<com.example.myapplication.Transaction>()
                .setQuery(query, com.example.myapplication.Transaction.class)
                .build();

        adapter = new PendingTransactionAdapter(options);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // **QUAN TRỌNG:** Gán listener cho adapter
        adapter.setOnConfirmClickListener(this);
    }

    /**
     * Đây là hàm được triển khai từ interface trong Adapter.
     * Nó sẽ được gọi khi nút "Xác nhận" trên một item được nhấn.
     */
    @Override
    public void onConfirmClick(DocumentSnapshot documentSnapshot, com.example.myapplication.Transaction transaction) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận Giao dịch")
                .setMessage(String.format("Bạn có chắc chắn muốn xác nhận nạp %,d VNĐ cho người dùng có mã %s không?",
                        transaction.getAmount(), transaction.getPayment_code()))
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    // Gọi hàm thực thi transaction
                    executeConfirmationTransaction(documentSnapshot.getId(), transaction.getUser_uid(), transaction.getAmount());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Thực thi transaction để cộng tiền cho user và cập nhật trạng thái giao dịch.
     */
    private void executeConfirmationTransaction(String transactionId, String userId, long amount) {
        progressBar.setVisibility(View.VISIBLE);

        final DocumentReference transactionRef = db.collection("transactions").document(transactionId);
        final DocumentReference userRef = db.collection("users").document(userId);

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) firestoreTransaction -> {
            // Bước 1: Đọc document của người dùng (bắt buộc trong transaction)
            DocumentSnapshot userSnapshot = firestoreTransaction.get(userRef);
            if (!userSnapshot.exists()) {
                throw new FirebaseFirestoreException("Không tìm thấy người dùng!",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            // Bước 2: Cập nhật số dư của người dùng
            firestoreTransaction.update(userRef, "wallet_balance", FieldValue.increment(amount));

            // Bước 3: Cập nhật trạng thái của giao dịch
            firestoreTransaction.update(transactionRef, "status", "COMPLETED");

            return null; // Transaction thành công
        }).addOnSuccessListener(aVoid -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Xác nhận nạp tiền thành công!", Toast.LENGTH_SHORT).show();
            // Adapter sẽ tự động xóa item khỏi danh sách vì status không còn là "PENDING"
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Log.e(TAG, "Transaction failed: ", e);
            Toast.makeText(this, "Giao dịch thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}
