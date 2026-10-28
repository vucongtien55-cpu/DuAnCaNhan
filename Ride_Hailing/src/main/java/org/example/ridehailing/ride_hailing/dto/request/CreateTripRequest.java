package org.example.ridehailing.ride_hailing.dto.request;

import org.example.ridehailing.ride_hailing.entity.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTripRequest {

    @NotNull(message = "ID khách hàng không được để trống")
    private Long customerId;

    @NotNull(message = "Loại dịch vụ (RIDE hoặc DELIVERY) là bắt buộc")
    private ServiceType serviceType;

    // --- Tọa độ & Địa chỉ Điểm đón ---
    @NotNull(message = "Vĩ độ điểm đón không được để trống")
    private Double pickupLat;

    @NotNull(message = "Kinh độ điểm đón không được để trống")
    private Double pickupLng;

    @NotBlank(message = "Địa chỉ điểm đón không được để trống")
    private String pickupAddress;

    // --- Tọa độ & Địa chỉ Điểm trả / Giao ---
    @NotNull(message = "Vĩ độ điểm trả không được để trống")
    private Double dropoffLat;

    @NotNull(message = "Kinh độ điểm trả không được để trống")
    private Double dropoffLng;

    @NotBlank(message = "Địa chỉ điểm trả không được để trống")
    private String dropoffAddress;

    // --- Thông tin kiện hàng (Bắt buộc nếu serviceType là DELIVERY) ---
    private String senderName;
    private String senderPhone;
    private String receiverName;
    private String receiverPhone;
    private String packageName;
    private Double weightKg;
    private String noteForDriver;
}