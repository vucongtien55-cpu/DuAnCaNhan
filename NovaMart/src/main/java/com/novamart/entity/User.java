package com.novamart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "nguoi_dung")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "mat_khau", nullable = false, length = 255)
    private String password;

    @Column(name = "ho_ten", nullable = false, length = 150)
    private String fullName;

    @Column(name = "so_dien_thoai", length = 20)
    private String phone;

    @Column(name = "duong_dan_anh_da_dien", length = 500)
    private String avatarUrl;

    @Column(name = "trang_thai", nullable = false, length = 20)
    private String status; // ACTIVE, LOCKED, PENDING

    @Column(name = "da_xac_minh_email", nullable = false)
    private boolean emailVerified = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "nguoi_dung_vai_tro",
            joinColumns = @JoinColumn(name = "nguoi_dung_id"),
            inverseJoinColumns = @JoinColumn(name = "vai_tro_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}