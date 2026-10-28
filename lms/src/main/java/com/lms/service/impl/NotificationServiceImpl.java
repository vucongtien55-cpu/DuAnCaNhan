package com.lms.service.impl;

import com.lms.dto.notification.AnnouncementRequest;
import com.lms.dto.notification.NotificationContent;
import com.lms.dto.notification.NotificationResponse;
import com.lms.entity.CourseSection;
import com.lms.entity.Notification;
import com.lms.entity.User;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.NotificationType;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.NotificationRepository;
import com.lms.repository.UserRepository;
import com.lms.service.NotificationService;
import com.lms.service.support.SectionAccessChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CourseSectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SectionAccessChecker access;

    // ---------- gửi thông báo (API cho service khác) ----------

    @Override
    @Transactional
    public boolean notifyUser(Long recipientId, NotificationContent content) {
        if (!userRepository.existsById(recipientId)) {
            throw new ResourceNotFoundException("Người nhận", recipientId);
        }
        return notifyUsers(Set.of(recipientId), content) > 0;
    }

    @Override
    @Transactional
    public int notifyUsers(Collection<Long> recipientIds, NotificationContent content) {
        Set<Long> targets = recipientIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (content.senderId() != null) {
            targets.remove(content.senderId());
        }
        if (targets.isEmpty()) {
            return 0;
        }

        CourseSection section = content.sectionId() == null ? null : sectionRepository.findById(content.sectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", content.sectionId()));
        User sender = content.senderId() == null ? null : userRepository.findById(content.senderId())
                .orElseThrow(() -> new ResourceNotFoundException("Người gửi", content.senderId()));

        // Chống trùng: bỏ qua những người đã có thông báo cho đúng sự kiện này.
        Set<Long> alreadyNotified = content.eventKey() == null
                ? Set.of()
                : new HashSet<>(notificationRepository.findRecipientIdsByEventKey(content.eventKey(), targets));

        List<Notification> created = new ArrayList<>();
        for (User recipient : userRepository.findAllById(targets)) {
            if (!recipient.isEnabled() || alreadyNotified.contains(recipient.getId())) {
                continue;
            }
            Notification notification = new Notification();
            notification.setRecipient(recipient);
            notification.setSender(sender);
            notification.setSection(section);
            notification.setType(content.type());
            notification.setTitle(content.title().trim());
            notification.setMessage(content.message());
            notification.setLink(content.link());
            notification.setEventKey(content.eventKey());
            notification.setRead(false);
            created.add(notification);
        }
        if (created.isEmpty()) {
            return 0;
        }
        notificationRepository.saveAll(created);
        return created.size();
    }

    @Override
    @Transactional
    public int notifySectionStudents(Long sectionId, NotificationContent content) {
        sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", sectionId));
        List<Long> studentIds = enrollmentRepository.findBySectionId(sectionId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
                .map(e -> e.getStudent().getId())
                .toList();
        return notifyUsers(studentIds, content.sectionId() == null ? content.withSection(sectionId) : content);
    }

    @Override
    @Transactional
    public int announceToSection(Long actorId, Long sectionId, AnnouncementRequest request) {
        User actor = access.findActiveUser(actorId);
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", sectionId));
        access.requireManager(actor, section);

        NotificationContent content = new NotificationContent(NotificationType.ANNOUNCEMENT,
                request.title().trim(), request.message().trim(), null, actor.getId(), sectionId, null);
        return notifySectionStudents(sectionId, content);
    }

    // ---------- thông báo của người dùng đang đăng nhập ----------

    @Override
    public Page<NotificationResponse> listMyNotifications(Long actorId, boolean unreadOnly, Pageable pageable) {
        User actor = access.findActiveUser(actorId);
        // Sắp xếp cố định theo thời gian tạo, không nhận tiêu chí sắp xếp từ bên ngoài.
        Pageable safe = pageable == null || pageable.isUnpaged()
                ? PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
                : PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalse(actor.getId(), safe)
                : notificationRepository.findByRecipientId(actor.getId(), safe);
        return page.map(NotificationResponse::from);
    }

    @Override
    public long countUnread(Long actorId) {
        User actor = access.findActiveUser(actorId);
        return notificationRepository.countByRecipientIdAndReadFalse(actor.getId());
    }

    @Override
    public NotificationResponse getMyNotification(Long actorId, Long notificationId) {
        User actor = access.findActiveUser(actorId);
        return NotificationResponse.from(findOwned(actor, notificationId));
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long actorId, Long notificationId) {
        User actor = access.findActiveUser(actorId);
        Notification notification = findOwned(actor, notificationId);
        notification.setRead(true);
        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(Long actorId) {
        User actor = access.findActiveUser(actorId);
        return notificationRepository.markAllRead(actor.getId());
    }

    private Notification findOwned(User actor, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Thông báo", notificationId));
        if (!notification.getRecipient().getId().equals(actor.getId())) {
            throw new ForbiddenException("Bạn không có quyền truy cập thông báo này");
        }
        return notification;
    }
}
