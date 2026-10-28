package com.lms.dto.grade;

import com.lms.entity.enums.GradeType;

import java.math.BigDecimal;
import java.util.List;

/** Kết quả tính điểm tổng kết của một sinh viên, dành cho Admin và giảng viên phụ trách. */
public record FinalGradeResponse(
        Long enrollmentId,
        Long studentId,
        String studentName,
        FinalGradeStatus status,
        /** Điểm tổng kết thang 10; null khi INCOMPLETE. */
        Double score,
        Double maxScore,
        Long gradeId,
        boolean published,
        List<ComponentResult> components,
        /** Các thành phần có trọng số nhưng sinh viên chưa có điểm đã công bố. */
        List<GradeType> missingComponents) {

    /**
     * @param percent   tỷ lệ điểm đạt được của thành phần, 0-100, 2 chữ số thập phân
     * @param itemCount số bài tập/bài kiểm tra có điểm đã công bố được đưa vào thành phần
     */
    public record ComponentResult(GradeType type, BigDecimal weight, BigDecimal percent, int itemCount) {
    }
}
