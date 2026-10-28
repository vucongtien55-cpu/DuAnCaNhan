package com.lms.repository;

import com.lms.entity.Quiz;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findBySectionId(Long sectionId);

    List<Quiz> findBySectionIdAndPublishedTrue(Long sectionId);

    /** Khóa dòng bài kiểm tra để các lượt bắt đầu làm bài được xử lý tuần tự (đếm số lần làm không chạy đua). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quiz q where q.id = :id")
    Optional<Quiz> findByIdForUpdate(@Param("id") Long id);
}
