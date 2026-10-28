package com.lms.repository;

import com.lms.entity.Assignment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findBySectionId(Long sectionId);

    List<Assignment> findBySectionIdOrderByDueDateAsc(Long sectionId);

    /** Khóa dòng bài tập để nộp bài, sửa và xóa được xử lý tuần tự (đếm số lần nộp không bị chạy đua). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Assignment a where a.id = :id")
    Optional<Assignment> findByIdForUpdate(@Param("id") Long id);
}
