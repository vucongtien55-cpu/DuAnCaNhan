package com.novamart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "thanh_toan")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_tong_id", unique = true, nullable = false)
    private Order order;

    @Column(name = "phuong_thuc", nullable = false, length = 20)
    private String method; // COD, MOCK_ONLINE

    @Column(name = "trang_thai", nullable = false, length = 20)
    private String status; // PENDING, SUCCESS, FAILED, REFUNDED

    @Column(name = "so_tien", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "ngay_thanh_toan")
    private LocalDateTime paidAt;

    @Column(name = "ma_tham_chieu_nha_cung_cap", length = 120)
    private String providerReference;
}