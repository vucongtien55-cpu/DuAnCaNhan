package org.example.ridehailing.ride_hailing.entity;

// Trạng thái hoạt động của tài xế
public enum DriverStatus {
    OFFLINE,    // Tắt ứng dụng / Không sẵn sàng nhận chuyến
    ONLINE,     // Đang sẵn sàng nhận chuyến xe mới
    BUSY        // Đang thực hiện chuyến xe
}