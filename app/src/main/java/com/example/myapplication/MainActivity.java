package com.example.myapplication;



import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.view.GravityCompat;

import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;

import android.content.SharedPreferences; // Import cần thiết

import android.content.Context; // Import cần thiết

import android.os.Bundle;

import android.view.MenuItem;

import android.view.View;

import android.widget.Button;

import android.widget.TextView;

import android.widget.Toast;



import com.example.myapplication.authapp.LoginActivity;

import com.google.android.material.navigation.NavigationView;

import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.auth.FirebaseUser;

import com.example.myapplication.R;



public class MainActivity extends AppCompatActivity

        implements NavigationView.OnNavigationItemSelectedListener {



    FirebaseAuth mAuth;

    DrawerLayout drawerLayout;



// Khai báo tvRole ở cấp độ Class để có thể truy cập trong loadUserRole()

    private TextView tvRole;



    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);



        mAuth = FirebaseAuth.getInstance();

        drawerLayout = findViewById(R.id.drawer_layout);



        NavigationView navigationView = findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(this);



// --- CÀI ĐẶT HEADER VIEW ---

        View headerView = navigationView.getHeaderView(0);

        TextView tvName = headerView.findViewById(R.id.tvName);

        TextView tvEmail = headerView.findViewById(R.id.tvEmail);

        tvRole = headerView.findViewById(R.id.tvRole); // Ánh xạ tvRole

        Button btnLogout = headerView.findViewById(R.id.btnLogout);



        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {

            tvName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Tên người dùng");

            tvEmail.setText(user.getEmail() != null ? user.getEmail() : "email@example.com");

        }



// TẢI ROLE NGAY TRONG onCreate

        loadUserRole();



// Logout (trong Header)

        btnLogout.setOnClickListener(v -> {

            mAuth.signOut();

// Xóa Role khỏi SharedPreferences khi Logout

            clearUserRole();

            startActivity(new Intent(MainActivity.this, LoginActivity.class));

            finish();

        });

// ----------------------------------------

    }



    @Override

    protected void onResume() {

        super.onResume();

// **BƯỚC QUAN TRỌNG:** TẢI LẠI ROLE KHI QUAY LẠI MÀN HÌNH CHÍNH

// Điều này đảm bảo Role luôn được hiển thị đúng sau khi quay lại từ ProfileDetailActivity

        loadUserRole();

    }



    private void loadUserRole() {

// Lấy dữ liệu Role từ SharedPreferences

        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);

// Đọc Role đã được lưu bằng Key "USER_ROLE_KEY" (Role mặc định là "Không xác định")

        String role = sharedPref.getString("USER_ROLE_KEY", "Không xác định");

        tvRole.setText("Role: " + role);

    }



    private void clearUserRole() {

        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();

        editor.remove("USER_ROLE_KEY");

        editor.apply();

    }



// XỬ LÝ SỰ KIỆN CLICK CHO CÁC MỤC MENU

    @Override

    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();



        if (id == R.id.nav_profile_detail) {

// Xử lý khi click vào "Cập nhật Hồ sơ"

            Intent intent = new Intent(MainActivity.this, ProfileDetailActivity.class);

            startActivity(intent);

        } else if (id == R.id.nav_home) {

// Xử lý Trang chủ

            Toast.makeText(this, "Bạn đã chọn Trang chủ", Toast.LENGTH_SHORT).show();

        }

        else if (id == R.id.btnLogout) {

            mAuth.signOut();

            clearUserRole(); // Xóa Role khi Logout qua menu

            startActivity(new Intent(MainActivity.this, LoginActivity.class));

            finish();

        }

// Thêm các xử lý khác cho các mục menu còn lại



// Đóng Navigation Drawer sau khi chọn

        drawerLayout.closeDrawer(GravityCompat.START);

        return true;

    }

}