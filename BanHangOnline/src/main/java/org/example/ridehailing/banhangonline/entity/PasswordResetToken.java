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
@Table(name = "ma_dat_lai_mat_khau", uniqueConstraints = {
        @UniqueConstraint(name = "uk_password_reset_token_hash", columnNames = "ma_bam")
})
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dat_lai")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private User user;

    @Column(name = "ma_bam", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "thoi_diem_het_han", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "thoi_diem_su_dung")
    private LocalDateTime usedAt;

    @Column(name = "thoi_diem_tao", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
