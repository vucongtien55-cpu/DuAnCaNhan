package com.lms.repository;

import com.lms.entity.QuizAttempt;
import com.lms.entity.enums.AttemptStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByQuizIdAndStudentId(Long quizId, Long studentId);

    List<QuizAttempt> findByQuizIdAndStudentIdOrderByAttemptNumberDesc(Long quizId, Long studentId);

    List<QuizAttempt> findByQuizId(Long quizId);

    long countByQuizIdAndStudentId(Long quizId, Long studentId);

    boolean existsByQuizId(Long quizId);

    Optional<QuizAttempt> findFirstByQuizIdAndStudentIdAndStatus(Long quizId, Long studentId, AttemptStatus status);

    /** Khóa dòng lượt làm bài: lưu câu trả lời, nộp bài và tự động chốt bài được xử lý tuần tự. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from QuizAttempt a where a.id = :id")
    Optional<QuizAttempt> findByIdForUpdate(@Param("id") Long id);

    @Query("select a.id from QuizAttempt a where a.status = :status and a.deadlineAt < :now")
    List<Long> findIdsByStatusAndDeadlineAtBefore(@Param("status") AttemptStatus status,
                                                  @Param("now") LocalDateTime now);
}
