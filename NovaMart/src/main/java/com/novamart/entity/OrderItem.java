package com.novamart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "chi_tiet_don_hang")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_gian_hang_id", nullable = false)
    private ShopOrder shopOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_the_id")
    private ProductVariant variant;

    @Column(name = "ten_san_pham_lich_su", nullable = false, length = 200)
    private String productNameSnapshot;

    @Column(name = "ten_bien_the_lich_su", nullable = false, length = 150)
    private String variantSnapshot;

    @Column(name = "ma_sku_lich_su", nullable = false, length = 80)
    private String skuSnapshot;

    @Column(name = "don_gia", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "so_luong", nullable = false)
    private int quantity;

    @Column(name = "thanh_tien", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;
}