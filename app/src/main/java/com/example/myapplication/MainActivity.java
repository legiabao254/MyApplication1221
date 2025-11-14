package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

// Imports không còn cần thiết cho việc xử lý Token/Claims
// import com.google.android.gms.tasks.Task;
// import com.google.android.gms.tasks.OnCompleteListener;
// import com.google.firebase.auth.GetTokenResult;

import com.example.myapplication.authapp.LoginActivity;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// Import lớp Activity đã được sửa lỗi gần nhất
import com.example.myapplication.ParkingSlotActivity;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private String userRole; // Lưu role để dùng chung

    // Khai báo biến thành viên cho các View trong Header
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvRole;
    private Button btnLogout;

    // Khai báo biến thành viên cho các View trong Layout chính
    private CardView cardAdmin;
    private Button btnStaffCheckIn;
    private Button btnViewFullReport; // Khai báo nút xem báo cáo chi tiết

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        drawerLayout = findViewById(R.id.drawer_layout);

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // 1. Ánh xạ và lưu trữ các View trong Header (Chỉ làm 1 lần)
        View headerView = navigationView.getHeaderView(0);
        tvName = headerView.findViewById(R.id.tvName);
        tvEmail = headerView.findViewById(R.id.tvEmail);
        tvRole = headerView.findViewById(R.id.tvRole);
        btnLogout = headerView.findViewById(R.id.btnLogout);

        // 2. Ánh xạ các View trong Layout chính
        cardAdmin = findViewById(R.id.card_admin_revenue_report_main_activity);
        btnStaffCheckIn = findViewById(R.id.btn_staff_check_in);
        btnViewFullReport = findViewById(R.id.btn_view_full_report_admin);


        // Tải thông tin người dùng và Role
        loadUserRole();
        updateUserInfoInHeader();

        // Cập nhật các thành phần UI theo Role
        setupUIAccordingToRole();

        btnLogout.setOnClickListener(v -> {
            logoutUser();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserRole();
        updateUserInfoInHeader();
        setupUIAccordingToRole();
    }

    private void updateUserInfoInHeader() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && tvName != null && tvEmail != null) {

            String name = user.getDisplayName();
            tvName.setText(name != null && !name.isEmpty() ? name :
                    (user.getEmail() != null ? user.getEmail().split("@")[0] : "Người dùng ẩn danh"));
            tvEmail.setText(user.getEmail() != null ? user.getEmail() : "Không có Email");
        }
    }

    private void loadUserRole() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userRole = sharedPref.getString("USER_ROLE_KEY", "user").toLowerCase();

        if (tvRole != null) {
            tvRole.setText("Vai trò: " + userRole.toUpperCase());
        }
    }

    private void setupUIAccordingToRole() {
        // 1. Ẩn/hiện Menu Drawer
        Menu navMenu = navigationView.getMenu();
        MenuItem parkingItem = navMenu.findItem(R.id.nav_parking);
        boolean shouldShowParking = userRole.equalsIgnoreCase("user") || userRole.equalsIgnoreCase("admin") || userRole.equalsIgnoreCase("staff");
        if (parkingItem != null) {
            parkingItem.setVisible(shouldShowParking);
        }

        // 2. Ẩn/hiện Card và Button trong Layout chính
        if (cardAdmin == null || btnStaffCheckIn == null || btnViewFullReport == null) return;

        if (userRole.equalsIgnoreCase("admin")) {
            cardAdmin.setVisibility(View.VISIBLE);
            btnStaffCheckIn.setVisibility(View.GONE);

            // ĐÃ SỬA: Chỉ cần kiểm tra role cục bộ và mở Activity.
            // Firebase Rules (backend) sẽ chịu trách nhiệm cấp quyền truy vấn data.
            btnViewFullReport.setOnClickListener(v -> {
                // Bỏ qua hàm checkAndOpenAdminReport() cũ (kiểm tra Custom Claim)
                Intent intent = new Intent(MainActivity.this, AdminReportActivity.class);
                startActivity(intent);
            });

        } else if (userRole.equalsIgnoreCase("staff")) {
            cardAdmin.setVisibility(View.GONE);
            btnStaffCheckIn.setVisibility(View.VISIBLE);
        } else { // Role user hoặc undefined
            cardAdmin.setVisibility(View.GONE);
            btnStaffCheckIn.setVisibility(View.GONE);
        }
    }

    private void clearUserRole() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove("USER_ROLE_KEY");
        editor.apply();
    }

    private void logoutUser() {
        mAuth.signOut();
        clearUserRole();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
    }

    // -------------------------------------------------------------
    // HÀM checkAndOpenAdminReport() CŨ ĐÃ BỊ LOẠI BỎ KHỎI onClickListener CỦA NÚT.
    // -------------------------------------------------------------


    // Xử lý sự kiện Navigation Drawer
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;

        if (id == R.id.nav_home) {
            // Đã ở trang chủ, không cần làm gì
        } else if (id == R.id.nav_profile_detail) {
            intent = new Intent(MainActivity.this, ProfileDetailActivity.class);
        } else if (id == R.id.nav_parking) {
            intent = new Intent(MainActivity.this, ParkingSlotActivity.class);
            intent.putExtra("USER_ROLE", userRole); // Truyền Role
        } else if (id == R.id.nav_logout) {
            logoutUser();
        } else {
            Toast.makeText(this, "Chức năng đang phát triển", Toast.LENGTH_SHORT).show();
        }

        if (intent != null) {
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}