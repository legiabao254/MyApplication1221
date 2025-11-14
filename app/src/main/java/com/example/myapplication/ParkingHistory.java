package com.example.myapplication;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class ParkingHistory {

    @Exclude // Annotation này để Firestore không lưu trường này vào document
    private String documentId;

    @PropertyName("user_uid")
    private String userUid;

    @PropertyName("bien_so_xe")
    private String bienSoXe;

    @PropertyName("slot_name")
    private String slotName;

    @PropertyName("status")
    private String status;

    @PropertyName("duration_hours")
    private long durationHours;

    @PropertyName("total_fee")
    private long totalFee;

    @PropertyName("startTime")
    @ServerTimestamp
    private Date startTime;

    @PropertyName("endTime")
    @ServerTimestamp
    private Date endTime;

    // --- Constructor rỗng bắt buộc cho Firestore ---
    public ParkingHistory() {}

    // --- Getters and Setters ---

    @Exclude // Getter cho trường bị loại trừ
    public String getDocumentId() {
        return documentId;
    }

    @Exclude // Setter cho trường bị loại trừ
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    // Các Getters và Setters còn lại...
    @PropertyName("user_uid")
    public String getUserUid() {
        return userUid;
    }
    @PropertyName("user_uid")
    public void setUserUid(String userUid) {
        this.userUid = userUid;
    }

    @PropertyName("bien_so_xe")
    public String getBienSoXe() {
        return bienSoXe;
    }
    @PropertyName("bien_so_xe")
    public void setBienSoXe(String bienSoXe) {
        this.bienSoXe = bienSoXe;
    }

    @PropertyName("slot_name")
    public String getSlotName() { return slotName; }
    @PropertyName("slot_name")
    public void setSlotName(String slotName) { this.slotName = slotName; }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("duration_hours")
    public long getDurationHours() { return durationHours; }
    @PropertyName("duration_hours")
    public void setDurationHours(long durationHours) { this.durationHours = durationHours; }

    @PropertyName("total_fee")
    public long getTotalFee() { return totalFee; }
    @PropertyName("total_fee")
    public void setTotalFee(long totalFee) { this.totalFee = totalFee; }

    @PropertyName("startTime")
    public Date getStartTime() { return startTime; }
    @PropertyName("startTime")
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    @PropertyName("endTime")
    public Date getEndTime() { return endTime; }
    @PropertyName("endTime")
    public void setEndTime(Date endTime) { this.endTime = endTime; }
}
