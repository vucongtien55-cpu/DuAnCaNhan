package com.lms.dto.grade;

public enum FinalGradeStatus {
    /** Đủ điểm đã công bố ở mọi thành phần: đã tính và lưu điểm tổng kết. */
    COMPUTED,
    /** Còn thành phần chưa có điểm đã công bố: không có điểm tổng kết (không coi là 0). */
    INCOMPLETE,
    /** Điểm tổng kết đã công bố: giữ nguyên, không tính lại. */
    LOCKED
}
