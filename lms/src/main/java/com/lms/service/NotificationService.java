package com.lms.service;

import com.lms.dto.notification.AnnouncementRequest;
import com.lms.dto.notification.NotificationContent;
import com.lms.dto.notification.NotificationResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

/**
 * Thông báo.
 *
 * <p><b>Cho các service khác gọi khi có sự kiện</b> ({@code notifyUser}, {@code notifyUsers},
 * {@code notifySectionStudents}): đây là API nội bộ, không kiểm tra quyền của người gọi; service gọi phải tự bảo đảm
 * sự kiện hợp lệ. Các phương thức tham gia giao dịch của service gọi, nên thông báo được ghi hoặc hoàn tác cùng sự kiện.
 * Truyền {@code eventKey} (ví dụ {@code "GRADE_PUBLISHED:60"}) để cùng một sự kiện xử lý lại không gửi trùng. Người
 * nhận bị khóa, không tồn tại (khi gửi nhiều người) hoặc chính là người gây ra sự kiện sẽ được bỏ qua.
 *
 * <p><b>Cho người dùng đang đăng nhập</b> ({@code actorId} bắt buộc): chỉ xem và đánh dấu đã đọc thông báo của chính
 * mình; Admin và giảng viên phụ trách gửi được thông báo lớp học.
 */
public interface NotificationService {

    /**
     * @return true nếu đã tạo thông báo; false nếu bị bỏ qua (trùng sự kiện, người nhận bị khóa, hoặc người nhận là
     * người gửi)
     */
    boolean notifyUser(@NotNull Long recipientId, @Valid @NotNull NotificationContent content);

    /** @return số thông báo thực sự được tạo */
    int notifyUsers(@NotNull Collection<Long> recipientIds, @Valid @NotNull NotificationContent content);

    /** Gửi cho mọi sinh viên đang theo học (ENROLLED) của lớp. @return số thông báo được tạo */
    int notifySectionStudents(@NotNull Long sectionId, @Valid @NotNull NotificationContent content);

    /** Admin hoặc giảng viên phụ trách gửi thông báo lớp học cho sinh viên đang theo học. @return số người nhận */
    int announceToSection(@NotNull Long actorId, @NotNull Long sectionId,
                          @Valid @NotNull AnnouncementRequest request);

    /** Mới nhất trước; kích thước trang tối đa 100. */
    Page<NotificationResponse> listMyNotifications(@NotNull Long actorId, boolean unreadOnly, Pageable pageable);

    long countUnread(@NotNull Long actorId);

    NotificationResponse getMyNotification(@NotNull Long actorId, @NotNull Long notificationId);

    NotificationResponse markAsRead(@NotNull Long actorId, @NotNull Long notificationId);

    /** @return số thông báo vừa được đánh dấu đã đọc */
    int markAllAsRead(@NotNull Long actorId);
}
