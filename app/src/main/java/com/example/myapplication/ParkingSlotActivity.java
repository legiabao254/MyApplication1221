package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myapplication.adapter.SlotAdapter;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.myapplication.ParkingSlotActivity.ParkingSlotData;

public class ParkingSlotActivity extends AppCompatActivity {
    public static class ParkingSlotData {
        public static final String STATUS_TRONG = "trong";
        public static final String STATUS_DANG_GUI = "dang_gui";
        public static final String STATUS_BAO_TRI = "bao_tri";

        public static final String FIELD_TRANG_THAI = "trang_thai";
        public static final String FIELD_UID_HIEN_TAI = "uid_hien_tai";
        public static final String FIELD_TEN_SLOT = "ten_slot"; // Thêm hằng số này

        private String id; // Document ID của Firestore
        private String ten_slot;
        private String trang_thai;
        private String uid_hien_tai;



        public ParkingSlotData() {}


        public String getId() { return id; }
        public String getTen_slot() { return ten_slot; }
        public String getTrang_thai() { return trang_thai; }
        public String getUid_hien_tai() { return uid_hien_tai; }

        // Setters
        public void setId(String id) { this.id = id; }
        public void setTen_slot(String ten_slot) { this.ten_slot = ten_slot; }
        public void setTrang_thai(String trang_thai) { this.trang_thai = trang_thai; }
        public void setUid_hien_tai(String uid_hien_tai) { this.uid_hien_tai = uid_hien_tai; }
    }
    // 🔔 END LỚP MODEL DATA

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private SlotAdapter slotAdapter;
    private List<ParkingSlotData> parkingSlotList;
    private ListenerRegistration registration;
    private boolean showAdminControls = false;
    private AlertDialog currentDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking_list); // Đảm bảo layout này tồn tại

        // 1. NHẬN VAI TRÒ NGƯỜI DÙNG TỪ INTENT
        Intent intent = getIntent();
        // Nhận role, ví dụ từ MainActivity: intent.putExtra("USER_ROLE", "admin");
        String userRole = intent.getStringExtra("USER_ROLE");

        if ("admin".equals(userRole) || "staff".equals(userRole)) {
            showAdminControls = true;
            setTitle("Quản lý Bãi đỗ xe (Admin/Staff)");
            // Kích hoạt nút Thêm slot (nếu bạn có FloatingActionButton trong layout)
            // findViewById(R.id.fabAddSlot).setVisibility(View.VISIBLE);
        } else {
            showAdminControls = false;
            setTitle("Danh sách Bãi đỗ xe");
        }

        db = FirebaseFirestore.getInstance();
        parkingSlotList = new ArrayList<>();
        recyclerView = findViewById(R.id.rvParkingSlots);

        if (recyclerView == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy RecyclerView (rvParkingSlots)!", Toast.LENGTH_LONG).show();
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 2. KHỞI TẠO ADAPTER VÀ LISTENER
        slotAdapter = new SlotAdapter(parkingSlotList, new SlotAdapter.SlotListener() {
            @Override
            public void onEdit(ParkingSlotData slot) {
                // XỬ LÝ ADMIN/STAFF
                if (showAdminControls) {
                    showEditDialog(slot);
                    return;
                }

                // XỬ LÝ USER (Gửi/Lấy xe)
                // ... (Logic cũ vẫn hoạt động)
                Intent actionIntent = new Intent(ParkingSlotActivity.this, ParkingActivity.class);
                actionIntent.putExtra("SLOT_ID", slot.getTen_slot());
                actionIntent.putExtra("SLOT_DOC_ID", slot.getId());
                actionIntent.putExtra("SLOT_STATUS", slot.getTrang_thai());
                actionIntent.putExtra("SLOT_UID", slot.getUid_hien_tai());

                String status = slot.getTrang_thai() != null ? slot.getTrang_thai().toLowerCase() : "";

                if (status.equals(ParkingSlotData.STATUS_TRONG) || status.equals(ParkingSlotData.STATUS_DANG_GUI)) {
                    startActivity(actionIntent);
                } else {
                    Toast.makeText(ParkingSlotActivity.this, "Slot đang Bảo trì.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDelete(ParkingSlotData slot) {
                if (showAdminControls) {
                    confirmDelete(slot);
                }
            }
        }, showAdminControls);

        recyclerView.setAdapter(slotAdapter);
        loadParkingSlotsRealtime();

        // Thêm Listener cho nút Thêm Slot (nếu FAB/Button tồn tại trong layout)
        // View fabAddSlot = findViewById(R.id.fabAddSlot);
        // if (fabAddSlot != null) {
        //     fabAddSlot.setVisibility(showAdminControls ? View.VISIBLE : View.GONE);
        //     fabAddSlot.setOnClickListener(v -> showEditDialog(null));
        // }
    }

    // ===============================================
    // HÀM QUẢN LÝ ADMIN
    // ===============================================

    private void showEditDialog(ParkingSlotData slot) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        // R.layout.dialog_slot phải chứa edtSlotName và edtSlotStatus
        View dialogView = inflater.inflate(R.layout.dialog_slot, null);
        builder.setView(dialogView);

        EditText edtSlotName = dialogView.findViewById(R.id.edtSlotName);
        EditText edtSlotStatus = dialogView.findViewById(R.id.edtSlotStatus);

        final boolean isEditing = (slot != null);

        if (isEditing) {
            builder.setTitle("Chỉnh sửa Slot: " + slot.getTen_slot());
            edtSlotName.setText(slot.getTen_slot());
            edtSlotStatus.setText(slot.getTrang_thai());
            // KHÔNG cho phép sửa tên slot khi đang chỉnh sửa (Tên slot là Document ID)
            edtSlotName.setEnabled(false);
        } else {
            builder.setTitle("Thêm Slot Mới");
            edtSlotName.setEnabled(true);
        }

        builder.setPositiveButton(isEditing ? "Lưu" : "Thêm", (dialog, id) -> {
            String name = edtSlotName.getText().toString().trim();
            // Trạng thái luôn được chuẩn hóa thành chữ thường
            String status = edtSlotStatus.getText().toString().trim().toLowerCase();

            if (!isEditing && TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Tên Slot không được để trống khi thêm mới.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(status)) {
                Toast.makeText(this, "Trạng thái không được để trống.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra trạng thái hợp lệ
            if (!status.equals(ParkingSlotData.STATUS_TRONG) &&
                    !status.equals(ParkingSlotData.STATUS_DANG_GUI) &&
                    !status.equals(ParkingSlotData.STATUS_BAO_TRI)) {
                Toast.makeText(this, "Trạng thái không hợp lệ. Chỉ chấp nhận: trong/dang_gui/bao_tri", Toast.LENGTH_LONG).show();
                return;
            }

            Map<String, Object> slotData = new HashMap<>();
            slotData.put(ParkingSlotData.FIELD_TRANG_THAI, status);

            if (isEditing) {
                // Cập nhật chỉ trạng thái
                editSlot(slot.getId(), slotData);
            } else {
                // Thêm mới: Tên slot là ID Document
                slotData.put(ParkingSlotData.FIELD_TEN_SLOT, name);

                // Mặc định UID_HIEN_TAI là null, chỉ được gán khi người dùng gửi xe
                slotData.put(ParkingSlotData.FIELD_UID_HIEN_TAI, null);

                addSlot(name, slotData);
            }
        });

        builder.setNegativeButton("Hủy", (dialog, id) -> dialog.dismiss());

        currentDialog = builder.create();
        currentDialog.show();
    }

    // Đã thay đổi: Dùng tên slot làm Document ID
    private void addSlot(String slotId, Map<String, Object> slotData) {
        db.collection("parking_slots").document(slotId).set(slotData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Thêm slot " + slotId + " thành công!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Lỗi thêm slot: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Đã thay đổi: Dùng Map thay vì gán lại trong object data
    private void editSlot(String docId, Map<String, Object> updates) {
        db.collection("parking_slots").document(docId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Cập nhật slot thành công!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Lỗi cập nhật slot: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void confirmDelete(ParkingSlotData slot) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận Xóa Slot")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn slot " + slot.getTen_slot() + " không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteSlot(slot))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteSlot(ParkingSlotData slot) {
        db.collection("parking_slots").document(slot.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Đã xóa slot " + slot.getTen_slot(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Lỗi xóa slot: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    // ===============================================
    // HÀM TẢI DỮ LIỆU VÀ QUẢN LÝ FIREBASE
    // ===============================================

    private void loadParkingSlotsRealtime() {
        // ... (Logic cũ vẫn hoạt động tốt)
        registration = db.collection("parking_slots")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải dữ liệu slot: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value == null) return;

                    parkingSlotList.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ParkingSlotData s = doc.toObject(ParkingSlotData.class);
                        if (s != null) {
                            s.setId(doc.getId()); // Gán Document ID
                            parkingSlotList.add(s);
                        }
                    }
                    slotAdapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ngừng lắng nghe để tránh rò rỉ bộ nhớ
        if (registration != null) registration.remove();
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
    }
}