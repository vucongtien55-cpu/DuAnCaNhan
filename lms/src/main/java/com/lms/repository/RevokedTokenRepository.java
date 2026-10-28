package com.lms.repository;

import com.lms.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByJti(String jti);

    /** Token đã hết hạn thì tự nó đã vô hiệu, không cần giữ bản ghi thu hồi. */
    @Modifying
    @Query("delete from RevokedToken r where r.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
