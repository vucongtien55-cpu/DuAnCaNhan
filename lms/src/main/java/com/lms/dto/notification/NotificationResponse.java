package com.lms.dto.notification;

import com.lms.entity.Notification;
import com.lms.entity.User;
import com.lms.entity.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        String link,
        Long sectionId,
        Long senderId,
        String senderName,
        boolean read,
        LocalDateTime createdAt) {

    public static NotificationResponse from(Notification notification) {
        User sender = notification.getSender();
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLink(),
                notification.getSection() == null ? null : notification.getSection().getId(),
                sender == null ? null : sender.getId(),
                sender == null ? null : sender.getFullName(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
