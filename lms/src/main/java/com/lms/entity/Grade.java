package com.lms.entity;

import com.lms.entity.enums.GradeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Điểm của sinh viên trong một lớp học phần; có thể gắn với bài nộp hoặc lượt làm quiz. */
@Entity
@Table(name = "grades", uniqueConstraints = @UniqueConstraint(columnNames = "submission_id"))
@Getter
@Setter
@NoArgsConstructor
public class Grade extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_attempt_id")
    private QuizAttempt quizAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GradeType type;

    private String title;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private Double maxScore = 10.0;

    /** Trọng số (%) khi tính điểm tổng kết. */
    private Double weight;

    /** Nhận xét sinh viên được xem, chỉ khi điểm đã công bố. */
    @Column(columnDefinition = "TEXT")
    private String feedback;

    /** Ghi chú nội bộ của giảng viên/Admin, không bao giờ trả về cho sinh viên. */
    @Column(columnDefinition = "TEXT")
    private String internalNote;

    private LocalDateTime gradedAt = LocalDateTime.now();

    /** Sinh viên chỉ thấy điểm khi đã được công bố (published = true). */
    @Column(nullable = false)
    private boolean published = false;

    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private User publishedBy;
}
