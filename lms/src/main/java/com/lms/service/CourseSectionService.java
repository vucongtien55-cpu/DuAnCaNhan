package com.lms.service;

import com.lms.dto.section.CreateSectionRequest;
import com.lms.dto.section.SectionResponse;
import com.lms.dto.section.UpdateSectionInfoRequest;
import com.lms.dto.section.UpdateSectionRequest;
import com.lms.entity.enums.SectionStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Nghiệp vụ lớp học phần. {@code actorId} là người thực hiện.
 * <ul>
 *   <li>ADMIN: tạo, sửa toàn bộ, gán giảng viên, đổi mã lớp, mọi chuyển trạng thái hợp lệ.</li>
 *   <li>Giảng viên được phân công: sửa tên/lịch học, chuyển lớp sang IN_PROGRESS hoặc ENDED.</li>
 *   <li>Sinh viên: chỉ xem lớp đang OPEN_REGISTRATION hoặc lớp mình đang theo học.</li>
 * </ul>
 */
public interface CourseSectionService {

    SectionResponse create(Long actorId, @Valid CreateSectionRequest request);

    /** Admin sửa toàn bộ thông tin, kể cả sức chứa (không nhỏ hơn số sinh viên đang học). */
    SectionResponse update(Long actorId, Long sectionId, @Valid UpdateSectionRequest request);

    /** Admin hoặc giảng viên phụ trách sửa tên và lịch học. */
    SectionResponse updateInfo(Long actorId, Long sectionId, @Valid UpdateSectionInfoRequest request);

    SectionResponse getById(Long actorId, Long sectionId);

    /** Tra cứu lớp bằng mã lớp (dùng khi sinh viên đăng ký). */
    SectionResponse getByCode(Long actorId, String sectionCode);

    /**
     * Admin: lọc theo {@code courseId}/{@code status}. Giảng viên: các lớp được phân công, chỉ lọc theo
     * {@code status}. Sinh viên: lớp đang mở đăng ký, lọc theo {@code courseId}.
     */
    Page<SectionResponse> list(Long actorId, Long courseId, SectionStatus status, Pageable pageable);

    SectionResponse assignTeacher(Long actorId, Long sectionId, Long teacherId);

    SectionResponse changeStatus(Long actorId, Long sectionId, SectionStatus target);

    /** Admin sinh mã lớp mới; chỉ khi lớp đang DRAFT hoặc OPEN_REGISTRATION. */
    SectionResponse regenerateSectionCode(Long actorId, Long sectionId);
}
