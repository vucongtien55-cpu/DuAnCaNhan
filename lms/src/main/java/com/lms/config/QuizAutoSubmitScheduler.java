package com.lms.config;

import com.lms.service.QuizAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Định kỳ chốt các lượt làm bài đã quá hạn mà sinh viên chưa nộp (ví dụ do mất kết nối). */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizAutoSubmitScheduler {

    private final QuizAttemptService quizAttemptService;

    @Scheduled(fixedDelayString = "${app.quiz.auto-submit-interval-ms:60000}",
            initialDelayString = "${app.quiz.auto-submit-interval-ms:60000}")
    public void autoSubmitExpiredAttempts() {
        try {
            int count = quizAttemptService.autoSubmitExpiredAttempts();
            if (count > 0) {
                log.info("Đã tự động chốt {} lượt làm bài quá hạn", count);
            }
        } catch (RuntimeException e) {
            log.error("Tự động chốt bài quá hạn thất bại", e);
        }
    }
}
