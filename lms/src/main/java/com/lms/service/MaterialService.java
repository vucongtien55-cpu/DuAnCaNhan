package com.lms.service;

import com.lms.dto.material.MaterialFile;
import com.lms.dto.material.MaterialResponse;
import com.lms.dto.material.UploadMaterialRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Nghiệp vụ tài liệu học tập. {@code actorId} là người đã đăng nhập (bắt buộc).
 * <ul>
 *   <li>Admin: quản lý tài liệu của mọi lớp.</li>
 *   <li>Giảng viên phụ trách: tải lên, xóa, xem tài liệu của lớp mình.</li>
 *   <li>Sinh viên: chỉ xem và tải tài liệu của lớp đang theo học (ENROLLED).</li>
 * </ul>
 * File tối đa 20 MB, chỉ nhận PDF, DOCX, PPTX, XLSX, ZIP; kiểm tra theo phần mở rộng và nội dung file,
 * không tin Content-Type từ client. Database chỉ lưu metadata và đường dẫn tương đối.
 */
public interface MaterialService {

    MaterialResponse upload(@NotNull Long actorId, @NotNull Long sectionId,
                            @Valid @NotNull UploadMaterialRequest request, MultipartFile file);

    List<MaterialResponse> listBySection(@NotNull Long actorId, @NotNull Long sectionId);

    MaterialResponse getById(@NotNull Long actorId, @NotNull Long materialId);

    MaterialFile download(@NotNull Long actorId, @NotNull Long materialId);

    /** Xóa bản ghi và file. File chỉ bị xóa sau khi giao dịch xóa bản ghi đã commit. */
    void delete(@NotNull Long actorId, @NotNull Long materialId);
}
