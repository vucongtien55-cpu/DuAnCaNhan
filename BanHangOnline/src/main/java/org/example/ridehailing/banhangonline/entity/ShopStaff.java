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

import java.time.LocalDateTime;

@Entity
@Table(name = "nhan_vien_cua_hang", uniqueConstraints = {
        @UniqueConstraint(name = "uk_shop_staff_user", columnNames = {"cua_hang_id", "nguoi_dung_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ShopStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_nhan_vien_cua_hang")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cua_hang_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private User user;

    @Column(name = "vai_tro", nullable = false, length = 30)
    private String role = "STAFF";

    @Column(name = "dang_hoat_dong", nullable = false)
    private Boolean active = true;

    @Column(name = "ngay_tuyen_dung", nullable = false, updatable = false)
    private LocalDateTime hiredAt = LocalDateTime.now();
}
