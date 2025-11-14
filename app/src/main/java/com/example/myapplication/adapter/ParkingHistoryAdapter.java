package com.example.myapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.ParkingHistory; // Import model

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ParkingHistoryAdapter extends RecyclerView.Adapter<ParkingHistoryAdapter.HistoryViewHolder> {

    private final List<ParkingHistory> historyList;
    private final boolean isAdmin;

    public ParkingHistoryAdapter(List<ParkingHistory> historyList, boolean isAdmin) {
        this.historyList = historyList;
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_parking_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        ParkingHistory history = historyList.get(position);
        holder.bind(history, isAdmin);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvLicensePlate, tvTime, tvFee, tvUserUid;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yy", Locale.getDefault());

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLicensePlate = itemView.findViewById(R.id.tv_history_license_plate);
            tvTime = itemView.findViewById(R.id.tv_history_time);
            tvFee = itemView.findViewById(R.id.tv_history_fee);
            tvUserUid = itemView.findViewById(R.id.tv_history_user_uid);
        }

        public void bind(ParkingHistory history, boolean isAdmin) {
            tvLicensePlate.setText(history.getBienSoXe());

            String startTime = (history.getStartTime() != null) ? sdf.format(history.getStartTime()) : "N/A";
            String endTime = (history.getEndTime() != null) ? sdf.format(history.getEndTime()) : "N/A";
            tvTime.setText(String.format("Vào: %s\nRa: %s", startTime, endTime));

            tvFee.setText(String.format(Locale.US, "Phí: %,d VND", history.getTotalFee()));

            if (isAdmin) {
                tvUserUid.setVisibility(View.VISIBLE);
                tvUserUid.setText("UID: " + history.getUserUid());
            } else {
                tvUserUid.setVisibility(View.GONE);
            }
        }
    }
}
