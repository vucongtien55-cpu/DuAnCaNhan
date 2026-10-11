package org.example.ridehailing.banhangonline.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "nguoi_dung")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_nguoi_dung")
    private Long id;

    @Column(name = "ho_ten", nullable = false, length = 120)
    private String fullName;

    @Column(name = "dia_chi_email", nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "mat_khau_ma_hoa", nullable = false)
    private String passwordHash;

    @Column(name = "vai_tro", nullable = false, length = 30)
    private String role = "CUSTOMER";

    @Column(name = "so_dien_thoai", length = 20)
    private String phone;

    @Column(name = "url_anh_dai_dien")
    private String avatarUrl;

    @Column(name = "dang_hoat_dong", nullable = false)
    private Boolean enabled = true;

    @Column(name = "so_lan_dang_nhap_sai")
    private Integer failedLoginAttempts = 0;

    @Column(name = "tai_khoan_bi_khoa")
    private Boolean locked = false;

    @Column(name = "thoi_diem_khoa")
    private LocalDateTime lockedAt;

    @Column(name = "thoi_diem_dang_nhap_sai_cuoi")
    private LocalDateTime lastFailedLoginAt;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ngay_cap_nhat", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
