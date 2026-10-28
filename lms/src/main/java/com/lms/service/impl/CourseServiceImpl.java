package com.lms.service.impl;

import com.lms.dto.course.CourseResponse;
import com.lms.dto.course.CreateCourseRequest;
import com.lms.dto.course.UpdateCourseRequest;
import com.lms.entity.Course;
import com.lms.entity.User;
import com.lms.entity.enums.CourseStatus;
import com.lms.entity.enums.RoleName;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseRepository;
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.UserRepository;
import com.lms.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Locale;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CourseResponse create(Long actorId, CreateCourseRequest request) {
        requireAdmin(actorId);
        String code = normalizeCode(request.code());
        if (courseRepository.existsByCode(code)) {
            throw new BusinessException("Mã khóa học đã tồn tại: " + code);
        }

        Course course = new Course();
        course.setCode(code);
        course.setTitle(request.title().trim());
        course.setDescription(request.description());
        course.setCredits(request.credits());
        course.setThumbnailUrl(request.thumbnailUrl());
        course.setStatus(CourseStatus.DRAFT);
        return CourseResponse.from(courseRepository.save(course));
    }

    @Override
    public CourseResponse getById(Long actorId, Long courseId) {
        boolean admin = isAdmin(findActor(actorId));
        Course course = findCourse(courseId);
        // Người không phải Admin không được biết đến khóa học chưa công bố hoặc đã lưu trữ.
        if (!admin && course.getStatus() != CourseStatus.ACTIVE) {
            throw new ResourceNotFoundException("Khóa học", courseId);
        }
        return CourseResponse.from(course);
    }

    @Override
    public Page<CourseResponse> list(Long actorId, CourseStatus status, Pageable pageable) {
        boolean admin = isAdmin(findActor(actorId));
        if (admin) {
            Page<Course> page = status == null
                    ? courseRepository.findAll(pageable)
                    : courseRepository.findByStatus(status, pageable);
            return page.map(CourseResponse::from);
        }
        if (status != null && status != CourseStatus.ACTIVE) {
            throw new ForbiddenException("Bạn chỉ được xem các khóa học đang hoạt động");
        }
        return courseRepository.findByStatus(CourseStatus.ACTIVE, pageable).map(CourseResponse::from);
    }

    @Override
    @Transactional
    public CourseResponse update(Long actorId, Long courseId, UpdateCourseRequest request) {
        requireAdmin(actorId);
        Course course = findCourse(courseId);
        String code = normalizeCode(request.code());
        if (!code.equals(course.getCode()) && courseRepository.existsByCode(code)) {
            throw new BusinessException("Mã khóa học đã tồn tại: " + code);
        }

        course.setCode(code);
        course.setTitle(request.title().trim());
        course.setDescription(request.description());
        course.setCredits(request.credits());
        course.setThumbnailUrl(request.thumbnailUrl());
        course.setStatus(request.status());
        return CourseResponse.from(course);
    }

    @Override
    @Transactional
    public void delete(Long actorId, Long courseId) {
        requireAdmin(actorId);
        Course course = findCourse(courseId);
        if (courseSectionRepository.existsByCourseId(courseId)) {
            throw new BusinessException(
                    "Khóa học đã có lớp học phần, không thể xóa. Hãy chuyển sang trạng thái ARCHIVED để giữ lịch sử");
        }
        courseRepository.delete(course);
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học", courseId));
    }

    private User findActor(Long actorId) {
        return userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", actorId));
    }

    private boolean isAdmin(User user) {
        return user.isEnabled() && user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN);
    }

    private void requireAdmin(Long actorId) {
        if (!isAdmin(findActor(actorId))) {
            throw new ForbiddenException("Chỉ Admin mới được quản lý khóa học");
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
