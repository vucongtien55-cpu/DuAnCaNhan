package com.lms.repository;

import com.lms.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByAssignmentId(Long assignmentId);

    List<Submission> findByStudentId(Long studentId);

    List<Submission> findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(Long assignmentId, Long studentId);

    Optional<Submission> findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(Long assignmentId, Long studentId);

    Optional<Submission> findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberAsc(Long assignmentId, Long studentId);

    long countByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    boolean existsByAssignmentId(Long assignmentId);

    /** Số lần nộp lớn nhất mà một sinh viên bất kỳ đã dùng cho bài tập (0 nếu chưa có bài nộp). */
    @Query("select coalesce(max(s.attemptNumber), 0) from Submission s where s.assignment.id = :assignmentId")
    int findMaxAttemptNumberByAssignmentId(@Param("assignmentId") Long assignmentId);
}
