package com.lms.service.impl;

import com.lms.dto.notification.AnnouncementRequest;
import com.lms.dto.notification.NotificationContent;
import com.lms.dto.notification.NotificationResponse;
import com.lms.entity.CourseSection;
import com.lms.entity.Enrollment;
import com.lms.entity.Notification;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.NotificationType;
import com.lms.entity.enums.RoleName;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.NotificationRepository;
import com.lms.repository.UserRepository;
import com.lms.service.support.SectionAccessChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long LECTURER_ID = 2L;
    private static final Long OTHER_LECTURER_ID = 3L;
    private static final Long STUDENT_ID = 4L;
    private static final Long OTHER_STUDENT_ID = 5L;
    private static final Long SECTION_ID = 20L;
    private static final Long NOTIFICATION_ID = 80L;

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseSectionRepository sectionRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        SectionAccessChecker access = new SectionAccessChecker(userRepository, enrollmentRepository);
        service = new NotificationServiceImpl(notificationRepository, userRepository, sectionRepository,
                enrollmentRepository, access);
    }

    // ---------- helpers ----------

    private User user(Long id, boolean enabled, RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setId(id);
        user.setFullName("User " + id);
        user.setEnabled(enabled);
        user.setRoles(Set.of(role));
        return user;
    }

    private User asUser(Long id, RoleName roleName) {
        User user = user(id, true, roleName);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    private CourseSection section(User teacher) {
        CourseSection section = new CourseSection();
        section.setId(SECTION_ID);
        section.setTeacher(teacher);
        return section;
    }

    private CourseSection stubSection(User teacher) {
        CourseSection section = section(teacher);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(section));
        return section;
    }

    private NotificationContent content(Long senderId, Long sectionId, String eventKey) {
        return new NotificationContent(NotificationType.GRADE, "  Điểm đã công bố ", "Bài 1: 8.5", "/grades/1",
                senderId, sectionId, eventKey);
    }

    private Enrollment enrollment(Long studentId, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(user(studentId, true, RoleName.STUDENT));
        enrollment.setStatus(status);
        return enrollment;
    }

    @SuppressWarnings("unchecked")
    private List<Notification> savedNotifications() {
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private Notification notification(User recipient, boolean read) {
        Notification notification = new Notification();
        notification.setId(NOTIFICATION_ID);
        notification.setRecipient(recipient);
        notification.setTitle("Tiêu đề");
        notification.setType(NotificationType.SYSTEM);
        notification.setRead(read);
        return notification;
    }

    // ---------- notifyUser ----------

    @Test
    @DisplayName("Tạo thông báo cho đúng người nhận với đầy đủ thông tin, mặc định chưa đọc")
    void notifyUser_success() {
        User recipient = user(STUDENT_ID, true, RoleName.STUDENT);
        User sender = user(LECTURER_ID, true, RoleName.LECTURER);
        CourseSection section = stubSection(sender);
        when(userRepository.existsById(STUDENT_ID)).thenReturn(true);
        when(userRepository.findById(LECTURER_ID)).thenReturn(Optional.of(sender));
        when(notificationRepository.findRecipientIdsByEventKey(anyString(), anyCollection())).thenReturn(List.of());
        when(userRepository.findAllById(Set.of(STUDENT_ID))).thenReturn(List.of(recipient));

        boolean created = service.notifyUser(STUDENT_ID, content(LECTURER_ID, SECTION_ID, "GRADE_PUBLISHED:60"));

        assertThat(created).isTrue();
        List<Notification> saved = savedNotifications();
        assertThat(saved).hasSize(1);
        Notification notification = saved.get(0);
        assertThat(notification.getRecipient()).isSameAs(recipient);
        assertThat(notification.getSender()).isSameAs(sender);
        assertThat(notification.getSection()).isSameAs(section);
        assertThat(notification.getType()).isEqualTo(NotificationType.GRADE);
        assertThat(notification.getTitle()).isEqualTo("Điểm đã công bố");
        assertThat(notification.getMessage()).isEqualTo("Bài 1: 8.5");
        assertThat(notification.getLink()).isEqualTo("/grades/1");
        assertThat(notification.getEventKey()).isEqualTo("GRADE_PUBLISHED:60");
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    @DisplayName("Thông báo hệ thống không có người gửi hay lớp, không cần khóa sự kiện")
    void notifyUser_systemNotificationWithoutEventKey() {
        User recipient = user(STUDENT_ID, true, RoleName.STUDENT);
        when(userRepository.existsById(STUDENT_ID)).thenReturn(true);
        when(userRepository.findAllById(Set.of(STUDENT_ID))).thenReturn(List.of(recipient));

        boolean created = service.notifyUser(STUDENT_ID,
                new NotificationContent(NotificationType.SYSTEM, "Bảo trì", null, null, null, null, null));

        assertThat(created).isTrue();
        Notification notification = savedNotifications().get(0);
        assertThat(notification.getSender()).isNull();
        assertThat(notification.getSection()).isNull();
        assertThat(notification.getEventKey()).isNull();
        verify(notificationRepository, never()).findRecipientIdsByEventKey(any(), any());
    }

    @Test
    @DisplayName("Người nhận không tồn tại -> ResourceNotFoundException")
    void notifyUser_recipientNotFound() {
        when(userRepository.existsById(STUDENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.notifyUser(STUDENT_ID, content(null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Người nhận bị khóa tài khoản: bỏ qua, không tạo thông báo")
    void notifyUser_disabledRecipient() {
        when(userRepository.existsById(STUDENT_ID)).thenReturn(true);
        when(userRepository.findAllById(Set.of(STUDENT_ID))).thenReturn(List.of(user(STUDENT_ID, false, RoleName.STUDENT)));

        assertThat(service.notifyUser(STUDENT_ID, content(null, null, null))).isFalse();

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Người gây ra sự kiện không tự nhận thông báo của mình")
    void notifyUser_senderIsRecipient() {
        when(userRepository.existsById(LECTURER_ID)).thenReturn(true);

        assertThat(service.notifyUser(LECTURER_ID, content(LECTURER_ID, null, null))).isFalse();

        verify(userRepository, never()).findAllById(any());
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Cùng một sự kiện xử lý lại: không gửi trùng")
    void notifyUser_duplicateEventIgnored() {
        when(userRepository.existsById(STUDENT_ID)).thenReturn(true);
        when(notificationRepository.findRecipientIdsByEventKey("GRADE_PUBLISHED:60", Set.of(STUDENT_ID)))
                .thenReturn(List.of(STUDENT_ID));
        when(userRepository.findAllById(Set.of(STUDENT_ID))).thenReturn(List.of(user(STUDENT_ID, true, RoleName.STUDENT)));

        assertThat(service.notifyUser(STUDENT_ID, content(null, null, "GRADE_PUBLISHED:60"))).isFalse();

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Lớp học phần của thông báo không tồn tại -> ResourceNotFoundException")
    void notifyUser_sectionNotFound() {
        when(userRepository.existsById(STUDENT_ID)).thenReturn(true);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.notifyUser(STUDENT_ID, content(null, SECTION_ID, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Người gửi không tồn tại -> ResourceNotFoundException")
    void notifyUser_senderNotFound() {
        when(userRepository.existsById(STUDENT_ID)).thenReturn(true);
        when(userRepository.findById(LECTURER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.notifyUser(STUDENT_ID, content(LECTURER_ID, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- notifyUsers ----------

    @Test
    @DisplayName("Gửi nhiều người: bỏ qua người đã nhận, người bị khóa, id không tồn tại và null")
    void notifyUsers_mixedRecipients() {
        User fresh = user(10L, true, RoleName.STUDENT);
        User alreadyNotified = user(11L, true, RoleName.STUDENT);
        User disabled = user(12L, false, RoleName.STUDENT);
        when(notificationRepository.findRecipientIdsByEventKey(anyString(), anyCollection())).thenReturn(List.of(11L));
        when(userRepository.findAllById(Set.of(10L, 11L, 12L, 13L)))
                .thenReturn(List.of(fresh, alreadyNotified, disabled));

        int count = service.notifyUsers(Arrays.asList(10L, 11L, 12L, 13L, null, 10L), content(null, null, "EVT:1"));

        assertThat(count).isEqualTo(1);
        assertThat(savedNotifications()).extracting(n -> n.getRecipient().getId()).containsExactly(10L);
    }

    @Test
    @DisplayName("Danh sách người nhận rỗng: không làm gì")
    void notifyUsers_empty() {
        assertThat(service.notifyUsers(List.of(), content(null, null, "EVT:1"))).isZero();

        verify(userRepository, never()).findAllById(any());
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Người gửi được loại khỏi danh sách người nhận")
    void notifyUsers_excludesSender() {
        User sender = user(LECTURER_ID, true, RoleName.LECTURER);
        User student = user(STUDENT_ID, true, RoleName.STUDENT);
        when(userRepository.findById(LECTURER_ID)).thenReturn(Optional.of(sender));
        when(userRepository.findAllById(Set.of(STUDENT_ID))).thenReturn(List.of(student));

        int count = service.notifyUsers(List.of(LECTURER_ID, STUDENT_ID), content(LECTURER_ID, null, null));

        assertThat(count).isEqualTo(1);
        assertThat(savedNotifications()).extracting(n -> n.getRecipient().getId()).containsExactly(STUDENT_ID);
    }

    // ---------- notifySectionStudents ----------

    @Test
    @DisplayName("Gửi cho cả lớp: chỉ sinh viên đang theo học, gắn mã lớp vào thông báo")
    void notifySectionStudents_onlyEnrolled() {
        CourseSection section = stubSection(null);
        when(enrollmentRepository.findBySectionId(SECTION_ID)).thenReturn(List.of(
                enrollment(10L, EnrollmentStatus.ENROLLED),
                enrollment(11L, EnrollmentStatus.DROPPED),
                enrollment(12L, EnrollmentStatus.COMPLETED)));
        when(userRepository.findAllById(Set.of(10L))).thenReturn(List.of(user(10L, true, RoleName.STUDENT)));

        int count = service.notifySectionStudents(SECTION_ID,
                new NotificationContent(NotificationType.ASSIGNMENT, "Bài tập mới", "Nộp trước thứ Sáu", null, null,
                        null, "ASSIGNMENT_CREATED:30"));

        assertThat(count).isEqualTo(1);
        Notification notification = savedNotifications().get(0);
        assertThat(notification.getRecipient().getId()).isEqualTo(10L);
        assertThat(notification.getSection()).isSameAs(section);
        assertThat(notification.getEventKey()).isEqualTo("ASSIGNMENT_CREATED:30");
    }

    @Test
    @DisplayName("Gửi cho lớp không tồn tại -> ResourceNotFoundException")
    void notifySectionStudents_sectionNotFound() {
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.notifySectionStudents(SECTION_ID, content(null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- announceToSection ----------

    private void stubTwoStudents() {
        when(enrollmentRepository.findBySectionId(SECTION_ID)).thenReturn(List.of(
                enrollment(STUDENT_ID, EnrollmentStatus.ENROLLED),
                enrollment(OTHER_STUDENT_ID, EnrollmentStatus.ENROLLED)));
        when(userRepository.findAllById(Set.of(STUDENT_ID, OTHER_STUDENT_ID))).thenReturn(List.of(
                user(STUDENT_ID, true, RoleName.STUDENT), user(OTHER_STUDENT_ID, true, RoleName.STUDENT)));
    }

    @Test
    @DisplayName("Admin gửi thông báo lớp học cho mọi sinh viên đang theo học")
    void announce_admin() {
        User admin = asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);
        stubTwoStudents();

        int count = service.announceToSection(ADMIN_ID, SECTION_ID,
                new AnnouncementRequest("  Nghỉ học ngày mai ", " Lớp nghỉ vì thầy bận "));

        assertThat(count).isEqualTo(2);
        List<Notification> saved = savedNotifications();
        assertThat(saved).extracting(n -> n.getRecipient().getId()).containsExactlyInAnyOrder(STUDENT_ID, OTHER_STUDENT_ID);
        assertThat(saved).allSatisfy(n -> {
            assertThat(n.getType()).isEqualTo(NotificationType.ANNOUNCEMENT);
            assertThat(n.getTitle()).isEqualTo("Nghỉ học ngày mai");
            assertThat(n.getMessage()).isEqualTo("Lớp nghỉ vì thầy bận");
            assertThat(n.getSender()).isSameAs(admin);
            assertThat(n.getEventKey()).isNull();
        });
    }

    @Test
    @DisplayName("Giảng viên phụ trách gửi thông báo lớp học; mỗi lần gửi là một thông báo riêng (không chống trùng)")
    void announce_assignedLecturerTwice() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        stubSection(lecturer);
        stubTwoStudents();

        service.announceToSection(LECTURER_ID, SECTION_ID, new AnnouncementRequest("Nhắc nhở", "Nội dung"));
        service.announceToSection(LECTURER_ID, SECTION_ID, new AnnouncementRequest("Nhắc nhở", "Nội dung"));

        verify(notificationRepository, times(2)).saveAll(any());
        verify(notificationRepository, never()).findRecipientIdsByEventKey(any(), any());
    }

    @Test
    @DisplayName("Giảng viên không phụ trách gửi thông báo lớp -> ForbiddenException")
    void announce_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubSection(assigned);

        assertThatThrownBy(() -> service.announceToSection(OTHER_LECTURER_ID, SECTION_ID,
                new AnnouncementRequest("Giả mạo", "Nội dung")))
                .isInstanceOf(ForbiddenException.class);

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Sinh viên gửi thông báo lớp -> ForbiddenException")
    void announce_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSection(null);

        assertThatThrownBy(() -> service.announceToSection(STUDENT_ID, SECTION_ID,
                new AnnouncementRequest("Tiêu đề", "Nội dung")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Gửi thông báo cho lớp không tồn tại -> ResourceNotFoundException")
    void announce_sectionNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.announceToSection(ADMIN_ID, SECTION_ID,
                new AnnouncementRequest("Tiêu đề", "Nội dung")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Tài khoản bị khóa gửi thông báo lớp -> ForbiddenException")
    void announce_disabledUser() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(user(ADMIN_ID, false, RoleName.ADMIN)));

        assertThatThrownBy(() -> service.announceToSection(ADMIN_ID, SECTION_ID,
                new AnnouncementRequest("Tiêu đề", "Nội dung")))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------- xem danh sách, đếm chưa đọc ----------

    @Test
    @DisplayName("Người dùng chỉ xem thông báo của chính mình, mới nhất trước")
    void list_ownNotificationsOnly() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        User student = user(STUDENT_ID, true, RoleName.STUDENT);
        Page<Notification> page = new PageImpl<>(List.of(notification(student, false)));
        when(notificationRepository.findByRecipientId(any(Long.class), any(Pageable.class))).thenReturn(page);

        Page<NotificationResponse> result = service.listMyNotifications(STUDENT_ID, false, PageRequest.of(2, 10));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findByRecipientId(org.mockito.ArgumentMatchers.eq(STUDENT_ID), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).read()).isFalse();
    }

    @Test
    @DisplayName("Chỉ xem thông báo chưa đọc")
    void list_unreadOnly() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(notificationRepository.findByRecipientIdAndReadFalse(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listMyNotifications(STUDENT_ID, true, PageRequest.of(0, 20));

        verify(notificationRepository).findByRecipientIdAndReadFalse(org.mockito.ArgumentMatchers.eq(STUDENT_ID),
                any(Pageable.class));
        verify(notificationRepository, never()).findByRecipientId(any(), any());
    }

    @Test
    @DisplayName("Kích thước trang bị giới hạn 100 và sắp xếp do client gửi lên bị bỏ qua")
    void list_pageSizeCappedAndSortIgnored() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(notificationRepository.findByRecipientId(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listMyNotifications(STUDENT_ID, false, PageRequest.of(0, 500, Sort.by("recipient.password")));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findByRecipientId(org.mockito.ArgumentMatchers.eq(STUDENT_ID), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    @DisplayName("Không phân trang: dùng trang đầu 20 phần tử")
    void list_unpagedUsesDefault() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(notificationRepository.findByRecipientId(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listMyNotifications(STUDENT_ID, false, Pageable.unpaged());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findByRecipientId(org.mockito.ArgumentMatchers.eq(STUDENT_ID), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("Tài khoản bị khóa xem thông báo -> ForbiddenException")
    void list_disabledUser() {
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(user(STUDENT_ID, false, RoleName.STUDENT)));

        assertThatThrownBy(() -> service.listMyNotifications(STUDENT_ID, false, PageRequest.of(0, 10)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Đếm thông báo chưa đọc của chính người dùng")
    void countUnread() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(notificationRepository.countByRecipientIdAndReadFalse(STUDENT_ID)).thenReturn(7L);

        assertThat(service.countUnread(STUDENT_ID)).isEqualTo(7L);

        verify(notificationRepository, never()).countByRecipientIdAndReadFalse(OTHER_STUDENT_ID);
    }

    // ---------- đọc, đánh dấu đã đọc ----------

    @Test
    @DisplayName("Xem chi tiết thông báo của chính mình")
    void get_own() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification(student, false)));

        assertThat(service.getMyNotification(STUDENT_ID, NOTIFICATION_ID).id()).isEqualTo(NOTIFICATION_ID);
    }

    @Test
    @DisplayName("Xem thông báo của người khác -> ForbiddenException")
    void get_otherUsersForbidden() {
        asUser(OTHER_STUDENT_ID, RoleName.STUDENT);
        when(notificationRepository.findById(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification(user(STUDENT_ID, true, RoleName.STUDENT), false)));

        assertThatThrownBy(() -> service.getMyNotification(OTHER_STUDENT_ID, NOTIFICATION_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Đánh dấu thông báo của mình là đã đọc")
    void markAsRead_own() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        Notification notification = notification(student, false);
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

        NotificationResponse response = service.markAsRead(STUDENT_ID, NOTIFICATION_ID);

        assertThat(notification.isRead()).isTrue();
        assertThat(response.read()).isTrue();
    }

    @Test
    @DisplayName("Đánh dấu đã đọc lần nữa vẫn thành công (không lỗi)")
    void markAsRead_idempotent() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        Notification notification = notification(student, true);
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

        assertThat(service.markAsRead(STUDENT_ID, NOTIFICATION_ID).read()).isTrue();
    }

    @Test
    @DisplayName("Đánh dấu thông báo của người khác -> ForbiddenException, trạng thái giữ nguyên")
    void markAsRead_otherUsersForbidden() {
        asUser(OTHER_STUDENT_ID, RoleName.STUDENT);
        Notification notification = notification(user(STUDENT_ID, true, RoleName.STUDENT), false);
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> service.markAsRead(OTHER_STUDENT_ID, NOTIFICATION_ID))
                .isInstanceOf(ForbiddenException.class);

        assertThat(notification.isRead()).isFalse();
    }

    @Test
    @DisplayName("Đánh dấu thông báo không tồn tại -> ResourceNotFoundException")
    void markAsRead_notFound() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsRead(STUDENT_ID, NOTIFICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Đánh dấu tất cả đã đọc chỉ áp dụng cho thông báo của chính người dùng")
    void markAllAsRead() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(notificationRepository.markAllRead(STUDENT_ID)).thenReturn(5);

        assertThat(service.markAllAsRead(STUDENT_ID)).isEqualTo(5);

        verify(notificationRepository, never()).markAllRead(OTHER_STUDENT_ID);
    }

    @Test
    @DisplayName("Danh sách thông báo trả về DTO, không lộ Entity")
    void responsesAreDtos() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        Notification notification = notification(student, false);
        notification.setSender(user(LECTURER_ID, true, RoleName.LECTURER));
        notification.setSection(section(null));
        notification.setLink("/assignments/30");
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

        NotificationResponse response = service.getMyNotification(STUDENT_ID, NOTIFICATION_ID);

        assertThat(response.senderId()).isEqualTo(LECTURER_ID);
        assertThat(response.senderName()).isEqualTo("User 2");
        assertThat(response.sectionId()).isEqualTo(SECTION_ID);
        assertThat(response.link()).isEqualTo("/assignments/30");
        assertThat(new ArrayList<>(Arrays.asList(NotificationResponse.class.getRecordComponents())))
                .noneMatch(c -> c.getName().equals("recipient") || c.getName().equals("eventKey"));
    }
}
