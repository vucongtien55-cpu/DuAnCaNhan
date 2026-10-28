package com.lms.repository;

import com.lms.entity.CourseSection;
import com.lms.entity.enums.SectionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseSectionRepository extends JpaRepository<CourseSection, Long> {

    Optional<CourseSection> findBySectionCode(String sectionCode);

    /** Khóa dòng lớp học phần để kiểm tra sức chứa và ghi danh tuần tự, tránh vượt sĩ số khi đăng ký đồng thời. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CourseSection s where s.sectionCode = :code")
    Optional<CourseSection> findBySectionCodeForUpdate(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CourseSection s where s.id = :id")
    Optional<CourseSection> findByIdForUpdate(@Param("id") Long id);

    boolean existsBySectionCode(String sectionCode);

    List<CourseSection> findByCourseId(Long courseId);

    boolean existsByCourseId(Long courseId);

    List<CourseSection> findByTeacherId(Long teacherId);

    Page<CourseSection> findByCourseId(Long courseId, Pageable pageable);

    Page<CourseSection> findByStatus(SectionStatus status, Pageable pageable);

    Page<CourseSection> findByCourseIdAndStatus(Long courseId, SectionStatus status, Pageable pageable);

    Page<CourseSection> findByTeacherId(Long teacherId, Pageable pageable);

    Page<CourseSection> findByTeacherIdAndStatus(Long teacherId, SectionStatus status, Pageable pageable);
}
