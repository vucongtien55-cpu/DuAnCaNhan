package com.lms.service;

import com.lms.dto.grade.GradeResponse;
import com.lms.dto.grade.GradeSubmissionRequest;
import com.lms.dto.grade.StudentGradeResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Nghiệp vụ điểm bài tập. {@code actorId} là người đã đăng nhập (bắt buộc).
 * <ul>
 *   <li>Admin và giảng viên phụ trách: chấm điểm từng bài nộp, công bố/hủy công bố, xem mọi điểm của lớp.</li>
 *   <li>Sinh viên: chỉ xem điểm chính thức của chính mình, và chỉ sau khi đã công bố.</li>
 * </ul>
 * Điểm lưu theo từng bài nộp (mỗi Submission tối đa một Grade). Điểm chính thức của sinh viên là điểm của bài nộp
 * được tính theo {@code Assignment.gradingPolicy} (LATEST, FIRST, MANUAL, HIGHEST). Điểm đã công bố không được sửa
 * cho đến khi hủy công bố.
 */
public interface GradeService {

    GradeResponse gradeSubmission(@NotNull Long actorId, @NotNull Long submissionId,
                                  @Valid @NotNull GradeSubmissionRequest request);

    GradeResponse getBySubmission(@NotNull Long actorId, @NotNull Long submissionId);

    /** Điểm của mọi bài nộp đã chấm trong bài tập (mọi sinh viên, mọi lần nộp). */
    List<GradeResponse> listByAssignment(@NotNull Long actorId, @NotNull Long assignmentId);

    /** Điểm chính thức (theo chính sách của bài tập) của một sinh viên, góc nhìn của giảng viên/Admin. */
    GradeResponse getOfficialGrade(@NotNull Long actorId, @NotNull Long assignmentId, @NotNull Long studentId);

    GradeResponse publishGrade(@NotNull Long actorId, @NotNull Long gradeId);

    GradeResponse unpublishGrade(@NotNull Long actorId, @NotNull Long gradeId);

    /** Công bố mọi điểm đã chấm nhưng chưa công bố của bài tập; trả về số điểm vừa công bố. */
    int publishAssignmentGrades(@NotNull Long actorId, @NotNull Long assignmentId);

    /** Sinh viên xem điểm chính thức của chính mình cho một bài tập. */
    StudentGradeResponse getMyGrade(@NotNull Long actorId, @NotNull Long assignmentId);

    /** Sinh viên xem điểm chính thức của chính mình cho mọi bài tập trong lớp. */
    List<StudentGradeResponse> listMyGrades(@NotNull Long actorId, @NotNull Long sectionId);
}
