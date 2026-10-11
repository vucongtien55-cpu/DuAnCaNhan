package org.example.ridehailing.banhangonline.service.impl;

import org.example.ridehailing.banhangonline.entity.AccountUnlockAudit;
import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.repository.AccountUnlockAuditRepository;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    @Test
    void adminUnlockResetsCounterAndWritesAuditRecord() {
        UserRepository users = mock(UserRepository.class);
        AccountUnlockAuditRepository audits = mock(AccountUnlockAuditRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        User administrator = user("ADMIN", false, 0);
        User lockedAccount = user("CUSTOMER", true, 5);
        lockedAccount.setLockedAt(LocalDateTime.now());
        when(users.findById(1L)).thenReturn(Optional.of(administrator));
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(lockedAccount));
        when(users.save(lockedAccount)).thenReturn(lockedAccount);

        User result = new UserServiceImpl(users, audits, encoder).unlockAccount(1L, 2L);

        assertFalse(result.getLocked());
        assertEquals(0, result.getFailedLoginAttempts());
        verify(audits).save(any(AccountUnlockAudit.class));
    }

    @Test
    void nonAdminCannotUnlockAccount() {
        UserRepository users = mock(UserRepository.class);
        AccountUnlockAuditRepository audits = mock(AccountUnlockAuditRepository.class);
        User customer = user("CUSTOMER", false, 0);
        when(users.findById(1L)).thenReturn(Optional.of(customer));

        UserServiceImpl service = new UserServiceImpl(users, audits, mock(PasswordEncoder.class));

        assertThrows(AccessDeniedException.class, () -> service.unlockAccount(1L, 2L));
        verifyNoInteractions(audits);
    }

    private User user(String role, boolean locked, int failedAttempts) {
        User user = new User();
        user.setId(role.equals("ADMIN") ? 1L : 2L);
        user.setRole(role);
        user.setEnabled(true);
        user.setLocked(locked);
        user.setFailedLoginAttempts(failedAttempts);
        return user;
    }
}
