package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.ParkingHistoryAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ParkingHistoryActivity extends AppCompatActivity {

    private static final String TAG = "ParkingHistoryActivity";
    private RecyclerView recyclerView;
    private ParkingHistoryAdapter adapter;
    private List<ParkingHistory> historyList;
    private SearchView searchView;
    private ProgressBar progressBar;
    private TextView tvNoHistory;
    private FirebaseFirestore db;
    private String userRole = "user"; // Mặc định

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking_history);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recycler_view_history);
        searchView = findViewById(R.id.search_view_history);
        progressBar = findViewById(R.id.progress_bar_history);
        tvNoHistory = findViewById(R.id.tv_no_history);

        historyList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Bắt đầu luồng logic mới, đồng bộ
        loadRoleFromIntentAndFetchData();
        setupSearchView();
    }

    private void loadRoleFromIntentAndFetchData() {
        // Lấy 'role' từ Intent mà MainActivity đã gửi qua
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("USER_ROLE")) {
            userRole = intent.getStringExtra("USER_ROLE");
        }

        Log.d(TAG, "Role received from Intent: " + userRole);

        adapter = new ParkingHistoryAdapter(historyList, "admin".equalsIgnoreCase(userRole));
        recyclerView.setAdapter(adapter);

        // Bắt đầu tải dữ liệu lần đầu
        fetchHistoryData("");
    }

    private void fetchHistoryData(final String searchText) {
        progressBar.setVisibility(View.VISIBLE);
        tvNoHistory.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        historyList.clear();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        Query query;
        // ===================================================================
        // *** SỬA LẠI LOGIC QUERY ĐỂ KHÔNG CẦN INDEX ***
        // ===================================================================
        if ("admin".equalsIgnoreCase(userRole)) {
            Log.d(TAG, "Role is ADMIN. Querying all from 'parking_history'.");
            // Admin chỉ query, không sắp xếp ở đây để tránh lỗi index
            query = db.collection("parking_history");
        } else {
            Log.d(TAG, "Role is USER. Querying 'parking_history' for UID: " + currentUser.getUid());
            // User chỉ query theo user_uid, không sắp xếp ở đây
            query = db.collection("parking_history").whereEqualTo("user_uid", currentUser.getUid());
        }

        // Bỏ phần orderBy phức tạp, sẽ sắp xếp sau khi lấy dữ liệu về
        query.limit(100).get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                historyList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    ParkingHistory history = document.toObject(ParkingHistory.class);
                    history.setDocumentId(document.getId());
                    historyList.add(history);
                }

                // SẮP XẾP DỮ LIỆU SAU KHI ĐÃ LẤY VỀ (CLIENT-SIDE SORTING)
                historyList.sort((o1, o2) -> {
                    if (o1.getStartTime() == null || o2.getStartTime() == null) return 0;
                    return o2.getStartTime().compareTo(o1.getStartTime());
                });

                adapter.notifyDataSetChanged();
                Log.d(TAG, "Query successful. Found " + historyList.size() + " records for role '" + userRole + "'.");

                if (historyList.isEmpty()) {
                    tvNoHistory.setText("Không có lịch sử nào.");
                    tvNoHistory.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                }
            } else {
                Log.e(TAG, "Error getting documents: ", task.getException());
                tvNoHistory.setText("Lỗi: " + task.getException().getMessage());
                tvNoHistory.setVisibility(View.VISIBLE);
            }
        });
    }


    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                fetchHistoryData(query.trim());
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().isEmpty()) {
                    searchView.post(() -> fetchHistoryData(""));
                }
                return true;
            }
        });
    }
}
