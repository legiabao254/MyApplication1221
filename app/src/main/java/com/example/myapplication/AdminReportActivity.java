package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.util.Locale;

public class AdminReportActivity extends AppCompatActivity {

    private TextView tvTotalRevenue, tvTotalTransactions, tvDateRange;
    private Button btnLoadReport;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_report);
        setTitle("Báo cáo Doanh thu");

        // Khởi tạo Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Ánh xạ View
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTotalTransactions = findViewById(R.id.tvTotalTransactions);
        tvDateRange = findViewById(R.id.tvDateRange);
        btnLoadReport = findViewById(R.id.btnLoadReport);

        // Đặt hành động cho nút
        btnLoadReport.setOnClickListener(v -> loadRevenueReport());

        // Tải báo cáo khi Activity được mở
        loadRevenueReport();
    }

    private void loadRevenueReport() {
        // Reset hiển thị và thông báo đang tải
        tvTotalRevenue.setText("Tổng Doanh thu: Đang tải...");
        tvTotalTransactions.setText("Tổng Giao dịch: Đang tải...");
        btnLoadReport.setEnabled(false); // Vô hiệu hóa nút trong khi tải

        // 1. Xây dựng truy vấn
        db.collection("parking_history")
                .whereEqualTo("status", "completed") // Chỉ lấy các giao dịch đã hoàn tất
                // Nếu bạn muốn lọc theo thời gian, thêm .whereGreaterThanOrEqualTo("startTime", dateStart)

                // 2. Thực thi truy vấn
                .get()
                .addOnCompleteListener(task -> {
                    btnLoadReport.setEnabled(true); // Kích hoạt lại nút
                    if (task.isSuccessful()) {
                        double totalRevenue = 0.0;
                        int totalTransactions = 0;

                        // 3. Lặp qua các tài liệu và tính tổng
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // total_fee được lưu là Double, nên ta truy xuất dưới dạng Double
                            Double fee = document.getDouble("total_fee");
                            if (fee != null) {
                                totalRevenue += fee;
                                totalTransactions++;
                            }
                        }

                        // 4. Định dạng và Hiển thị kết quả
                        DecimalFormat feeFormat = new DecimalFormat("###,### VNĐ");

                        tvTotalRevenue.setText("Tổng Doanh thu: " + feeFormat.format(totalRevenue));
                        tvTotalTransactions.setText("Tổng Giao dịch: " + totalTransactions);

                        Toast.makeText(this, "Tải báo cáo thành công!", Toast.LENGTH_SHORT).show();

                    } else {
                        // Xử lý lỗi
                        Toast.makeText(this, "Lỗi khi tải báo cáo: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        tvTotalRevenue.setText("Tổng Doanh thu: Lỗi!");
                        tvTotalTransactions.setText("Tổng Giao dịch: Lỗi!");
                    }
                });
    }
}