package org.example.ridehailing.banhangonline.service;

import org.example.ridehailing.banhangonline.entity.CustomerNote;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerNoteService {

    // Lấy lịch sử chăm sóc khách hàng trong một cửa hàng.
    List<CustomerNote> getCustomerHistory(Long actorUserId, Long shopId, Long customerId);

    // Lấy các ghi chú do principal hiện tại tạo trong shop.
    List<CustomerNote> getMyCreatedNotes(Long actorUserId, Long shopId);

    // Lấy các ghi chú được giao cho principal hiện tại trong shop.
    List<CustomerNote> getMyAssignedNotes(Long actorUserId, Long shopId);

    // Lấy lịch hẹn của cửa hàng trong khoảng thời gian.
    List<CustomerNote> getAppointments(Long actorUserId, Long shopId, LocalDateTime from, LocalDateTime to);

    // Ghi nhận hoạt động chăm sóc và người thực hiện.
    CustomerNote createNote(
            Long actorUserId,
            Long shopId,
            Long customerId,
            String note,
            LocalDateTime appointmentAt);
}
