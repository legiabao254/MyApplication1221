package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.authapp.LoginActivity;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration dashboardListener;
    private ListenerRegistration userProfileListener;

    private String userRole;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnOpenDrawer;
    private TextView tvName, tvEmail, tvRole, tvWalletBalance;
    private CardView cardAdminReport;
    private Button btnStaffCheckIn;
    private TextView tvAvailableSlots, tvOccupiedSlots;

    // Thêm Button báo cáo
    private Button btnViewFullReportAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            goToLogin();
            return;
        }

        mapViews();
        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupDashboardListener();
        setupUserProfileListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (dashboardListener != null) dashboardListener.remove();
        if (userProfileListener != null) userProfileListener.remove();
    }

    private void mapViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        btnOpenDrawer = findViewById(R.id.btn_open_drawer);

        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            tvName = headerView.findViewById(R.id.tvName);
            tvEmail = headerView.findViewById(R.id.tvEmail);
            tvRole = headerView.findViewById(R.id.tvRole);
            tvWalletBalance = headerView.findViewById(R.id.tvWalletBalance);
        }

        cardAdminReport = findViewById(R.id.card_admin_revenue_report_main_activity);
        // Ánh xạ Button báo cáo
        btnViewFullReportAdmin = findViewById(R.id.btn_view_full_report_admin);

        btnStaffCheckIn = findViewById(R.id.btn_staff_check_in);
        tvAvailableSlots = findViewById(R.id.tv_available_slots);
        tvOccupiedSlots = findViewById(R.id.tv_occupied_slots);
    }

    private void setupClickListeners() {
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        if (btnOpenDrawer != null) {
            btnOpenDrawer.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        // Gán sự kiện cho cả CardView và Button bên trong
        if (cardAdminReport != null) {
            cardAdminReport.setOnClickListener(v -> goToAdminReport());
        }
        if (btnViewFullReportAdmin != null) {
            btnViewFullReportAdmin.setOnClickListener(v -> goToAdminReport());
        }
    }

    private void setupUserProfileListener() {
        if (userProfileListener != null) return;
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        final DocumentReference userRef = db.collection("users").document(currentUser.getUid());
        userProfileListener = userRef.addSnapshotListener(this, (snapshot, error) -> {
            if (error != null) {
                Log.w(TAG, "Lỗi lắng nghe thông tin người dùng.", error);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                userRole = snapshot.getString("role");
                long balance = 0L;
                Object balanceObject = snapshot.get("wallet_balance");
                if (balanceObject instanceof Number) {
                    balance = ((Number) balanceObject).longValue();
                }
                updateHeaderUI(snapshot.getString("name"), snapshot.getString("email"), userRole, balance);
                setupUIAccordingToRole();
            } else {
                logoutUser();
            }
        });
    }

    private void setupDashboardListener() {
        if (dashboardListener != null) return;
        dashboardListener = db.collection("parking_slots").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Lỗi lắng nghe dashboard.", e);
                return;
            }
            if (snapshots != null) {
                int availableCount = 0;
                int occupiedCount = 0;
                for (QueryDocumentSnapshot doc : snapshots) {
                    String status = doc.getString("trang_thai");
                    if ("trong".equalsIgnoreCase(status)) availableCount++;
                    else if ("dang_gui".equalsIgnoreCase(status)) occupiedCount++;
                }
                if (tvAvailableSlots != null) tvAvailableSlots.setText(String.valueOf(availableCount));
                if (tvOccupiedSlots != null) tvOccupiedSlots.setText(String.valueOf(occupiedCount));
            }
        });
    }

    private void updateHeaderUI(String name, String email, String role, long balance) {
        if (tvName != null) tvName.setText(name);
        if (tvEmail != null) tvEmail.setText(email);
        if (tvRole != null) tvRole.setText(role != null ? role.toUpperCase() : "USER");
        if (tvWalletBalance != null) {
            tvWalletBalance.setText(NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(balance));
        }
    }

    private void setupUIAccordingToRole() {
        if (userRole == null) return;
        boolean isAdmin = "admin".equalsIgnoreCase(userRole);
        boolean isUser = "user".equalsIgnoreCase(userRole);
        boolean isStaff = "staff".equalsIgnoreCase(userRole);

        // *** SỬA LẠI HOÀN TOÀN LOGIC NÀY ***
        if (navigationView != null) {
            Menu navMenu = navigationView.getMenu();

            // Tìm và điều khiển trực tiếp item "Xác nhận nạp tiền"
            MenuItem confirmDepositItem = navMenu.findItem(R.id.nav_admin_confirm_deposit);
            if (confirmDepositItem != null) {
                confirmDepositItem.setVisible(isAdmin);
            }

            // Giữ nguyên logic cho các item khác
            MenuItem historyItem = navMenu.findItem(R.id.nav_parking_history);
            if (historyItem != null) historyItem.setVisible(isAdmin || isUser);
        }

        if (cardAdminReport != null) cardAdminReport.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        if (btnStaffCheckIn != null) btnStaffCheckIn.setVisibility(isStaff ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        Intent intent = null;

        if (id == R.id.nav_home) {
            // Không làm gì
        } else if (id == R.id.nav_profile_detail) {
            intent = new Intent(this, ProfileDetailActivity.class);
        } else if (id == R.id.nav_parking_history) {
            intent = new Intent(this, ParkingHistoryActivity.class);
            intent.putExtra("USER_ROLE", userRole);
        } else if (id == R.id.nav_parking) {
            intent = new Intent(this, ParkingSlotActivity.class);
            intent.putExtra("USER_ROLE", userRole);
        } else if (id == R.id.nav_top_up) {
            intent = new Intent(this, TopUpActivity.class);
        } else if (id == R.id.nav_admin_confirm_deposit) {
            intent = new Intent(this, AdminConfirmDepositActivity.class);
        } else if (id == R.id.nav_logout) {
            logoutUser();
        }

        if (intent != null) {
            startActivity(intent);
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    // Hàm helper để tránh lặp code
    private void goToAdminReport() {
        startActivity(new Intent(MainActivity.this, AdminReportActivity.class));
    }

    private void logoutUser() {
        mAuth.signOut();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
