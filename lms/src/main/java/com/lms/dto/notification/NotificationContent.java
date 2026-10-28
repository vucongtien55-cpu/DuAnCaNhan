package com.lms.dto.notification;

import com.lms.entity.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Nội dung thông báo mà các service khác truyền vào khi có sự kiện (giao bài tập, công bố điểm, ...).
 *
 * @param senderId  người gây ra sự kiện (null nếu do hệ thống); không bao giờ tự nhận thông báo của chính mình
 * @param sectionId lớp học phần liên quan (null nếu không có)
 * @param eventKey  khóa nhận diện sự kiện để chống gửi trùng, ví dụ {@code "ASSIGNMENT_CREATED:30"};
 *                  null nghĩa là không chống trùng
 */
public record NotificationContent(
        @NotNull NotificationType type,

        @NotBlank @Size(max = 255) String title,

        @Size(max = 2000) String message,

        @Size(max = 255) String link,

        Long senderId,

        Long sectionId,

        @Size(max = 200) String eventKey) {

    public NotificationContent withSection(Long sectionId) {
        return new NotificationContent(type, title, message, link, senderId, sectionId, eventKey);
    }
}
