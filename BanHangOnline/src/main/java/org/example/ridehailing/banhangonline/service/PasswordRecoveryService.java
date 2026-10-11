package org.example.ridehailing.banhangonline.service;

public interface PasswordRecoveryService {

    void requestReset(String email);

    void resetPassword(String token, String newPassword);

    void changePassword(Long userId, String currentPassword, String newPassword);
}
