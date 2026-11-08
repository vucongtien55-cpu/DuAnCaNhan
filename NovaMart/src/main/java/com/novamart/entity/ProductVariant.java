package com.novamart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "bien_the_san_pham")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private Product product;

    @Column(name = "ma_sku", unique = true, nullable = false, length = 80)
    private String sku;

    @Column(name = "ten_bien_the", nullable = false, length = 150)
    private String variantName;

    @Column(name = "gia_ban", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "gia_niem_yet", precision = 19, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(name = "dang_ban", nullable = false)
    private boolean active = true;
}