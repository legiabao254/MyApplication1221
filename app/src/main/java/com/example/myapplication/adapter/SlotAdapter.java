package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import java.util.List;
import android.graphics.Color; // Cần import Color

// Import static để dùng trực tiếp tên lớp Model (ParkingSlotData)
import static com.example.myapplication.ParkingSlotActivity.ParkingSlotData;

public class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.SlotViewHolder> {

    // Interface cho các sự kiện click
    public interface SlotListener {
        void onEdit(ParkingSlotData slot);
        void onDelete(ParkingSlotData slot);
    }

    private final List<ParkingSlotData> slots;
    private final SlotListener listener;
    private final boolean showAdminControls;

    public SlotAdapter(List<ParkingSlotData> slots, SlotListener listener, boolean showAdminControls) {
        this.slots = slots;
        this.listener = listener;
        this.showAdminControls = showAdminControls;
    }

    // ... (Hàm updateData giữ nguyên)

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slot, parent, false);
        return new SlotViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        final ParkingSlotData slot = slots.get(position);

        holder.bind(slot);

        // 1. HIỂN THỊ/ẨN NHÓM ADMIN CONTROLS
        // 🌟 ĐÃ SỬA LỖI: Ánh xạ 'adminControlsLayout' đã được thêm vào ViewHolder,
        // Logic hiển thị/ẩn là chính xác.
        holder.adminControlsLayout.setVisibility(showAdminControls ? View.VISIBLE : View.GONE);

        // 2. THIẾT LẬP LISTENER CHO CẢ 2 KỊCH BẢN
        if (showAdminControls) {
            holder.ivEdit.setOnClickListener(v -> listener.onEdit(slot));
            holder.ivDelete.setOnClickListener(v -> listener.onDelete(slot));
        } else {
            holder.itemView.setOnClickListener(v -> listener.onEdit(slot));
            holder.ivEdit.setOnClickListener(null);
            holder.ivDelete.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    // --- ViewHolder ---
    static class SlotViewHolder extends RecyclerView.ViewHolder {
        TextView tvSlotName, tvSlotStatus, tvSlotLoc;
        ImageView ivEdit, ivDelete;
        // 🌟 ĐÃ SỬA LỖI: Sử dụng ViewGroup hoặc LinearLayout tùy thuộc vào item_slot.xml
        // Giả sử adminControlsLayout là LinearLayout chứa ivEdit và ivDelete.
        LinearLayout adminControlsLayout;

        public SlotViewHolder(View itemView) {
            super(itemView);
            tvSlotName = itemView.findViewById(R.id.tvSlotName);
            tvSlotStatus = itemView.findViewById(R.id.tvSlotStatus);
            tvSlotLoc = itemView.findViewById(R.id.tvSlotLoc);
            ivEdit = itemView.findViewById(R.id.ivEdit);
            ivDelete = itemView.findViewById(R.id.ivDelete);
            // 🌟 ĐÃ SỬA LỖI: Đảm bảo ID này tồn tại trong item_slot.xml
            adminControlsLayout = itemView.findViewById(R.id.adminControlsLayout);
        }

        // Hàm bind đã được sửa để xử lý logic hiển thị trực tiếp
        public void bind(ParkingSlotData slot) {
            tvSlotName.setText(slot.getTen_slot());
            tvSlotLoc.setText("Vị trí: Tầng 1");

            // 🌟 SỬA LỖI: Thay thế các hàm không tồn tại bằng logic trực tiếp
            String status = slot.getTrang_thai();

            // Set text và color
            if (ParkingSlotData.STATUS_DANG_GUI.equals(status)) {
                tvSlotStatus.setText("Đang gửi");
                tvSlotStatus.setTextColor(Color.RED);
            } else if (ParkingSlotData.STATUS_TRONG.equals(status)) {
                tvSlotStatus.setText("Trống");
                tvSlotStatus.setTextColor(Color.parseColor("#4CAF50")); // Xanh lá
            } else if (ParkingSlotData.STATUS_BAO_TRI.equals(status)) {
                tvSlotStatus.setText("Bảo trì");
                tvSlotStatus.setTextColor(Color.GRAY);
            } else {
                tvSlotStatus.setText(status);
                tvSlotStatus.setTextColor(Color.BLACK);
            }
        }
    }
}