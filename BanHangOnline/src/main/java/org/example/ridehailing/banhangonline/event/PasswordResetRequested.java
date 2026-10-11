package org.example.ridehailing.banhangonline.event;

public record PasswordResetRequested(String email, String rawToken) {
}
