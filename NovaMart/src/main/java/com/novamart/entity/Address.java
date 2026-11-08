package com.novamart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dia_chi")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private User user;

    @Column(name = "ten_nguoi_nhan", nullable = false, length = 150)
    private String recipientName;

    @Column(name = "so_dien_thoai_nhan", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "tinh_thanh", nullable = false, length = 100)
    private String province;

    @Column(name = "quan_huyen", nullable = false, length = 100)
    private String district;

    @Column(name = "phuong_xa", nullable = false, length = 100)
    private String ward;

    @Column(name = "chi_tiet", nullable = false, length = 255)
    private String detail;

    @Column(name = "la_mac_dinh", nullable = false)
    private boolean isDefault = false;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}