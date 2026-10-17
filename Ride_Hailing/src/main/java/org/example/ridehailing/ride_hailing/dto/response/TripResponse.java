package org.example.ridehailing.ride_hailing.dto.response;

import org.example.ridehailing.ride_hailing.entity.ServiceType;
import org.example.ridehailing.ride_hailing.entity.TripStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TripResponse {

    private Long id;
    private Long customerId;
    private Long driverId;
    private ServiceType serviceType;

    private Double pickupLat;
    private Double pickupLng;
    private String pickupAddress;

    private Double dropoffLat;
    private Double dropoffLng;
    private String dropoffAddress;

    private BigDecimal distanceKm;
    private BigDecimal price;
    private Double surgeMultiplier;
    private TripStatus status;

    private LocalDateTime createdAt;

    // Đính kèm thông tin giao hàng nếu có
    private DeliveryInfoResponse deliveryInfo;

    @Getter
    @Builder
    public static class DeliveryInfoResponse {
        private String senderName;
        private String senderPhone;
        private String receiverName;
        private String receiverPhone;
        private String packageName;
        private Double weightKg;
        private String noteForDriver;
    }
}