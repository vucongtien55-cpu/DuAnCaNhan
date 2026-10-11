package org.example.ridehailing.banhangonline.repository;

import org.example.ridehailing.banhangonline.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetToken token where token.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    long countByUser_IdAndCreatedAtAfter(Long userId, LocalDateTime createdAfter);

    @Modifying
    @Query("update PasswordResetToken token set token.expiresAt = :expiredAt "
            + "where token.user.id = :userId and token.usedAt is null")
    void expireUnusedByUserId(@Param("userId") Long userId, @Param("expiredAt") LocalDateTime expiredAt);
}
