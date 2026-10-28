package com.lms.entity.enums;

/** Cách chọn bài nộp được chấm khi sinh viên nộp nhiều lần. */
public enum GradingPolicy {
    /** Lần nộp mới nhất. */
    LATEST,
    /** Lần nộp đầu tiên. */
    FIRST,
    /** Giảng viên tự chọn một bài nộp. */
    MANUAL,
    /** Bài nộp có điểm cao nhất (điểm lưu ở Grade.submission); bằng điểm thì lấy lần nộp sau. */
    HIGHEST
}
