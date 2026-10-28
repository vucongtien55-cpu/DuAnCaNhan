package com.lms.service.support;

import com.lms.entity.CourseSection;
import com.lms.entity.User;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.RoleName;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Quy tắc quyền truy cập theo lớp học phần dùng chung cho các service:
 * Admin và giảng viên phụ trách là người quản lý; sinh viên đang theo học (ENROLLED) là người đọc.
 */
@Component
@RequiredArgsConstructor
public class SectionAccessChecker {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    public User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));
        if (!user.isEnabled()) {
            throw new ForbiddenException("Tài khoản đã bị khóa");
        }
        return user;
    }

    public boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(r -> r.getName() == roleName);
    }

    public boolean isManager(User actor, CourseSection section) {
        boolean assignedLecturer = hasRole(actor, RoleName.LECTURER)
                && section.getTeacher() != null
                && section.getTeacher().getId().equals(actor.getId());
        return hasRole(actor, RoleName.ADMIN) || assignedLecturer;
    }

    public boolean isEnrolledStudent(User actor, CourseSection section) {
        return hasRole(actor, RoleName.STUDENT)
                && enrollmentRepository.findByStudentIdAndSectionId(actor.getId(), section.getId())
                .map(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
                .orElse(false);
    }

    public void requireManager(User actor, CourseSection section) {
        if (!isManager(actor, section)) {
            throw new ForbiddenException("Bạn không có quyền quản lý lớp học phần này");
        }
    }

    public void requireReader(User actor, CourseSection section) {
        if (!isManager(actor, section) && !isEnrolledStudent(actor, section)) {
            throw new ForbiddenException("Bạn không có quyền truy cập lớp học phần này");
        }
    }
}
