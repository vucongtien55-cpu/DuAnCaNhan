package com.lms.service;

import com.lms.dto.course.CourseResponse;
import com.lms.dto.course.CreateCourseRequest;
import com.lms.dto.course.UpdateCourseRequest;
import com.lms.entity.enums.CourseStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Nghiệp vụ khóa học. {@code actorId} là người thực hiện:
 * chỉ ADMIN được tạo, sửa, xóa; các vai trò khác chỉ xem được khóa học ACTIVE.
 */
public interface CourseService {

    CourseResponse create(Long actorId, @Valid CreateCourseRequest request);

    CourseResponse getById(Long actorId, Long courseId);

    /** {@code status} có thể null (không lọc). Chỉ ADMIN được lọc theo trạng thái khác ACTIVE. */
    Page<CourseResponse> list(Long actorId, CourseStatus status, Pageable pageable);

    CourseResponse update(Long actorId, Long courseId, @Valid UpdateCourseRequest request);

    /** Chỉ xóa được khóa học chưa có lớp học phần; nếu đã có thì nên chuyển sang ARCHIVED. */
    void delete(Long actorId, Long courseId);
}
