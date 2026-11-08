package com.novamart.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vai_tro")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ma_vai_tro", length = 30, unique = true, nullable = false)
    private RoleName name;

    @Column(name = "ten_vai_tro", length = 100, nullable = false)
    private String displayName;

    @Column(name = "mo_ta", length = 255)
    private String description;
}