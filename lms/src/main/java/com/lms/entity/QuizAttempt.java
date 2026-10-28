package com.lms.entity;

import com.lms.entity.enums.AttemptStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_attempts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"quiz_id", "student_id", "attempt_number"}))
@Getter
@Setter
@NoArgsConstructor
public class QuizAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(nullable = false)
    private Integer attemptNumber = 1;

    private LocalDateTime startedAt = LocalDateTime.now();

    /** Hạn chót do server tính lúc bắt đầu: nhỏ hơn của (bắt đầu + thời lượng) và thời điểm đóng bài kiểm tra. */
    @Column(nullable = false)
    private LocalDateTime deadlineAt;

    private LocalDateTime submittedAt;

    private Double score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAnswer> answers = new ArrayList<>();
}
