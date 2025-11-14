package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.example.myapplication.ParkingSlotActivity.ParkingSlotData;

public class ParkingActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Hằng số tính phí
    private static final double HOURLY_RATE = 5000.0;
    private static final long MILLIS_PER_HOUR = 3600000;

    // Biến cho Layout Gửi/Lấy xe (activity_parking.xml)
    private TextView tvParkingTitle, tvCurrentStatus, tvSlotId, tvStartTime;
    private TextInputLayout layoutLicensePlate;
    private EditText etLicensePlate;
    private Button btnAction;

    // Data from Intent
    private String slotDocId;
    private String slotId;
    private String currentSlotStatus;
    private String slotOccupiedByUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. LẤY DỮ LIỆU TỪ INTENT VÀ USER
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        Intent intent = getIntent();
        slotId = intent.getStringExtra("SLOT_ID");
        slotDocId = intent.getStringExtra("SLOT_DOC_ID");
        currentSlotStatus = intent.getStringExtra("SLOT_STATUS");
        slotOccupiedByUid = intent.getStringExtra("SLOT_UID");

        if (currentUser == null || slotDocId == null || slotId == null) {
            Toast.makeText(this, "Lỗi dữ liệu hoặc chưa đăng nhập.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 2. THIẾT LẬP LAYOUT GỬI/LẤY XE BAN ĐẦU
        setContentView(R.layout.activity_parking);
        mapViewsForParking();
        setupUIForAction(currentUser.getUid());
    }

    private void mapViewsForParking() {
        tvParkingTitle = findViewById(R.id.tvParkingTitle);
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus);
        tvSlotId = findViewById(R.id.tvParkingSlotId);
        tvStartTime = findViewById(R.id.tvStartTime);
        layoutLicensePlate = findViewById(R.id.layoutLicensePlate);
        etLicensePlate = findViewById(R.id.etLicensePlate);
        btnAction = findViewById(R.id.btnAction);
    }

    // Hàm này không cần thiết nữa vì ta chuyển sang PaymentActivity
    // private void mapViewsForPayment() { ... }

    private void setupUIForAction(String currentUserId) {
        tvSlotId.setText("Slot đã chọn: " + slotId);

        if (ParkingSlotData.STATUS_TRONG.equals(currentSlotStatus)) {
            // GỬI XE
            setTitle("Phiếu Gửi Xe");
            tvParkingTitle.setText("PHIẾU GỬI XE");
            tvCurrentStatus.setText("Trạng thái: Slot trống.");
            layoutLicensePlate.setVisibility(View.VISIBLE);
            etLicensePlate.setText(""); // Xóa dữ liệu cũ
            btnAction.setText("BẮT ĐẦU GỬI XE");

            Timestamp startTime = Timestamp.now();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
            tvStartTime.setText("Giờ hiện tại: " + sdf.format(startTime.toDate()));

            btnAction.setOnClickListener(v -> startParkingProcess(currentUserId, startTime));

        } else if (ParkingSlotData.STATUS_DANG_GUI.equals(currentSlotStatus)
                && currentUserId.equals(slotOccupiedByUid)) {
            // LẤY XE (Xe của User hiện tại)
            setTitle("Phiếu Lấy Xe");
            tvParkingTitle.setText("PHIẾU LẤY XE");
            tvCurrentStatus.setText("Trạng thái: Xe của bạn đang gửi.");
            layoutLicensePlate.setVisibility(View.GONE);
            btnAction.setText("KẾT THÚC VÀ LẤY XE");
            tvStartTime.setText("Thông tin thời gian sẽ được tải."); // Sẽ được tải sau khi lấy dữ liệu

            btnAction.setOnClickListener(v -> endParkingProcess(currentUserId));

        } else {
            // KHÔNG THAO TÁC ĐƯỢC (Đang bị người khác chiếm hoặc Bảo trì)
            setTitle("Thông tin Slot");
            tvParkingTitle.setText("THÔNG TIN SLOT");
            tvCurrentStatus.setText("Slot không khả dụng để thao tác.");
            layoutLicensePlate.setVisibility(View.GONE);
            btnAction.setVisibility(View.GONE);
            tvStartTime.setVisibility(View.GONE);
            Toast.makeText(this, "Không thể thao tác. Slot đang bị chiếm hoặc Bảo trì.", Toast.LENGTH_LONG).show();
        }
    }

    // =========================================================================================
    // HÀM GỬI XE (START PARKING PROCESS)
    // =========================================================================================
    private void startParkingProcess(String userId, Timestamp startTime) {
        String licensePlate = etLicensePlate.getText().toString().trim();
        if (TextUtils.isEmpty(licensePlate)) {
            Toast.makeText(this, "Vui lòng nhập biển số xe.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(ParkingSlotData.FIELD_TRANG_THAI, ParkingSlotData.STATUS_DANG_GUI);
        updates.put(ParkingSlotData.FIELD_UID_HIEN_TAI, userId);
        updates.put("thoi_gian_bat_dau", startTime);
        updates.put("bien_so_xe", licensePlate.toUpperCase());

        // Cập nhật Firestore
        db.collection("parking_slots").document(slotDocId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Gửi xe thành công tại " + slotId, Toast.LENGTH_SHORT).show();
                    finish(); // Đóng phiếu và trở lại danh sách slot
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Lỗi khi gửi xe: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    // =========================================================================================
    // HÀM LẤY XE & CHUYỂN SANG ACTIVITY THANH TOÁN
    // =========================================================================================
    private void endParkingProcess(String userId) {

        db.collection("parking_slots").document(slotDocId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Timestamp recordStartTime = documentSnapshot.getTimestamp("thoi_gian_bat_dau");
                        String recordLicensePlate = documentSnapshot.getString("bien_so_xe");

                        if (recordStartTime == null) {
                            Toast.makeText(this, "Lỗi: Không tìm thấy thời gian bắt đầu.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // TÍNH TOÁN PHÍ GỬI XE
                        long endTimeMillis = System.currentTimeMillis();
                        long durationMillis = endTimeMillis - recordStartTime.toDate().getTime();
                        double durationHours = (double) durationMillis / MILLIS_PER_HOUR;
                        // Làm tròn lên giờ tiếp theo
                        long totalHours = (long) Math.ceil(durationHours);
                        double totalFee = totalHours * HOURLY_RATE;

                        // FORMAT
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
                        String startTimeStr = sdf.format(recordStartTime.toDate());
                        String endTimeStr = sdf.format(new Date(endTimeMillis));

                        // TẠO RECORD LỊCH SỬ
                        Map<String, Object> historyRecord = new HashMap<>();
                        historyRecord.put("user_uid", userId);
                        historyRecord.put("slot_name", slotId);
                        historyRecord.put("bien_so_xe", recordLicensePlate);
                        historyRecord.put("startTime", recordStartTime);
                        historyRecord.put("endTime", new Timestamp(new Date(endTimeMillis)));
                        historyRecord.put("duration_hours", totalHours);
                        historyRecord.put("total_fee", totalFee);
                        historyRecord.put("status", "completed");

                        // Lưu lịch sử và Reset Slot trong Transaction
                        db.collection("parking_history").add(historyRecord)
                                .addOnSuccessListener(historyDoc -> {
                                    // RESET TRẠNG THÁI SLOT
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put(ParkingSlotData.FIELD_TRANG_THAI, ParkingSlotData.STATUS_TRONG);

                                    // ĐÃ SỬA: Thay thế việc set null bằng lệnh xóa FieldValue.delete()
                                    updates.put(ParkingSlotData.FIELD_UID_HIEN_TAI, FieldValue.delete());

                                    updates.put("thoi_gian_bat_dau", FieldValue.delete());
                                    updates.put("bien_so_xe", FieldValue.delete());

                                    db.collection("parking_slots").document(slotDocId)
                                            .update(updates)
                                            .addOnSuccessListener(v -> {
                                                // ✅ CHUYỂN SANG PAYMENT ACTIVITY
                                                showPaymentReceipt(
                                                        slotId,
                                                        recordLicensePlate,
                                                        currentUser.getEmail(),
                                                        startTimeStr,
                                                        endTimeStr,
                                                        totalHours,
                                                        totalFee
                                                );
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(this, "❌ Lỗi reset Slot: " + e.getMessage(), Toast.LENGTH_LONG).show());
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "❌ Lỗi lưu lịch sử: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    } else {
                        Toast.makeText(this, "Lỗi: Document Slot không tồn tại.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi đọc dữ liệu Slot: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // =========================================================================================
    // KHỞI CHẠY PAYMENT ACTIVITY (Đã sửa để đồng bộ)
    // =========================================================================================
    private void showPaymentReceipt(
            String slotName,
            String licensePlate,
            String userEmail,
            String startTime,
            String endTime,
            long totalHours,
            double totalFee
    ) {
        // TẠO INTENT GỌI PAYMENT ACTIVITY
        Intent paymentIntent = new Intent(ParkingActivity.this, PaymentActivity.class);

        // TRUYỀN DỮ LIỆU THANH TOÁN QUA INTENT
        paymentIntent.putExtra("SLOT_NAME", slotName);
        paymentIntent.putExtra("LICENSE_PLATE", licensePlate);
        paymentIntent.putExtra("USER_EMAIL", userEmail);
        paymentIntent.putExtra("START_TIME_STR", startTime);
        paymentIntent.putExtra("END_TIME_STR", endTime);
        paymentIntent.putExtra("TOTAL_HOURS", totalHours);
        paymentIntent.putExtra("TOTAL_FEE", totalFee);

        // KHỞI CHẠY VÀ KẾT THÚC ParkingActivity
        startActivity(paymentIntent);

        // Đóng ParkingActivity sau khi chuyển màn hình
        finish();

        Toast.makeText(this, "Đã hoàn tất giao dịch. Đang chuyển đến màn hình thanh toán.", Toast.LENGTH_LONG).show();
    }
}