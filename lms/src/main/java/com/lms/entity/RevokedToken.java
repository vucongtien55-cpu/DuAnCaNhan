package com.lms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Access token đã bị thu hồi (đăng xuất) trước khi hết hạn; chỉ cần giữ đến {@code expiresAt}. */
@Entity
@Table(name = "revoked_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RevokedToken extends BaseEntity {

    /** Định danh duy nhất của token (claim {@code jti}). */
    @Column(nullable = false, unique = true, length = 64)
    private String jti;

    @Column(nullable = false)
    private Instant expiresAt;
}
