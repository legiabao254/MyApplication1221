package com.example.myapplication;

import java.util.regex.Pattern;

public class LicensePlateValidator {

    // Regex cho phép chữ, số và dấu gạch ngang. Yêu cầu ít nhất 1 chữ và 1 số.
    // Đây là regex chung để lọc các chuỗi ký tự vô nghĩa.
    private static final Pattern GENERAL_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*\\d)[A-Z0-9-]{7,10}$");

    /**
     * Kiểm tra biển số xe có hợp lệ không.
     * @param licensePlate Biển số cần kiểm tra (nên được chuyển thành chữ hoa và bỏ dấu cách).
     * @return true nếu hợp lệ, false nếu không.
     */
    public static boolean isValid(String licensePlate) {
        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            return false;
        }
        // Hiện tại chỉ cần kiểm tra một định dạng chung, dễ nâng cấp sau này
        return GENERAL_PATTERN.matcher(licensePlate).matches();
    }
}
