package com.lms.entity;

import com.lms.entity.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"recipient_id", "event_key"}))
@Getter
@Setter
@NoArgsConstructor
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private CourseSection section;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type = NotificationType.SYSTEM;

    /** Đường dẫn trên giao diện khi bấm vào thông báo. */
    private String link;

    /**
     * Khóa nhận diện sự kiện sinh ra thông báo (ví dụ "GRADE_PUBLISHED:60"). Mỗi người nhận chỉ có một thông báo
     * cho mỗi khóa, nên xử lý lại cùng một sự kiện không gửi trùng. Null: không chống trùng.
     */
    @Column(name = "event_key", length = 200)
    private String eventKey;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;
}
