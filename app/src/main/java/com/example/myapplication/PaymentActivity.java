package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PaymentActivity extends AppCompatActivity {
    private static final String TAG = "PaymentActivity";
    private static final long PRICE_PER_HOUR = 5000; // GIÁ GỬI XE: 5,000 VNĐ / giờ

    // Views
    private TextView tvPaymentSlotName, tvPaymentLicensePlate, tvPaymentVehicleType,
            tvPaymentStartTime, tvPaymentEndTime, tvPaymentDuration,
            tvPaymentTotalFee, tvPaymentUser;
    private Button btnConfirmPayment, btnCancelPayment;
    private View loadingOverlay; // View để hiển thị khi đang xử lý

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DocumentReference currentUserRef;
    private ListenerRegistration userBalanceListener;

    // Dữ liệu cần cho thanh toán
    private long currentUserBalance = 0L;
    private long totalCost = 0L;
    private String slotId, ticketId, historyId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        setTitle("Xác nhận Thanh toán");

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        mapViews();
        disablePaymentButton("Đang tải hóa đơn...");

        Intent intent = getIntent();
        // CHỈ CẦN 2 THÔNG TIN NÀY TỪ PARKINGSLOTACTIVITY
        slotId = intent.getStringExtra("SLOT_ID");
        String slotName = intent.getStringExtra("SLOT_NAME");

        if (slotId == null || slotName == null) {
            showErrorAndFinish("Lỗi: Không có thông tin vị trí đỗ.");
            return;
        }

        tvPaymentSlotName.setText("Vị trí: " + slotName);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserRef = db.collection("users").document(currentUser.getUid());
            tvPaymentUser.setText("Tài khoản: " + currentUser.getEmail());
            listenToCurrentUserBalance();
            loadTicketAndCalculateCost(currentUser.getUid());
        } else {
            showErrorAndFinish("Lỗi: Không xác định được người dùng.");
        }

        btnCancelPayment.setOnClickListener(v -> finish());
        btnConfirmPayment.setOnClickListener(v -> confirmAndProcessPayment());
    }

    private void mapViews() {
        // ID CỦA BẠN CÓ THỂ KHÁC, HÃY ĐỔI LẠI NẾU CẦN
        tvPaymentSlotName = findViewById(R.id.tvPaymentSlotId);
        tvPaymentLicensePlate = findViewById(R.id.tvPaymentLicensePlate);
        tvPaymentVehicleType = findViewById(R.id.tvPaymentVehicleType);
        tvPaymentStartTime = findViewById(R.id.tvPaymentStartTime);
        tvPaymentEndTime = findViewById(R.id.tvPaymentEndTime);
        tvPaymentDuration = findViewById(R.id.tvPaymentDuration);
        tvPaymentTotalFee = findViewById(R.id.tvPaymentTotalFee);
        tvPaymentUser = findViewById(R.id.tvPaymentUser);
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment); // THAY ID NẾU CẦN
        btnCancelPayment = findViewById(R.id.btnClosePayment);
        loadingOverlay = findViewById(R.id.loading_overlay); // THÊM VIEW NÀY VÀO LAYOUT
    }

    private void listenToCurrentUserBalance() {
        if (currentUserRef != null) {
            userBalanceListener = currentUserRef.addSnapshotListener(this, (snapshot, error) -> {
                if (snapshot != null && snapshot.exists()) {
                    Object balanceObj = snapshot.get("wallet_balance");
                    if (balanceObj instanceof Number) {
                        currentUserBalance = ((Number) balanceObj).longValue();
                        // Sau khi có số dư mới, kiểm tra lại nút thanh toán
                        checkBalanceAndEnableButton();
                    }
                }
            });
        }
    }

    private void loadTicketAndCalculateCost(String userId) {
        db.collection("phieu_gui_xe")
                .whereEqualTo("uid_user", userId)
                .whereEqualTo("id_slot", slotId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        showErrorAndFinish("Lỗi: Không tìm thấy vé xe đang hoạt động của bạn.");
                        return;
                    }
                    DocumentSnapshot ticket = queryDocumentSnapshots.getDocuments().get(0);

                    this.ticketId = ticket.getId();
                    this.historyId = ticket.getString("history_id");

                    Date startTime = ticket.getDate("startTime");
                    Date endTime = new Date(); // Thời gian hiện tại

                    if (startTime == null) {
                        showErrorAndFinish("Lỗi: Dữ liệu vé xe không hợp lệ (thiếu thời gian bắt đầu).");
                        return;
                    }

                    long durationMillis = endTime.getTime() - startTime.getTime();
                    long totalHours = TimeUnit.MILLISECONDS.toHours(durationMillis) + 1; // Luôn làm tròn lên
                    this.totalCost = totalHours * PRICE_PER_HOUR;

                    // Hiển thị thông tin lên UI
                    tvPaymentLicensePlate.setText("Biển số: " + ticket.getString("bien_so_xe"));
                    String vehicleType = ticket.getString("loai_xe");
                    if(vehicleType != null && !vehicleType.isEmpty()){
                        tvPaymentVehicleType.setText("Loại xe: " + vehicleType);
                        tvPaymentVehicleType.setVisibility(View.VISIBLE);
                    } else {
                        tvPaymentVehicleType.setVisibility(View.GONE);
                    }

                    tvPaymentStartTime.setText("Bắt đầu: " + dateFormat.format(startTime));
                    tvPaymentEndTime.setText("Kết thúc: " + dateFormat.format(endTime));
                    tvPaymentDuration.setText("Tổng giờ: ~" + totalHours + " giờ");
                    tvPaymentTotalFee.setText(formatCurrency(this.totalCost));

                    // Sau khi tính toán xong, kiểm tra số dư
                    checkBalanceAndEnableButton();
                })
                .addOnFailureListener(e -> showErrorAndFinish("Lỗi tải thông tin vé xe: " + e.getMessage()));
    }

    private void checkBalanceAndEnableButton() {
        if (totalCost > 0) { // Chỉ kiểm tra khi đã tính được giá
            if (currentUserBalance >= totalCost) {
                enablePaymentButton();
            } else {
                disablePaymentButton("Số dư không đủ. Vui lòng nạp thêm!");
            }
        }
    }

    private void confirmAndProcessPayment() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận thanh toán")
                .setMessage("Thực hiện thanh toán " + formatCurrency(totalCost) + " từ ví của bạn?")
                .setPositiveButton("Xác nhận", (dialog, which) -> processPayment())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void processPayment() {
        showLoading(true);
        disablePaymentButton("Đang xử lý...");

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot userSnapshot = transaction.get(currentUserRef);
            Long balanceInTransaction = userSnapshot.getLong("wallet_balance");

            if (balanceInTransaction == null || balanceInTransaction < totalCost) {
                throw new FirebaseFirestoreException("Số dư không đủ.", FirebaseFirestoreException.Code.ABORTED);
            }

            // Thực hiện các thao tác cập nhật
            transaction.update(currentUserRef, "wallet_balance", balanceInTransaction - totalCost);
            transaction.update(db.collection("parking_slots").document(slotId), "trang_thai", "trong", "uid_hien_tai", null);
            transaction.delete(db.collection("phieu_gui_xe").document(ticketId));
            if (historyId != null) {
                transaction.update(db.collection("parking_history").document(historyId), "status", "Đã thanh toán", "cost", totalCost, "endTime", new Date());
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            showLoading(false);
            Toast.makeText(this, "✅ Thanh toán thành công!", Toast.LENGTH_LONG).show();
            finish(); // Đóng màn hình thanh toán sau khi thành công
        }).addOnFailureListener(e -> {
            showLoading(false);
            Toast.makeText(this, "❌ Thanh toán thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
            checkBalanceAndEnableButton(); // Kích hoạt lại nút nếu có thể
        });
    }

    private void disablePaymentButton(String message) {
        btnConfirmPayment.setEnabled(false);
        btnConfirmPayment.setText(message);
    }

    private void enablePaymentButton() {
        btnConfirmPayment.setEnabled(true);
        btnConfirmPayment.setText("Thanh toán bằng ví");
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    private String formatCurrency(long amount) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(amount);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userBalanceListener != null) {
            userBalanceListener.remove();
        }
    }
}
