package com.novamart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "san_pham")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gian_hang_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "danh_muc_id", nullable = false)
    private Category category;

    @Column(name = "ten_san_pham", nullable = false, length = 200)
    private String name;

    @Column(name = "duong_dan_dinh_danh", nullable = false, length = 220)
    private String slug;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String description;

    @Column(name = "trang_thai", nullable = false, length = 20)
    private String status; // DRAFT, ACTIVE, INACTIVE, REJECTED

    @Column(name = "ghi_chu_kiemdut", length = 500)
    private String approvalNote;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "DRAFT";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}