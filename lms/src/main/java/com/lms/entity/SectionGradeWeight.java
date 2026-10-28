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

import java.math.BigDecimal;

/** Trọng số (phần trăm) của một thành phần đánh giá trong lớp học phần; tổng các trọng số của lớp phải bằng 100. */
@Entity
@Table(name = "section_grade_weights",
        uniqueConstraints = @UniqueConstraint(columnNames = {"section_id", "grade_type"}))
@Getter
@Setter
@NoArgsConstructor
public class SectionGradeWeight extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id")
    private CourseSection section;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_type", nullable = false, length = 20)
    private GradeType gradeType;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;
}
