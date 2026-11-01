package com.lms.controller;

import com.lms.dto.notification.AnnouncementRequest;
import com.lms.dto.notification.NotificationResponse;
import com.lms.security.CurrentUser;
import com.lms.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Chỉ công khai các thao tác của người dùng đang đăng nhập. Các hàm {@code notifyUser...} của service là API nội bộ
 * (không kiểm tra quyền người gọi) nên cố ý không có endpoint.
 */
@Tag(name = "Notifications")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    public record CountResponse(long count) {
    }

    @GetMapping("/notifications")
    public Page<NotificationResponse> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                           @ParameterObject Pageable pageable) {
        return notificationService.listMyNotifications(currentUser.id(), unreadOnly, pageable);
    }

    @GetMapping("/notifications/unread-count")
    public CountResponse unreadCount() {
        return new CountResponse(notificationService.countUnread(currentUser.id()));
    }

    @GetMapping("/notifications/{notificationId}")
    public NotificationResponse get(@PathVariable Long notificationId) {
        return notificationService.getMyNotification(currentUser.id(), notificationId);
    }

    @PutMapping("/notifications/{notificationId}/read")
    public NotificationResponse markAsRead(@PathVariable Long notificationId) {
        return notificationService.markAsRead(currentUser.id(), notificationId);
    }

    @PutMapping("/notifications/read-all")
    public CountResponse markAllAsRead() {
        return new CountResponse(notificationService.markAllAsRead(currentUser.id()));
    }

    /** Admin và giảng viên phụ trách gửi thông báo đến sinh viên đang theo học lớp. */
    @PostMapping("/sections/{sectionId}/announcements")
    public ResponseEntity<CountResponse> announce(@PathVariable Long sectionId,
                                                  @Valid @RequestBody AnnouncementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CountResponse(notificationService.announceToSection(currentUser.id(), sectionId, request)));
    }
}
