package org.example.ridehailing.ride_hailing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long walletId; // ID ví tiền

    @Column(nullable = false)
    private Long driverId; // ID tài xế

    private Long tripId; // ID chuyến xe tương ứng (null nếu là nạp tiền)

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount; // Số tiền giao dịch (VNĐ)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType transactionType; // TOP_UP hoặc COMMISSION_DEDUCTION

    @Column(length = 255)
    private String description; // Mô tả giao dịch

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}