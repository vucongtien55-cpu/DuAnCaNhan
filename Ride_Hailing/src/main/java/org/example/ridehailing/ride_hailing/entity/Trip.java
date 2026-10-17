package org.example.ridehailing.ride_hailing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId; // ID khách hàng đặt chuyến

    private Long driverId; // ID tài xế nhận chuyến (null khi chưa ghép)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServiceType serviceType; // RIDE hoặc DELIVERY

    // --- Điểm đón ---
    @Column(nullable = false)
    private Double pickupLat;
    @Column(nullable = false)
    private Double pickupLng;
    @Column(nullable = false, length = 255)
    private String pickupAddress;

    // --- Điểm trả / Giao hàng ---
    @Column(nullable = false)
    private Double dropoffLat;
    @Column(nullable = false)
    private Double dropoffLng;
    @Column(nullable = false, length = 255)
    private String dropoffAddress;

    // --- Quãng đường & Chi phí ---
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    private Double surgeMultiplier; // Hệ số giá động (VD: 1.2, 1.5)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status;

    // --- Mốc thời gian ---
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TripStatus.CREATED;
        }
        if (this.surgeMultiplier == null) {
            this.surgeMultiplier = 1.0;
        }
    }
}