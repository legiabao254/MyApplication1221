package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

public class ProfileDetailActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Khai báo Views Cũ
    private TextView uidTextView;
    private TextInputEditText fullNameEditText;
    private TextInputEditText dobEditText;
    private Button saveButton;

    // Khai báo Views MỚI
    private TextInputEditText phoneNumberEditText;
    private TextInputEditText addressEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_detail);

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ánh xạ Views Cũ
        uidTextView = findViewById(R.id.text_user_uid);
        fullNameEditText = findViewById(R.id.edit_full_name);
        dobEditText = findViewById(R.id.edit_dob);
        saveButton = findViewById(R.id.btn_save_profile);

        // Ánh xạ Views MỚI
        phoneNumberEditText = findViewById(R.id.edit_phone_number);
        addressEditText = findViewById(R.id.edit_address);

        loadUserProfile();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserProfile();
            }
        });
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Lỗi: Người dùng chưa đăng nhập.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        final String currentUserId = currentUser.getUid();
        uidTextView.setText("UID: " + currentUserId);

        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot document) {
                        if (document.exists()) {
                            // Tải các trường cũ
                            fullNameEditText.setText(document.getString("fullName"));
                            dobEditText.setText(document.getString("dateOfBirth"));

                            // Tải các trường MỚI
                            phoneNumberEditText.setText(document.getString("phoneNumber"));
                            addressEditText.setText(document.getString("address"));

                            Toast.makeText(ProfileDetailActivity.this, "Đã tải thông tin cũ thành công.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProfileDetailActivity.this, "Tài khoản mới. Vui lòng bổ sung thông tin.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ProfileDetailActivity.this, "Lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String currentUserId = currentUser.getUid();

        String newFullName = fullNameEditText.getText().toString().trim();
        String newDob = dobEditText.getText().toString().trim();
        String newPhoneNumber = phoneNumberEditText.getText().toString().trim(); // Lấy dữ liệu mới
        String newAddress = addressEditText.getText().toString().trim(); // Lấy dữ liệu mới

        if (newFullName.isEmpty() || newDob.isEmpty() || newPhoneNumber.isEmpty() || newAddress.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ các thông tin bắt buộc.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo Map chứa dữ liệu cần cập nhật (bao gồm các trường mới)
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("fullName", newFullName);
        profileData.put("dateOfBirth", newDob);
        profileData.put("phoneNumber", newPhoneNumber); // Thêm số điện thoại
        profileData.put("address", newAddress); // Thêm địa chỉ
        profileData.put("lastUpdated", Timestamp.now());

        // Cập nhật lên Firestore
        db.collection("users").document(currentUserId)
                .set(profileData, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(ProfileDetailActivity.this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ProfileDetailActivity.this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}