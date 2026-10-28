package com.lms.service;

import com.lms.dto.assignment.AssignmentResponse;
import com.lms.dto.assignment.CreateAssignmentRequest;
import com.lms.dto.assignment.UpdateAssignmentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Nghiệp vụ bài tập. {@code actorId} là người đã đăng nhập (bắt buộc).
 * <ul>
 *   <li>Admin: quản lý bài tập mọi lớp. Giảng viên: chỉ lớp mình phụ trách.</li>
 *   <li>Sinh viên đang theo học (ENROLLED): chỉ xem.</li>
 *   <li>Lớp ENDED: không tạo, sửa, xóa. Đã có bài nộp: không xóa, không đổi điểm tối đa/chính sách chấm,
 *       không giảm số lần nộp xuống thấp hơn số lần đã dùng.</li>
 * </ul>
 */
public interface AssignmentService {

    AssignmentResponse create(@NotNull Long actorId, @Valid @NotNull CreateAssignmentRequest request);

    AssignmentResponse update(@NotNull Long actorId, @NotNull Long assignmentId,
                              @Valid @NotNull UpdateAssignmentRequest request);

    AssignmentResponse getById(@NotNull Long actorId, @NotNull Long assignmentId);

    List<AssignmentResponse> listBySection(@NotNull Long actorId, @NotNull Long sectionId);

    void delete(@NotNull Long actorId, @NotNull Long assignmentId);
}
