package com.lms.service.impl;

import com.lms.dto.section.CreateSectionRequest;
import com.lms.dto.section.SectionResponse;
import com.lms.dto.section.UpdateSectionInfoRequest;
import com.lms.dto.section.UpdateSectionRequest;
import com.lms.entity.Course;
import com.lms.entity.CourseSection;
import com.lms.entity.User;
import com.lms.entity.enums.CourseStatus;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.RoleName;
import com.lms.entity.enums.SectionStatus;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseRepository;
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.UserRepository;
import com.lms.service.CourseSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseSectionServiceImpl implements CourseSectionService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_SUFFIX_LENGTH = 6;
    private static final int MAX_CODE_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Map<SectionStatus, Set<SectionStatus>> TRANSITIONS = Map.of(
            SectionStatus.DRAFT, Set.of(SectionStatus.OPEN_REGISTRATION),
            SectionStatus.OPEN_REGISTRATION, Set.of(SectionStatus.DRAFT, SectionStatus.IN_PROGRESS),
            SectionStatus.IN_PROGRESS, Set.of(SectionStatus.ENDED),
            SectionStatus.ENDED, Set.of());

    /** Các trạng thái giảng viên phụ trách được tự chuyển lớp sang. */
    private static final Set<SectionStatus> LECTURER_TARGETS =
            Set.of(SectionStatus.IN_PROGRESS, SectionStatus.ENDED);

    private final CourseSectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public SectionResponse create(Long actorId, CreateSectionRequest request) {
        requireAdmin(actorId);
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học", request.courseId()));
        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new BusinessException("Không thể tạo lớp học phần cho khóa học đã lưu trữ");
        }
        validateDates(request.startDate(), request.endDate());
        User teacher = request.teacherId() == null ? null : findLecturer(request.teacherId());

        String code;
        if (request.sectionCode() == null) {
            code = generateUniqueCode(course);
        } else {
            code = normalizeCode(request.sectionCode());
            if (sectionRepository.existsBySectionCode(code)) {
                throw new BusinessException("Mã lớp đã tồn tại: " + code);
            }
        }

        CourseSection section = new CourseSection();
        section.setCourse(course);
        section.setTeacher(teacher);
        section.setSectionCode(code);
        section.setName(request.name().trim());
        section.setSemester(request.semester());
        section.setStartDate(request.startDate());
        section.setEndDate(request.endDate());
        section.setMaxStudents(request.maxStudents());
        section.setSchedule(request.schedule());
        section.setStatus(SectionStatus.DRAFT);
        return toResponse(sectionRepository.save(section));
    }

    @Override
    @Transactional
    public SectionResponse update(Long actorId, Long sectionId, UpdateSectionRequest request) {
        requireAdmin(actorId);
        CourseSection section = findSection(sectionId);
        requireNotEnded(section);
        validateDates(request.startDate(), request.endDate());

        long enrolled = countEnrolled(sectionId);
        if (request.maxStudents() < enrolled) {
            throw new BusinessException("Sức chứa (" + request.maxStudents()
                    + ") không được nhỏ hơn số sinh viên đang học (" + enrolled + ")");
        }

        section.setName(request.name().trim());
        section.setSemester(request.semester());
        section.setStartDate(request.startDate());
        section.setEndDate(request.endDate());
        section.setMaxStudents(request.maxStudents());
        section.setSchedule(request.schedule());
        return SectionResponse.from(section, enrolled);
    }

    @Override
    @Transactional
    public SectionResponse updateInfo(Long actorId, Long sectionId, UpdateSectionInfoRequest request) {
        User actor = findActiveActor(actorId);
        CourseSection section = findSection(sectionId);
        if (!isAdmin(actor) && !isAssignedLecturer(actor, section)) {
            throw new ForbiddenException("Bạn không phụ trách lớp học phần này");
        }
        requireNotEnded(section);

        section.setName(request.name().trim());
        section.setSchedule(request.schedule());
        return toResponse(section);
    }

    @Override
    public SectionResponse getById(Long actorId, Long sectionId) {
        User actor = findActiveActor(actorId);
        CourseSection section = findSection(sectionId);
        requireVisible(actor, section, sectionId);
        return toResponse(section);
    }

    @Override
    public SectionResponse getByCode(Long actorId, String sectionCode) {
        User actor = findActiveActor(actorId);
        String code = normalizeCode(sectionCode);
        CourseSection section = sectionRepository.findBySectionCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", code));
        requireVisible(actor, section, code);
        return toResponse(section);
    }

    @Override
    public Page<SectionResponse> list(Long actorId, Long courseId, SectionStatus status, Pageable pageable) {
        User actor = findActiveActor(actorId);
        Page<CourseSection> page;

        if (isAdmin(actor)) {
            if (courseId != null && status != null) {
                page = sectionRepository.findByCourseIdAndStatus(courseId, status, pageable);
            } else if (courseId != null) {
                page = sectionRepository.findByCourseId(courseId, pageable);
            } else if (status != null) {
                page = sectionRepository.findByStatus(status, pageable);
            } else {
                page = sectionRepository.findAll(pageable);
            }
        } else if (hasRole(actor, RoleName.LECTURER)) {
            page = status == null
                    ? sectionRepository.findByTeacherId(actor.getId(), pageable)
                    : sectionRepository.findByTeacherIdAndStatus(actor.getId(), status, pageable);
        } else {
            if (status != null && status != SectionStatus.OPEN_REGISTRATION) {
                throw new ForbiddenException("Bạn chỉ được xem các lớp đang mở đăng ký");
            }
            page = courseId == null
                    ? sectionRepository.findByStatus(SectionStatus.OPEN_REGISTRATION, pageable)
                    : sectionRepository.findByCourseIdAndStatus(courseId, SectionStatus.OPEN_REGISTRATION, pageable);
        }
        return page.map(this::toResponse);
    }

    @Override
    @Transactional
    public SectionResponse assignTeacher(Long actorId, Long sectionId, Long teacherId) {
        requireAdmin(actorId);
        CourseSection section = findSection(sectionId);
        requireNotEnded(section);
        section.setTeacher(findLecturer(teacherId));
        return toResponse(section);
    }

    @Override
    @Transactional
    public SectionResponse changeStatus(Long actorId, Long sectionId, SectionStatus target) {
        User actor = findActiveActor(actorId);
        CourseSection section = findSection(sectionId);

        boolean allowed = isAdmin(actor)
                || (isAssignedLecturer(actor, section) && LECTURER_TARGETS.contains(target));
        if (!allowed) {
            throw new ForbiddenException("Bạn không có quyền chuyển lớp học phần sang trạng thái " + target);
        }

        SectionStatus current = section.getStatus();
        if (!TRANSITIONS.get(current).contains(target)) {
            throw new BusinessException("Không thể chuyển trạng thái từ " + current + " sang " + target);
        }

        long enrolled = countEnrolled(sectionId);
        if (target == SectionStatus.OPEN_REGISTRATION) {
            if (section.getTeacher() == null) {
                throw new BusinessException("Cần gán giảng viên trước khi mở đăng ký");
            }
            if (section.getCourse().getStatus() != CourseStatus.ACTIVE) {
                throw new BusinessException("Chỉ mở đăng ký khi khóa học đang hoạt động");
            }
        }
        if (target == SectionStatus.DRAFT && enrolled > 0) {
            throw new BusinessException("Lớp đã có " + enrolled + " sinh viên đăng ký, không thể quay về DRAFT");
        }

        // Rời OPEN_REGISTRATION (sang IN_PROGRESS) đồng nghĩa đóng đăng ký:
        // việc đăng ký chỉ được chấp nhận khi lớp ở trạng thái OPEN_REGISTRATION.
        section.setStatus(target);
        return SectionResponse.from(section, enrolled);
    }

    @Override
    @Transactional
    public SectionResponse regenerateSectionCode(Long actorId, Long sectionId) {
        requireAdmin(actorId);
        CourseSection section = findSection(sectionId);
        if (section.getStatus() != SectionStatus.DRAFT && section.getStatus() != SectionStatus.OPEN_REGISTRATION) {
            throw new BusinessException("Chỉ đổi mã lớp khi lớp đang DRAFT hoặc OPEN_REGISTRATION");
        }
        section.setSectionCode(generateUniqueCode(section.getCourse()));
        return toResponse(section);
    }

    // ---------- helpers ----------

    private CourseSection findSection(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", sectionId));
    }

    private User findActiveActor(Long actorId) {
        User user = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", actorId));
        if (!user.isEnabled()) {
            throw new ForbiddenException("Tài khoản đã bị khóa");
        }
        return user;
    }

    private void requireAdmin(Long actorId) {
        if (!isAdmin(findActiveActor(actorId))) {
            throw new ForbiddenException("Chỉ Admin mới được thực hiện thao tác này");
        }
    }

    private User findLecturer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Giảng viên", userId));
        if (!user.isEnabled() || !hasRole(user, RoleName.LECTURER)) {
            throw new BusinessException("Người dùng " + userId + " không phải giảng viên hợp lệ");
        }
        return user;
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(r -> r.getName() == roleName);
    }

    private boolean isAdmin(User user) {
        return hasRole(user, RoleName.ADMIN);
    }

    private boolean isAssignedLecturer(User user, CourseSection section) {
        return hasRole(user, RoleName.LECTURER)
                && section.getTeacher() != null
                && section.getTeacher().getId().equals(user.getId());
    }

    /** Người không có quyền xem sẽ nhận "không tồn tại" để không lộ thông tin lớp. */
    private void requireVisible(User actor, CourseSection section, Object identifier) {
        boolean visible = isAdmin(actor)
                || isAssignedLecturer(actor, section)
                || (hasRole(actor, RoleName.STUDENT) && isVisibleToStudent(actor, section));
        if (!visible) {
            throw new ResourceNotFoundException("Lớp học phần", identifier);
        }
    }

    private boolean isVisibleToStudent(User student, CourseSection section) {
        return section.getStatus() == SectionStatus.OPEN_REGISTRATION
                || enrollmentRepository.findByStudentIdAndSectionId(student.getId(), section.getId())
                .map(e -> e.getStatus() != EnrollmentStatus.DROPPED)
                .orElse(false);
    }

    private void requireNotEnded(CourseSection section) {
        if (section.getStatus() == SectionStatus.ENDED) {
            throw new BusinessException("Lớp học phần đã kết thúc, không thể chỉnh sửa");
        }
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BusinessException("Ngày kết thúc không được trước ngày bắt đầu");
        }
    }

    private long countEnrolled(Long sectionId) {
        return enrollmentRepository.countBySectionIdAndStatus(sectionId, EnrollmentStatus.ENROLLED);
    }

    private SectionResponse toResponse(CourseSection section) {
        return SectionResponse.from(section, countEnrolled(section.getId()));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String generateUniqueCode(Course course) {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            StringBuilder suffix = new StringBuilder(CODE_SUFFIX_LENGTH);
            for (int i = 0; i < CODE_SUFFIX_LENGTH; i++) {
                suffix.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String code = course.getCode() + "-" + suffix;
            if (!sectionRepository.existsBySectionCode(code)) {
                return code;
            }
        }
        throw new BusinessException("Không thể sinh mã lớp duy nhất, vui lòng thử lại");
    }
}
