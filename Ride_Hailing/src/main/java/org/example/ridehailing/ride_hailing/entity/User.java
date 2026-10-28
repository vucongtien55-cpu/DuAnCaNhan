package org.example.ridehailing.ride_hailing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 15)
    private String phoneNumber; // Số điện thoại (Tài khoản)

    @Column(nullable = false)
    private String password; // Mật khẩu mã hóa

    @Column(nullable = false, length = 100)
    private String fullName; // Họ và tên

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role; // CUSTOMER, DRIVER, ADMIN

    // --- Thông tin riêng cho Tài xế ---
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DriverStatus driverStatus; // OFFLINE, ONLINE, BUSY

    private Double currentLat; // Vĩ độ hiện tại
    private Double currentLng; // Kinh độ hiện tại

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.role == UserRole.DRIVER && this.driverStatus == null) {
            this.driverStatus = DriverStatus.OFFLINE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}