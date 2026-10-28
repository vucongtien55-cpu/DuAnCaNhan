package com.lms.repository;

import com.lms.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    Page<Notification> findByRecipientId(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndReadFalse(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    /** Trong số các người nhận, những ai đã có thông báo cho sự kiện này. */
    @Query("select n.recipient.id from Notification n "
            + "where n.eventKey = :eventKey and n.recipient.id in :recipientIds")
    List<Long> findRecipientIdsByEventKey(@Param("eventKey") String eventKey,
                                          @Param("recipientIds") Collection<Long> recipientIds);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.read = true where n.recipient.id = :recipientId and n.read = false")
    int markAllRead(@Param("recipientId") Long recipientId);
}
