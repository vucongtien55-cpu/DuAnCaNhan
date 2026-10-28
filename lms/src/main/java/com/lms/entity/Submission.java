package com.lms.entity;

import com.lms.entity.enums.SubmissionStatus;
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

/** Mỗi lần nộp là một bản ghi riêng (attemptNumber tăng dần) để giữ lịch sử các lần nộp. */
@Entity
@Table(name = "submissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "student_id", "attempt_number"}))
@Getter
@Setter
@NoArgsConstructor
public class Submission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber = 1;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 1000)
    private String filePath;

    private String originalFileName;

    private String contentType;

    private Long fileSize;

    private LocalDateTime submittedAt = LocalDateTime.now();

    /** Đặt khi nộp: true nếu submittedAt sau dueDate của bài tập. */
    @Column(nullable = false)
    private boolean late = false;

    /** Bài nộp được giảng viên chọn để chấm (dùng với GradingPolicy.MANUAL). */
    @Column(nullable = false)
    private boolean selectedForGrading = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;
}
