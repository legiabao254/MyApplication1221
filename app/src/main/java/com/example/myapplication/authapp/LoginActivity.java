package com.example.myapplication.authapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;
import android.content.SharedPreferences; // Import cần thiết
import android.content.Context; // Import cần thiết

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

public class LoginActivity extends AppCompatActivity {

    // ... (Các khai báo và onCreate giữ nguyên) ...

    EditText edtEmail, edtPassword;
    Button btnLogin, btnGoRegister;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoRegister = findViewById(R.id.btnGoRegister);

        btnLogin.setOnClickListener(v -> loginUser());
        btnGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void loginUser() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        FirebaseUser user = mAuth.getCurrentUser();
                        if(user != null){
                            String uid = user.getUid();

                            db.collection("users")
                                    .document(uid)
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        String role = "Không xác định";
                                        if(documentSnapshot.exists()){
                                            String tempRole = documentSnapshot.getString("role");
                                            if(tempRole != null) role = tempRole;
                                        }

                                        // 🌟 BƯỚC QUAN TRỌNG: LƯU ROLE VÀO SharedPreferences
                                        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                                        SharedPreferences.Editor editor = sharedPref.edit();
                                        editor.putString("USER_ROLE_KEY", role); // Key này phải khớp với key dùng để đọc trong MainActivity
                                        editor.apply();
                                        // --------------------------------------------------

                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        // ❌ BỎ: intent.putExtra("role", role); // KHÔNG CẦN TRUYỀN QUA INTENT NỮA
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(LoginActivity.this,
                                                "Lỗi khi lấy dữ liệu: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(this,
                                "Sai tài khoản hoặc mật khẩu",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}