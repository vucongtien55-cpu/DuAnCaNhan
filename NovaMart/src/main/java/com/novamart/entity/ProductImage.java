package com.novamart.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "anh_san_pham")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private Product product;

    @Column(name = "duong_dan_anh", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "la_anh_chinh", nullable = false)
    private boolean isPrimary = false;

    @Column(name = "thu_tu_hien_thi", nullable = false)
    private int displayOrder = 0;
}