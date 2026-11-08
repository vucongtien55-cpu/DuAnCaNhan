package com.novamart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "don_hang_gian_hang")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_tong_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gian_hang_id", nullable = false)
    private Shop shop;

    @Column(name = "ma_don_gian_hang", unique = true, nullable = false, length = 40)
    private String shopOrderCode;

    @Column(name = "tien_hang", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "giam_gia", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "phi_van_chuyen", nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "doanh_thu_nguoi_ban", nullable = false, precision = 19, scale = 2)
    private BigDecimal sellerAmount;

    @Column(name = "trang_thai", nullable = false, length = 30)
    private String status; // PENDING, CONFIRMED, PROCESSING, SHIPPING, DELIVERED, CANCELLED, RETURNED

    @Column(name = "ly_do_huy", length = 500)
    private String cancelReason;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}