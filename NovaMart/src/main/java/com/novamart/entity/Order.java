package com.novamart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "don_hang_tong")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_don_hang", unique = true, nullable = false, length = 30)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private User customer;

    @Column(name = "ten_nguoi_nhan", nullable = false, length = 150)
    private String recipientName;

    @Column(name = "so_dien_thoai_nhan", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "dia_chi_giao_hang", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "tong_tien_hang", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tong_giam_gia", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountTotal;

    @Column(name = "tong_phi_vanchuyen", nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingTotal;

    @Column(name = "tong_thanh_toan", nullable = false, precision = 19, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "trang_thai", nullable = false, length = 30)
    private String status; // PENDING, PROCESSING, PARTIALLY_COMPLETED, COMPLETED, CANCELLED

    @Column(name = "ghi_chu", length = 500)
    private String note;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}