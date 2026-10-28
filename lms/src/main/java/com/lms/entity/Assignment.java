package com.lms.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import com.lms.entity.enums.GradingPolicy;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
public class Assignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id")
    private CourseSection section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Đường dẫn tương đối của file đính kèm đề bài trên máy chủ (null nếu không có). */
    private String attachmentPath;

    private String attachmentFileName;

    private String attachmentContentType;

    private Long attachmentSize;

    /** Hạn nộp; nộp sau thời điểm này sẽ bị đánh dấu muộn. */
    private LocalDateTime dueDate;

    /** Số lần nộp tối đa cho mỗi sinh viên; null là không giới hạn. */
    private Integer maxSubmissions;

    @Column(nullable = false)
    private Double maxScore = 10.0;

    @Column(nullable = false)
    private boolean allowLateSubmission = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GradingPolicy gradingPolicy = GradingPolicy.LATEST;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL)
    private List<Submission> submissions = new ArrayList<>();
}
