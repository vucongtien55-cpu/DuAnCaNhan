package org.example.ridehailing.banhangonline.repository;

import org.example.ridehailing.banhangonline.entity.CustomerNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerNoteRepository extends JpaRepository<CustomerNote, Long> {

    // Lấy lịch sử chăm sóc một khách tại cửa hàng, mới nhất trước.
    List<CustomerNote> findByShop_IdAndCustomer_IdOrderByCreatedAtDesc(Long shopId, Long customerId);

    // Lấy toàn bộ ghi chú chăm sóc của cửa hàng, mới nhất trước.
    List<CustomerNote> findByShop_IdOrderByCreatedAtDesc(Long shopId);

    // Lấy ghi chú do người dùng tạo trong shop đã được phân quyền.
    List<CustomerNote> findByShop_IdAndCreatedBy_IdOrderByCreatedAtDesc(Long shopId, Long createdById);

    // Lấy ghi chú gắn với nhân viên trong shop đã được phân quyền.
    List<CustomerNote> findByShop_IdAndStaff_IdOrderByCreatedAtDesc(Long shopId, Long staffId);

    // Lấy các lịch hẹn của cửa hàng trong khoảng thời gian, sớm nhất trước.
    List<CustomerNote> findByShop_IdAndAppointmentAtBetweenOrderByAppointmentAtAsc(
            Long shopId, LocalDateTime from, LocalDateTime to);
}
