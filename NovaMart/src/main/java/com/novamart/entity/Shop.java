package com.novamart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gian_hang")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chu_so_huu_id", unique = true, nullable = false)
    private User owner;

    @Column(name = "ten_gian_hang", unique = true, nullable = false, length = 150)
    private String name;

    @Column(name = "duong_dan_dinh_danh", unique = true, nullable = false, length = 180)
    private String slug;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duong_dan_logo", length = 500)
    private String logoUrl;

    @Column(name = "so_dien_thoai_lien_he", length = 20)
    private String contactPhone;

    @Column(name = "trang_thai", nullable = false, length = 20)
    private String status; // PENDING, ACTIVE, REJECTED, SUSPENDED

    @Column(name = "ghi_chu_quan_tri", length = 500)
    private String adminNote;

    @Column(name = "ngay_phe_duyet")
    private LocalDateTime approvedAt;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}