package com.lms.service.impl;

import com.lms.dto.enrollment.EnrollmentResponse;
import com.lms.entity.CourseSection;
import com.lms.entity.Enrollment;
import com.lms.entity.User;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.RoleName;
import com.lms.entity.enums.SectionStatus;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.UserRepository;
import com.lms.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseSectionRepository sectionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public EnrollmentResponse enroll(Long actorId, String sectionCode) {
        User student = findActiveUser(actorId);
        if (!hasRole(student, RoleName.STUDENT)) {
            throw new ForbiddenException("Chỉ sinh viên mới được tự đăng ký lớp học phần");
        }
        String code = sectionCode.trim().toUpperCase(Locale.ROOT);
        CourseSection section = sectionRepository.findBySectionCodeForUpdate(code)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", code));

        if (section.getStatus() != SectionStatus.OPEN_REGISTRATION) {
            throw new BusinessException("Lớp học phần hiện không mở đăng ký");
        }

        Optional<Enrollment> existing = enrollmentRepository.findByStudentIdAndSectionId(student.getId(), section.getId());
        if (existing.isPresent()) {
            if (existing.get().getStatus() == EnrollmentStatus.DROPPED) {
                throw new BusinessException(
                        "Bạn đã rút khỏi lớp này, vui lòng liên hệ Admin hoặc giảng viên để được thêm lại");
            }
            throw new BusinessException("Bạn đã đăng ký lớp học phần này");
        }

        requireCapacity(section);
        return EnrollmentResponse.from(enrollmentRepository.save(newEnrollment(student, section)));
    }

    @Override
    @Transactional
    public EnrollmentResponse addStudent(Long actorId, Long sectionId, Long studentId) {
        User actor = findActiveUser(actorId);
        CourseSection section = findSectionForUpdate(sectionId);
        requireManager(actor, section);

        if (section.getStatus() != SectionStatus.OPEN_REGISTRATION && section.getStatus() != SectionStatus.IN_PROGRESS) {
            throw new BusinessException("Chỉ thêm sinh viên khi lớp đang OPEN_REGISTRATION hoặc IN_PROGRESS");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Sinh viên", studentId));
        if (!student.isEnabled() || !hasRole(student, RoleName.STUDENT)) {
            throw new BusinessException("Người dùng " + studentId + " không phải sinh viên hợp lệ");
        }

        Optional<Enrollment> existing = enrollmentRepository.findByStudentIdAndSectionId(studentId, sectionId);
        if (existing.isPresent() && existing.get().getStatus() != EnrollmentStatus.DROPPED) {
            throw new BusinessException("Sinh viên đã đăng ký lớp học phần này");
        }

        requireCapacity(section);

        if (existing.isPresent()) {
            // Khôi phục bản ghi đã rút: unique (student, section) không cho tạo dòng thứ hai.
            Enrollment enrollment = existing.get();
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            enrollment.setEnrolledAt(LocalDateTime.now());
            enrollment.setDroppedAt(null);
            enrollment.setDropReason(null);
            return EnrollmentResponse.from(enrollment);
        }
        return EnrollmentResponse.from(enrollmentRepository.save(newEnrollment(student, section)));
    }

    @Override
    @Transactional
    public EnrollmentResponse withdraw(Long actorId, Long sectionId, Long studentId, String reason) {
        User actor = findActiveUser(actorId);
        CourseSection section = findSectionForUpdate(sectionId);

        boolean self = actor.getId().equals(studentId);
        if (!self) {
            requireManager(actor, section);
        }

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndSectionId(studentId, sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Đăng ký học", studentId + "/" + sectionId));
        if (enrollment.getStatus() != EnrollmentStatus.ENROLLED) {
            throw new BusinessException("Chỉ rút được khi đang theo học (trạng thái hiện tại: "
                    + enrollment.getStatus() + ")");
        }
        if (section.getStatus() == SectionStatus.ENDED) {
            throw new BusinessException("Lớp học phần đã kết thúc, không thể rút");
        }

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollment.setDroppedAt(LocalDateTime.now());
        enrollment.setDropReason(reason == null || reason.isBlank() ? null : reason.trim());
        return EnrollmentResponse.from(enrollment);
    }

    @Override
    public List<EnrollmentResponse> listBySection(Long actorId, Long sectionId) {
        User actor = findActiveUser(actorId);
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", sectionId));
        requireManager(actor, section);
        return enrollmentRepository.findBySectionId(sectionId).stream().map(EnrollmentResponse::from).toList();
    }

    @Override
    public List<EnrollmentResponse> listMyEnrollments(Long actorId) {
        User student = findActiveUser(actorId);
        return enrollmentRepository.findByStudentId(student.getId()).stream().map(EnrollmentResponse::from).toList();
    }

    // ---------- helpers ----------

    private Enrollment newEnrollment(User student, CourseSection section) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setSection(section);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollment.setEnrolledAt(LocalDateTime.now());
        return enrollment;
    }

    /** Sĩ số chỉ tính sinh viên đang theo học; maxStudents null nghĩa là không giới hạn. */
    private void requireCapacity(CourseSection section) {
        if (section.getMaxStudents() == null) {
            return;
        }
        long enrolled = enrollmentRepository.countBySectionIdAndStatus(section.getId(), EnrollmentStatus.ENROLLED);
        if (enrolled >= section.getMaxStudents()) {
            throw new BusinessException("Lớp học phần đã đủ sĩ số (" + section.getMaxStudents() + ")");
        }
    }

    private CourseSection findSectionForUpdate(Long sectionId) {
        return sectionRepository.findByIdForUpdate(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", sectionId));
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));
        if (!user.isEnabled()) {
            throw new ForbiddenException("Tài khoản đã bị khóa");
        }
        return user;
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(r -> r.getName() == roleName);
    }

    /** Admin hoặc giảng viên được phân công vào chính lớp này. */
    private void requireManager(User actor, CourseSection section) {
        boolean admin = hasRole(actor, RoleName.ADMIN);
        boolean assignedLecturer = hasRole(actor, RoleName.LECTURER)
                && section.getTeacher() != null
                && section.getTeacher().getId().equals(actor.getId());
        if (!admin && !assignedLecturer) {
            throw new ForbiddenException("Bạn không có quyền quản lý đăng ký của lớp học phần này");
        }
    }
}
