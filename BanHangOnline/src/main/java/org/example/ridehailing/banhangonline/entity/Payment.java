package org.example.ridehailing.banhangonline.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "thanh_toan")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_thanh_toan")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "phuong_thuc", nullable = false, length = 30)
    private String method;

    @Column(name = "trang_thai", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "so_tien", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "ma_giao_dich_ben_ngoai", length = 120)
    private String transactionReference;

    @Column(name = "ngay_thanh_toan")
    private LocalDateTime paidAt;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
