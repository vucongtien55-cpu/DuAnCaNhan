package com.lms.dto.grade;

import java.time.LocalDateTime;

/**
 * Điểm tổng kết cho sinh viên. Khi chưa công bố chỉ có {@code published = false}; không có điểm,
 * thành phần hay thông tin nào khác.
 */
public record StudentFinalGradeResponse(
        Long sectionId,
        boolean published,
        Double score,
        Double maxScore,
        LocalDateTime publishedAt) {
}
