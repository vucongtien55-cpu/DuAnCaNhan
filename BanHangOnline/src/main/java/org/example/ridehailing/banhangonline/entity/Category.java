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
@Table(name = "danh_muc", uniqueConstraints = {
        @UniqueConstraint(name = "uk_category_slug", columnNames = "duong_dan")
})
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_danh_muc")
    private Long id;

    @Column(name = "ten_danh_muc", nullable = false, length = 120)
    private String name;

    @Column(name = "duong_dan", nullable = false, length = 160)
    private String slug;

    @Column(name = "mo_ta", length = 1000)
    private String description;

    @Column(name = "url_hinh_anh")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "danh_muc_cha_id")
    private Category parentCategory;

    @Column(name = "dang_hoat_dong", nullable = false)
    private Boolean active = true;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
