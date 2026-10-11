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
@Table(name = "lich_su_mo_khoa_tai_khoan")
@Getter
@Setter
@NoArgsConstructor
public class AccountUnlockAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_lich_su_mo_khoa")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tai_khoan_id", nullable = false)
    private User account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_mo_khoa_id", nullable = false)
    private User unlockedBy;

    @Column(name = "thoi_diem_mo_khoa", nullable = false, updatable = false)
    private LocalDateTime unlockedAt = LocalDateTime.now();
}
