package com.lms.service;

import com.lms.dto.submission.SubmissionFile;
import com.lms.dto.submission.SubmissionResponse;
import com.lms.dto.submission.SubmitRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Nghiệp vụ bài nộp. {@code actorId} là người đã đăng nhập (bắt buộc).
 * <ul>
 *   <li>Sinh viên đang theo học (ENROLLED): nộp bài, xem và tải bài nộp của chính mình.</li>
 *   <li>Admin và giảng viên phụ trách: xem toàn bộ lịch sử nộp, chọn bài nộp để chấm.</li>
 * </ul>
 * Mỗi lần nộp là một bản ghi riêng (không ghi đè); nộp sau hạn được đánh dấu {@code late}.
 * File tối đa 20 MB, chỉ PDF/DOCX/PPTX/XLSX/ZIP.
 */
public interface SubmissionService {

    SubmissionResponse submit(@NotNull Long actorId, @NotNull Long assignmentId,
                              @Valid SubmitRequest request, MultipartFile file);

    SubmissionResponse getById(@NotNull Long actorId, @NotNull Long submissionId);

    SubmissionFile download(@NotNull Long actorId, @NotNull Long submissionId);

    /** Toàn bộ bài nộp của một bài tập; chỉ Admin và giảng viên phụ trách. */
    List<SubmissionResponse> listByAssignment(@NotNull Long actorId, @NotNull Long assignmentId);

    /** Lịch sử nộp của một sinh viên (mới nhất trước); chính sinh viên đó, Admin hoặc giảng viên phụ trách. */
    List<SubmissionResponse> listHistory(@NotNull Long actorId, @NotNull Long assignmentId, @NotNull Long studentId);

    /** Chọn bài nộp để chấm; chỉ khi bài tập dùng chính sách MANUAL. Bài đã chọn trước đó được bỏ chọn. */
    SubmissionResponse selectForGrading(@NotNull Long actorId, @NotNull Long submissionId);

    /** Bài nộp sẽ được chấm của một sinh viên, xác định theo chính sách của bài tập. */
    SubmissionResponse getSubmissionToGrade(@NotNull Long actorId, @NotNull Long assignmentId,
                                            @NotNull Long studentId);
}
