package com.lms.security;

import com.lms.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Định kỳ xóa bản ghi thu hồi của token đã hết hạn để bảng không phình ra. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RevokedTokenCleanupScheduler {

    private final AuthService authService;

    @Scheduled(fixedDelayString = "${app.security.jwt.cleanup-interval-ms:3600000}",
            initialDelayString = "${app.security.jwt.cleanup-interval-ms:3600000}")
    public void purge() {
        try {
            int removed = authService.purgeExpiredRevokedTokens();
            if (removed > 0) {
                log.info("Đã dọn {} bản ghi token hết hạn", removed);
            }
        } catch (RuntimeException e) {
            log.error("Dọn token hết hạn thất bại", e);
        }
    }
}
