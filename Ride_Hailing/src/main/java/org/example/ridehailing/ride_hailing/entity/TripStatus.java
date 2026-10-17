package org.example.ridehailing.ride_hailing.entity;

// Trạng thái vòng đời của Chuyến xe / Đơn giao hàng (State Machine)
public enum TripStatus {
    CREATED,        // Khách tạo yêu cầu chuyến
    SEARCHING,      // Hệ thống đang tìm / phát đề nghị tới tài xế
    ACCEPTED,       // Tài xế đã nhận chuyến
    ARRIVED_PICKUP, // Tài xế đã đến điểm đón / lấy hàng
    IN_PROGRESS,    // Đang di chuyển tới điểm trả / giao hàng
    COMPLETED,      // Chuyến xe hoàn thành thành công
    CANCELLED       // Chuyến xe bị hủy (bởi khách hoặc tài xế)
}
