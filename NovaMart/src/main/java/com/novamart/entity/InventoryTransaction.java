package com.novamart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lich_su_kho_hang")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_the_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "so_luong_thay_doi", nullable = false)
    private int changeQuantity;

    @Column(name = "loai_giao_dich", nullable = false, length = 30)
    private String transactionType; // RESTOCK, RESERVE, RELEASE, SALE, ADJUST

    @Column(name = "loai_tham_chieu", length = 30)
    private String referenceType; // ORDER, MANUAL...

    @Column(name = "ma_tham_chieu_id")
    private Long referenceId;

    @Column(name = "ghi_chu", length = 255)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_thuc_hien_id")
    private User createdBy;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}