package com.example.myapplication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class TopUpActivity extends AppCompatActivity {

    // Thông tin tài khoản nhận tiền của bạn
    private static final String YOUR_BANK_ACCOUNT = "0123456789"; // <<-- SỬA THÀNH SỐ TÀI KHOẢN CỦA BẠN
    private static final String YOUR_BANK_NAME = "Vietcombank";   // <<-- SỬA THÀNH TÊN NGÂN HÀNG CỦA BẠN

    private TextInputEditText etAmount;
    private Button btnCreateRequest, btnCopyCode;
    private ProgressBar progressBar;
    private CardView cardPaymentInfo;
    private TextView tvBankAccount, tvBankName, tvPaymentCode;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_up);
        setTitle("Nạp tiền");

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thực hiện chức năng này", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mapViews();
        setupListeners();
    }

    private void mapViews() {
        etAmount = findViewById(R.id.et_amount);
        btnCreateRequest = findViewById(R.id.btn_create_top_up_request);
        btnCopyCode = findViewById(R.id.btn_copy_code);
        progressBar = findViewById(R.id.progress_bar_top_up);
        cardPaymentInfo = findViewById(R.id.card_payment_info);
        tvBankAccount = findViewById(R.id.tv_bank_account);
        tvBankName = findViewById(R.id.tv_bank_name);
        tvPaymentCode = findViewById(R.id.tv_payment_code);
    }

    private void setupListeners() {
        btnCreateRequest.setOnClickListener(v -> createTopUpRequest());
        btnCopyCode.setOnClickListener(v -> copyToClipboard(tvPaymentCode.getText().toString()));
    }

    private void createTopUpRequest() {
        String amountStr = etAmount.getText().toString();
        if (amountStr.isEmpty()) {
            etAmount.setError("Số tiền không được để trống");
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            etAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount < 10000) {
            etAmount.setError("Số tiền phải lớn hơn hoặc bằng 10,000 VNĐ");
            return;
        }

        // Tạo mã giao dịch duy nhất
        String paymentCode = "NAPTIEN" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(10, 99);

        setLoading(true);

        // Tạo document mới trong collection 'transactions'
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("user_uid", currentUser.getUid());
        transaction.put("amount", amount);
        transaction.put("type", "DEPOSIT"); // Loại giao dịch là NẠP TIỀN
        transaction.put("status", "PENDING"); // Trạng thái ban đầu là CHỜ XÁC NHẬN
        transaction.put("payment_code", paymentCode);
        transaction.put("timestamp", Timestamp.now());

        db.collection("transactions")
                .add(transaction)
                .addOnSuccessListener(documentReference -> {
                    // Tạo yêu cầu thành công, hiển thị thông tin cho người dùng
                    setLoading(false);
                    showPaymentInfo(paymentCode);
                    Toast.makeText(TopUpActivity.this, "Tạo yêu cầu thành công!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Có lỗi xảy ra
                    setLoading(false);
                    Toast.makeText(TopUpActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showPaymentInfo(String paymentCode) {
        // Ẩn form nhập liệu và hiển thị card thông tin
        etAmount.setEnabled(false);
        btnCreateRequest.setVisibility(View.GONE);

        tvBankAccount.setText(YOUR_BANK_ACCOUNT);
        tvBankName.setText(YOUR_BANK_NAME);
        tvPaymentCode.setText(paymentCode);

        cardPaymentInfo.setVisibility(View.VISIBLE);
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("PaymentCode", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Đã sao chép nội dung", Toast.LENGTH_SHORT).show();
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnCreateRequest.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnCreateRequest.setEnabled(true);
        }
    }
}
