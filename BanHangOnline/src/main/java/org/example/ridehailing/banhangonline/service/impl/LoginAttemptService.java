package org.example.ridehailing.banhangonline.service.impl;

import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository userRepository;

    public LoginAttemptService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordFailure(String normalizedEmail) {
        User user = userRepository.findByEmailForUpdate(normalizedEmail).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return false;
        }
        if (Boolean.TRUE.equals(user.getLocked())) {
            return true;
        }

        int failedAttempts = user.getFailedLoginAttempts() == null
                ? 0 : user.getFailedLoginAttempts();
        LocalDateTime now = LocalDateTime.now();
        if (user.getLastFailedLoginAt() == null
                || Duration.between(user.getLastFailedLoginAt(), now).compareTo(Duration.ofMinutes(15)) > 0) {
            failedAttempts = 0;
        }
        failedAttempts++;
        user.setFailedLoginAttempts(failedAttempts);
        user.setLastFailedLoginAt(now);
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            user.setLocked(true);
            user.setLockedAt(now);
        }
        userRepository.save(user);
        return Boolean.TRUE.equals(user.getLocked());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordSuccess(Long userId) {
        User user = userRepository.findByIdForUpdate(userId).orElse(null);
        if (user == null || Boolean.TRUE.equals(user.getLocked())
                || !Boolean.TRUE.equals(user.getEnabled())) {
            return false;
        }
        user.setFailedLoginAttempts(0);
        user.setLastFailedLoginAt(null);
        userRepository.save(user);
        return true;
    }
}
