package com.example.myapplication.authapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import android.text.TextUtils; // Import thêm TextUtils để kiểm tra chuỗi rỗng

import com.google.firebase.auth.FirebaseAuth;
import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin, btnGoRegister;
    private FirebaseAuth mAuth;
    // Không cần FirebaseFirestore ở đây nữa

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Ánh xạ Views
        mapViews();

        // 3. Thiết lập sự kiện click
        setupClickListeners();
    }

    private void mapViews() {
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoRegister = findViewById(R.id.btnGoRegister);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        btnGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void loginUser() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        // Sử dụng TextUtils để kiểm tra chuỗi rỗng, an toàn hơn
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Vô hiệu hóa nút bấm để tránh người dùng click nhiều lần
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Đăng nhập thành công, chuyển ngay sang MainActivity
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                        goToMainActivity();
                    } else {
                        // Đăng nhập thất bại, hiển thị thông báo và bật lại nút bấm
                        Toast.makeText(this, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                        btnLogin.setEnabled(true);
                    }
                });
    }

    /**
     * CẬP NHẬT: Chuyển sang MainActivity một cách an toàn và đúng chuẩn.
     * Hàm này sẽ xóa LoginActivity khỏi stack, người dùng không thể quay lại bằng nút back.
     */
    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        // Cờ này sẽ xóa tất cả các Activity trước đó và tạo một task mới cho MainActivity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        // Không cần gọi finish() nữa vì cờ CLEAR_TASK đã làm việc đó
    }
}
