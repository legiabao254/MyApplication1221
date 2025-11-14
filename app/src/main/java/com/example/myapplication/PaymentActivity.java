package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;
import java.text.DecimalFormat;
import java.util.Locale;

public class PaymentActivity extends AppCompatActivity {

    private TextView tvPaymentSlotId, tvPaymentLicensePlate, tvPaymentStartTime,
            tvPaymentEndTime, tvPaymentDuration, tvPaymentTotalFee, tvPaymentUser;
    private Button btnClosePayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        setTitle("Chi tiết Thanh toán");

        // Ánh xạ View (Đảm bảo ID khớp với activity_payment.xml)
        tvPaymentSlotId = findViewById(R.id.tvPaymentSlotId);
        tvPaymentLicensePlate = findViewById(R.id.tvPaymentLicensePlate);
        tvPaymentStartTime = findViewById(R.id.tvPaymentStartTime);
        tvPaymentEndTime = findViewById(R.id.tvPaymentEndTime);
        tvPaymentDuration = findViewById(R.id.tvPaymentDuration);
        tvPaymentTotalFee = findViewById(R.id.tvPaymentTotalFee);
        tvPaymentUser = findViewById(R.id.tvPaymentUser);
        btnClosePayment = findViewById(R.id.btnClosePayment);

        // LẤY DỮ LIỆU TỪ INTENT
        Intent intent = getIntent();
        String slotName = intent.getStringExtra("SLOT_NAME");
        String licensePlate = intent.getStringExtra("LICENSE_PLATE");
        String userEmail = intent.getStringExtra("USER_EMAIL");
        String startTimeStr = intent.getStringExtra("START_TIME_STR");
        String endTimeStr = intent.getStringExtra("END_TIME_STR");
        long totalHours = intent.getLongExtra("TOTAL_HOURS", 0);
        double totalFee = intent.getDoubleExtra("TOTAL_FEE", 0.0);

        // HIỂN THỊ DỮ LIỆU
        tvPaymentSlotId.setText("Slot: " + slotName);
        tvPaymentLicensePlate.setText("Biển số xe: " + licensePlate);
        tvPaymentUser.setText("Tài khoản: " + userEmail);
        tvPaymentStartTime.setText("Bắt đầu: " + startTimeStr);
        tvPaymentEndTime.setText("Kết thúc: " + endTimeStr);
        tvPaymentDuration.setText("Tổng thời gian: " + totalHours + " giờ");

        DecimalFormat feeFormat = new DecimalFormat("###,### VNĐ");
        tvPaymentTotalFee.setText(feeFormat.format(totalFee));

        // Nút hoàn tất sẽ đóng màn hình và quay lại màn hình trước (ParkingSlotActivity)
        btnClosePayment.setOnClickListener(v -> finish());
    }
}