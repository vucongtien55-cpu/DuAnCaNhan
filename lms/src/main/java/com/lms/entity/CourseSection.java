package com.lms.entity;

import com.lms.entity.enums.SectionStatus;
import jakarta.persistence.Column;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Lớp học phần: một lần mở của một khóa học, do một giảng viên phụ trách. */
@Entity
@Table(name = "course_sections")
@Getter
@Setter
@NoArgsConstructor
public class CourseSection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Column(nullable = false, unique = true, length = 40)
    private String sectionCode;

    private String name;

    private String semester;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer maxStudents;

    private String schedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SectionStatus status = SectionStatus.DRAFT;

    @OneToMany(mappedBy = "section")
    private List<Enrollment> enrollments = new ArrayList<>();
}
