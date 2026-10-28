package com.lms.service;

import com.lms.dto.assignment.AssignmentAttachment;
import com.lms.dto.assignment.AssignmentResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * File đính kèm đề bài (mỗi bài tập tối đa một file). {@code actorId} là người đã đăng nhập (bắt buộc).
 * <ul>
 *   <li>Admin và giảng viên phụ trách: tải lên (thay thế file cũ), gỡ file; lớp ENDED thì không thay đổi.</li>
 *   <li>Admin, giảng viên phụ trách và sinh viên đang theo học (ENROLLED): tải xuống.</li>
 * </ul>
 * File tối đa 20 MB, chỉ PDF/DOCX/PPTX/XLSX/ZIP, kiểm tra theo phần mở rộng và nội dung file.
 */
public interface AssignmentAttachmentService {

    AssignmentResponse upload(@NotNull Long actorId, @NotNull Long assignmentId, MultipartFile file);

    AssignmentAttachment download(@NotNull Long actorId, @NotNull Long assignmentId);

    AssignmentResponse remove(@NotNull Long actorId, @NotNull Long assignmentId);
}
