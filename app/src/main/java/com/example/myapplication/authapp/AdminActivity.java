package com.example.myapplication.authapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.adapter.SlotAdapter;
import com.example.myapplication.R;
import com.example.myapplication.ParkingSlotActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.myapplication.ParkingSlotActivity.ParkingSlotData;

public class AdminActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private RecyclerView rv;
    private SlotAdapter adapter;

    private List<ParkingSlotData> slots = new ArrayList<>();
    private ListenerRegistration registration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lưu ý: Nếu AdminActivity dùng chung layout với MainActivity, hãy đổi tên layout
        setContentView(R.layout.activity_admin);
        setTitle("Quản lý Slot (Admin)");
        db = FirebaseFirestore.getInstance();

        rv = findViewById(R.id.rvSlots); // Đảm bảo ID này tồn tại
        rv.setLayoutManager(new LinearLayoutManager(this));

        // KHỞI TẠO ADAPTER: TRUYỀN 'TRUE' (HIỂN THỊ ADMIN CONTROLS)
        adapter = new SlotAdapter(slots, new SlotAdapter.SlotListener() {
            @Override
            public void onEdit(ParkingSlotData slot) {
                showEditDialog(slot);
            }
            @Override
            public void onDelete(ParkingSlotData slot) {
                confirmDelete(slot);
            }
        }, true);

        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddSlot); // Đảm bảo ID này tồn tại
        fab.setOnClickListener(v -> showEditDialog(null));

        loadSlotsRealtime();
    }

    private void loadSlotsRealtime() {
        // Realtime listener
        registration = db.collection("parking_slots")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải Slot: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value == null) return;

                    slots.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ParkingSlotData s = doc.toObject(ParkingSlotData.class);
                        if (s != null) {
                            s.setId(doc.getId());
                            slots.add(s);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showEditDialog(@Nullable ParkingSlotData slot) {
        // [CẢI TIẾN]: Dùng Layout Dialog phù hợp (dialog_slot.xml)
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_slot, null);
        EditText edtTenSlot = view.findViewById(R.id.edtSlotName);
        EditText edtTrangThai = view.findViewById(R.id.edtSlotStatus);

        if (slot != null) {
            edtTenSlot.setText(slot.getTen_slot());
            edtTrangThai.setText(slot.getTrang_thai());
            // Không cho phép sửa tên slot khi sửa (tùy chọn)
            edtTenSlot.setEnabled(false);
        } else {
            edtTrangThai.setText(ParkingSlotData.STATUS_TRONG); // Mặc định là 'trong' khi thêm mới
        }

        new AlertDialog.Builder(this)
                .setTitle(slot == null ? "Thêm slot mới" : "Sửa slot")
                .setView(view)
                .setPositiveButton("Lưu", (d, which) -> {
                    String tenSlot = edtTenSlot.getText().toString().trim();
                    String trangThai = edtTrangThai.getText().toString().trim();

                    if (tenSlot.isEmpty() || trangThai.isEmpty()) {
                        Toast.makeText(this, "Nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (slot == null) {
                        // 🔥 SỬA CHỮA ĐỒNG BỘ: Sử dụng Map hoặc Class Object với các trường cần thiết
                        // Đảm bảo tên trường khớp với ParkingSlotData và Firestore
                        Map<String, Object> newSlotData = new HashMap<>();
                        newSlotData.put("ten_slot", tenSlot);
                        newSlotData.put(ParkingSlotData.FIELD_TRANG_THAI, trangThai);
                        newSlotData.put(ParkingSlotData.FIELD_UID_HIEN_TAI, null); // Mặc định UID là null
                        addSlot(newSlotData);
                    } else {
                        // Cập nhật các trường cần thiết khi sửa
                        slot.setTrang_thai(trangThai);
                        editSlot(slot);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // SỬA CHỮA: Hàm addSlot nhận Map<String, Object> để đảm bảo tất cả fields được khởi tạo
    private void addSlot(Map<String, Object> slotData) {
        db.collection("parking_slots").add(slotData)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(this, "Đã thêm slot!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi thêm: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Giữ nguyên hàm editSlot (sử dụng set(slot) của Firestore)
    private void editSlot(ParkingSlotData slot) {
        // Lệnh set(slot) này sẽ hoạt động vì ParkingSlotData có getters/setters chuẩn
        db.collection("parking_slots").document(slot.getId()).set(slot)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Đã sửa slot!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi sửa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void confirmDelete(ParkingSlotData slot) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa slot?")
                .setMessage("Bạn chắc chắn muốn xóa slot " + slot.getTen_slot() + " này?")
                .setPositiveButton("Xóa", (d, which) -> deleteSlot(slot))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteSlot(ParkingSlotData slot) {
        db.collection("parking_slots").document(slot.getId()).delete()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Đã xóa slot!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
    }
}