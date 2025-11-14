package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
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

    // Firebase & User
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Constants
    private static final double HOURLY_RATE = 5000.0;
    private static final long MILLIS_PER_HOUR = 3600000;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_CODE_SCANNER = 200;

    // Views
    private TextView tvParkingTitle, tvCurrentStatus, tvSlotId, tvStartTime;
    private TextInputLayout layoutLicensePlate;
    private TextInputEditText etLicensePlate;
    private Button btnAction;
    private RadioGroup rgVehicleType;

    // Data from Intent
    private String slotDocId, slotId, currentSlotStatus, slotOccupiedByUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking);

        if (!initializeData()) {
            finish();
            return;
        }

        mapViews();
        setupUIForAction();
        setupListeners();
    }

    private boolean initializeData() {
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
            return false;
        }
        return true;
    }

    private void mapViews() {
        tvParkingTitle = findViewById(R.id.tvParkingTitle);
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus);
        tvSlotId = findViewById(R.id.tvParkingSlotId);
        tvStartTime = findViewById(R.id.tvStartTime);
        layoutLicensePlate = findViewById(R.id.layoutLicensePlate);
        etLicensePlate = findViewById(R.id.etLicensePlate);
        btnAction = findViewById(R.id.btnAction);
        rgVehicleType = findViewById(R.id.rg_vehicle_type);
    }

    private void setupListeners() {
        btnAction.setOnClickListener(v -> handleActionClick());
        layoutLicensePlate.setEndIconOnClickListener(v -> handleCameraIconClick());
        rgVehicleType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_motorbike) {
                etLicensePlate.setHint("Ví dụ: 29H1-12345");
            } else {
                etLicensePlate.setHint("Ví dụ: 30A-123.45");
            }
        });
    }

    private void setupUIForAction() {
        tvSlotId.setText("Slot đã chọn: " + slotId);
        boolean isMyVehicle = currentUser.getUid().equals(slotOccupiedByUid);

        if (ParkingSlotData.STATUS_TRONG.equals(currentSlotStatus)) {
            setTitle("Phiếu Gửi Xe");
            tvParkingTitle.setText("PHIẾU GỬI XE");
            tvCurrentStatus.setText("Trạng thái: Slot trống.");
            btnAction.setText("BẮT ĐẦU GỬI XE");
            layoutLicensePlate.setVisibility(View.VISIBLE);
            rgVehicleType.setVisibility(View.VISIBLE);
            tvStartTime.setVisibility(View.VISIBLE);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
            tvStartTime.setText("Giờ hiện tại: " + sdf.format(new Date()));
        } else if (ParkingSlotData.STATUS_DANG_GUI.equals(currentSlotStatus) && isMyVehicle) {
            setTitle("Phiếu Lấy Xe");
            tvParkingTitle.setText("PHIẾU LẤY XE");
            tvCurrentStatus.setText("Trạng thái: Xe của bạn đang gửi.");
            btnAction.setText("KẾT THÚC VÀ LẤY XE");
            layoutLicensePlate.setVisibility(View.GONE);
            rgVehicleType.setVisibility(View.GONE);
            tvStartTime.setVisibility(View.GONE);
        } else {
            setTitle("Thông tin Slot");
            tvParkingTitle.setText("THÔNG TIN SLOT");
            tvCurrentStatus.setText("Slot không khả dụng để thao tác.");
            layoutLicensePlate.setVisibility(View.GONE);
            rgVehicleType.setVisibility(View.GONE);
            btnAction.setVisibility(View.GONE);
            tvStartTime.setVisibility(View.GONE);
        }
    }

    private void handleActionClick() {
        if (ParkingSlotData.STATUS_TRONG.equals(currentSlotStatus)) {
            startParkingProcess();
        } else {
            endParkingProcess();
        }
    }

    private void startParkingProcess() {
        String licensePlate = etLicensePlate.getText().toString().trim().toUpperCase();
        boolean isMotorbike = rgVehicleType.getCheckedRadioButtonId() == R.id.rb_motorbike;
        String vehicleType = isMotorbike ? "Xe máy" : "Ô tô";

        if (!validateInput(licensePlate)) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(ParkingSlotData.FIELD_TRANG_THAI, ParkingSlotData.STATUS_DANG_GUI);
        updates.put(ParkingSlotData.FIELD_UID_HIEN_TAI, currentUser.getUid());
        updates.put("thoi_gian_bat_dau", Timestamp.now());
        updates.put("bien_so_xe", licensePlate);
        updates.put("loai_xe", vehicleType);

        db.collection("parking_slots").document(slotDocId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Gửi xe thành công tại " + slotId, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "❌ Lỗi khi gửi xe: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private boolean validateInput(String licensePlate) {
        layoutLicensePlate.setError(null);
        if (licensePlate.isEmpty()) {
            layoutLicensePlate.setError("Biển số xe không được để trống");
            return false;
        }

        // Giả sử đã có lớp LicensePlateValidator
        // if (!LicensePlateValidator.isValid(licensePlate)) {
        //     layoutLicensePlate.setError("Định dạng biển số không hợp lệ");
        //     return false;
        // }

        return true;
    }

    private void handleCameraIconClick() {
        if (ParkingSlotData.STATUS_TRONG.equals(currentSlotStatus)) {
            checkAndRequestCameraPermission();
        } else {
            Toast.makeText(this, "Không thể quét khi xe đã gửi.", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            openLicensePlateScanner();
        }
    }

    private void openLicensePlateScanner() {
        Intent intent = new Intent(this, LPRScannerActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SCANNER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openLicensePlateScanner();
        } else {
            Toast.makeText(this, "Cần quyền camera để quét biển số.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCANNER && resultCode == RESULT_OK && data != null) {
            String scannedPlate = data.getStringExtra("LICENSE_PLATE_RESULT");
            if (etLicensePlate != null && scannedPlate != null && !scannedPlate.isEmpty()) {
                etLicensePlate.setText(scannedPlate.toUpperCase());
                Toast.makeText(this, "Đã quét biển số: " + scannedPlate, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void endParkingProcess() {
        db.collection("parking_slots").document(slotDocId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Lỗi: Document Slot không tồn tại.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Timestamp recordStartTime = documentSnapshot.getTimestamp("thoi_gian_bat_dau");
                    String recordLicensePlate = documentSnapshot.getString("bien_so_xe");
                    String vehicleType = documentSnapshot.getString("loai_xe");

                    if (recordStartTime == null) {
                        Toast.makeText(this, "Lỗi: Không tìm thấy thời gian bắt đầu.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long endTimeMillis = System.currentTimeMillis();
                    long durationMillis = endTimeMillis - recordStartTime.toDate().getTime();
                    double durationHours = (double) durationMillis / MILLIS_PER_HOUR;
                    long totalHours = (long) Math.ceil(durationHours > 0 ? durationHours : 1);
                    double totalFee = totalHours * HOURLY_RATE;

                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
                    String startTimeStr = sdf.format(recordStartTime.toDate());
                    String endTimeStr = sdf.format(new Date(endTimeMillis));

                    Map<String, Object> historyRecord = new HashMap<>();
                    historyRecord.put("user_uid", currentUser.getUid());
                    historyRecord.put("slot_name", slotId);
                    historyRecord.put("bien_so_xe", recordLicensePlate);
                    historyRecord.put("loai_xe", vehicleType);
                    historyRecord.put("startTime", recordStartTime);
                    historyRecord.put("endTime", new Timestamp(new Date(endTimeMillis)));
                    historyRecord.put("duration_hours", totalHours);
                    historyRecord.put("total_fee", totalFee);
                    historyRecord.put("status", "completed");

                    db.collection("parking_history").add(historyRecord)
                            .addOnSuccessListener(historyDoc -> {
                                Map<String, Object> slotUpdates = new HashMap<>();
                                slotUpdates.put(ParkingSlotData.FIELD_TRANG_THAI, ParkingSlotData.STATUS_TRONG);
                                slotUpdates.put(ParkingSlotData.FIELD_UID_HIEN_TAI, FieldValue.delete());
                                slotUpdates.put("thoi_gian_bat_dau", FieldValue.delete());
                                slotUpdates.put("bien_so_xe", FieldValue.delete());
                                slotUpdates.put("loai_xe", FieldValue.delete());

                                db.collection("parking_slots").document(slotDocId).update(slotUpdates)
                                        .addOnSuccessListener(v -> showPaymentReceipt(slotId, recordLicensePlate, vehicleType, currentUser.getEmail(), startTimeStr, endTimeStr, totalHours, totalFee));
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "❌ Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
                });
    }

    private void showPaymentReceipt(String slotName, String licensePlate, String vehicleType, String userEmail, String startTime, String endTime, long totalHours, double totalFee) {
        Intent paymentIntent = new Intent(this, PaymentActivity.class);
        paymentIntent.putExtra("SLOT_NAME", slotName);
        paymentIntent.putExtra("LICENSE_PLATE", licensePlate);
        paymentIntent.putExtra("VEHICLE_TYPE", vehicleType); // <-- Đã thêm
        paymentIntent.putExtra("USER_EMAIL", userEmail);
        paymentIntent.putExtra("START_TIME_STR", startTime);
        paymentIntent.putExtra("END_TIME_STR", endTime);
        paymentIntent.putExtra("TOTAL_HOURS", totalHours);
        paymentIntent.putExtra("TOTAL_FEE", totalFee);
        startActivity(paymentIntent);
        finish();
    }
}
