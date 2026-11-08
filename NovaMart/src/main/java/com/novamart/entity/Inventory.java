package com.novamart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kho_hang")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_the_id", unique = true, nullable = false)
    private ProductVariant variant;

    @Column(name = "so_luong_kha_dung", nullable = false)
    private int quantityAvailable = 0;

    @Column(name = "so_luong_tamtinh_giucho", nullable = false)
    private int quantityReserved = 0;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}