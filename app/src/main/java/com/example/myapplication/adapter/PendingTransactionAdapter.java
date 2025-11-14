
package com.example.myapplication.adapter;
// --- CÁC LỆNH IMPORT CẦN THIẾT ĐÃ ĐƯỢC THÊM VÀO ---
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R; // <-- SỬA LỖI 1: Import R class
import com.example.myapplication.Transaction; // <-- SỬA LỖI 2: Import model Transaction
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.NumberFormat;
import java.util.Locale;

public class PendingTransactionAdapter extends FirestoreRecyclerAdapter<Transaction, PendingTransactionAdapter.TransactionViewHolder> {

    private OnConfirmClickListener listener;

    public PendingTransactionAdapter(@NonNull FirestoreRecyclerOptions<Transaction> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull TransactionViewHolder holder, int position, @NonNull Transaction model) {
        holder.tvPaymentCode.setText(model.getPayment_code());
        holder.tvUserUid.setText("UID: " + model.getUser_uid());

        // Format tiền tệ
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvAmount.setText(currencyFormat.format(model.getAmount()));
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvPaymentCode, tvAmount, tvUserUid;
        Button btnConfirm;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPaymentCode = itemView.findViewById(R.id.tv_transaction_payment_code);
            tvAmount = itemView.findViewById(R.id.tv_transaction_amount);
            tvUserUid = itemView.findViewById(R.id.tv_transaction_user_uid);
            btnConfirm = itemView.findViewById(R.id.btn_confirm_deposit);

            // SỬA LỖI 4: Gán OnClickListener ở đây để nó chỉ được tạo một lần
            btnConfirm.setOnClickListener(v -> {
                int position = getBindingAdapterPosition(); // Sử dụng getBindingAdapterPosition() để an toàn hơn
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    // Lấy model chính xác tại vị trí click thông qua getItem()
                    listener.onConfirmClick(getSnapshots().getSnapshot(position), getItem(position));
                }
            });
        }
    }

    // Interface để xử lý sự kiện click
    public interface OnConfirmClickListener {
        void onConfirmClick(DocumentSnapshot documentSnapshot, Transaction transaction);
    }

    public void setOnConfirmClickListener(OnConfirmClickListener listener) {
        this.listener = listener;
    }
}
