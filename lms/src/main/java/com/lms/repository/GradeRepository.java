package com.lms.repository;

import com.lms.entity.Grade;
import com.lms.entity.enums.GradeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findByEnrollmentId(Long enrollmentId);

    Optional<Grade> findBySubmissionId(Long submissionId);

    Optional<Grade> findByQuizAttemptId(Long quizAttemptId);

    Optional<Grade> findByEnrollmentIdAndType(Long enrollmentId, GradeType type);

    /** Mọi điểm của một loại trong lớp học phần (ví dụ điểm tổng kết của cả lớp). */
    @Query("select g from Grade g where g.enrollment.section.id = :sectionId and g.type = :type")
    List<Grade> findBySectionIdAndType(@Param("sectionId") Long sectionId, @Param("type") GradeType type);

    /** Điểm của mọi lượt làm một bài kiểm tra. */
    @Query("select g from Grade g where g.quizAttempt.quiz.id = :quizId")
    List<Grade> findByQuizAttemptOfQuiz(@Param("quizId") Long quizId);

    /** Điểm của mọi bài nộp thuộc một bài tập (mọi sinh viên, mọi lần nộp). */
    @Query("select g from Grade g where g.submission.assignment.id = :assignmentId")
    List<Grade> findBySubmissionOfAssignment(@Param("assignmentId") Long assignmentId);

    /** Điểm sinh viên được phép xem. */
    List<Grade> findByEnrollmentIdAndPublishedTrue(Long enrollmentId);

    /** Điểm của từng bài nộp của một sinh viên trong một bài tập (mỗi Grade trỏ tới một Submission). */
    @Query("select g from Grade g where g.submission.assignment.id = :assignmentId "
            + "and g.submission.student.id = :studentId")
    List<Grade> findBySubmissionOfAssignmentAndStudent(@Param("assignmentId") Long assignmentId,
                                                       @Param("studentId") Long studentId);
}
