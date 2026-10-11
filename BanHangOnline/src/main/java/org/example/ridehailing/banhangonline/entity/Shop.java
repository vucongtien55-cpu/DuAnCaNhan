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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cua_hang")
@Getter
@Setter
@NoArgsConstructor
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_cua_hang")
    private Long id;

    @Column(name = "ten_cua_hang", nullable = false, length = 150)
    private String name;

    @Column(name = "mo_ta", length = 2000)
    private String description;

    @Column(name = "dia_chi", length = 300)
    private String address;

    @Column(name = "so_dien_thoai", length = 20)
    private String phone;

    @Column(name = "dia_chi_email", length = 180)
    private String email;

    @Column(name = "url_bieu_trung")
    private String logoUrl;

    @Column(name = "trang_thai", nullable = false, length = 30)
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chu_so_huu_id", nullable = false)
    private User owner;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
