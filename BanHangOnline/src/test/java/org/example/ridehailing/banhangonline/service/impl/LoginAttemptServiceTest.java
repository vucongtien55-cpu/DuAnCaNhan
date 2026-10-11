package org.example.ridehailing.banhangonline.service.impl;

import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginAttemptServiceTest {

    @Test
    void locksAccountOnFifthConsecutiveFailedAttempt() {
        UserRepository repository = mock(UserRepository.class);
        User account = new User();
        account.setEnabled(true);
        account.setLocked(false);
        account.setFailedLoginAttempts(4);
        account.setLastFailedLoginAt(LocalDateTime.now());
        when(repository.findByEmailForUpdate("buyer@example.com")).thenReturn(Optional.of(account));
        when(repository.save(account)).thenReturn(account);

        boolean locked = new LoginAttemptService(repository).recordFailure("buyer@example.com");

        assertTrue(locked);
        assertEquals(5, account.getFailedLoginAttempts());
        assertTrue(account.getLocked());
        assertNotNull(account.getLockedAt());
    }

    @Test
    void successfulLoginResetsCounterWithoutUnlockingLockedAccount() {
        UserRepository repository = mock(UserRepository.class);
        User account = new User();
        account.setEnabled(true);
        account.setLocked(false);
        account.setFailedLoginAttempts(3);
        when(repository.findByIdForUpdate(9L)).thenReturn(Optional.of(account));
        when(repository.save(account)).thenReturn(account);

        boolean accepted = new LoginAttemptService(repository).recordSuccess(9L);

        assertTrue(accepted);
        assertEquals(0, account.getFailedLoginAttempts());
        assertFalse(account.getLocked());
    }

    @Test
    void neverResetsAnAccountWhileItIsLocked() {
        UserRepository repository = mock(UserRepository.class);
        User account = new User();
        account.setEnabled(true);
        account.setLocked(true);
        account.setFailedLoginAttempts(5);
        when(repository.findByIdForUpdate(9L)).thenReturn(Optional.of(account));

        boolean accepted = new LoginAttemptService(repository).recordSuccess(9L);

        assertFalse(accepted);
        assertEquals(5, account.getFailedLoginAttempts());
        assertTrue(account.getLocked());
    }
}
