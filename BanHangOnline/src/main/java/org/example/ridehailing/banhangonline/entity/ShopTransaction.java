package org.example.ridehailing.banhangonline.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "giao_dich_cua_hang", uniqueConstraints = {
        @UniqueConstraint(name = "uk_shop_transaction_reference", columnNames = "ma_giao_dich")
})
@Getter
@Setter
@NoArgsConstructor
public class ShopTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_giao_dich_cua_hang")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cua_hang_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_cua_hang_id")
    private ShopOrder shopOrder;

    @Column(name = "loai_giao_dich", nullable = false, length = 30)
    private String type;

    @Column(name = "trang_thai", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "so_tien", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "ma_giao_dich", length = 120)
    private String transactionReference;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
