package com.novamart.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "danh_muc")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "danh_muc_cha_id")
    private Category parent;

    @Column(name = "ten_danh_muc", nullable = false, length = 120)
    private String name;

    @Column(name = "duong_dan_dinh_danh", unique = true, nullable = false, length = 150)
    private String slug;

    @Column(name = "mo_ta", length = 500)
    private String description;

    @Column(name = "dang_hoat_dong", nullable = false)
    private boolean active = true;
}