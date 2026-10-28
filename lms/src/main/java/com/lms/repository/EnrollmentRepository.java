package com.lms.repository;

import com.lms.entity.Enrollment;
import com.lms.entity.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findBySectionId(Long sectionId);

    Optional<Enrollment> findByStudentIdAndSectionId(Long studentId, Long sectionId);

    boolean existsByStudentIdAndSectionId(Long studentId, Long sectionId);

    /** Đếm sĩ số thực tế (chỉ tính sinh viên đang học) để kiểm tra sức chứa. */
    long countBySectionIdAndStatus(Long sectionId, EnrollmentStatus status);
}
