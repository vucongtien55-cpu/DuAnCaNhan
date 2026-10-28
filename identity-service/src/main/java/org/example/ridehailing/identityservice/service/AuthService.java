package org.example.ridehailing.identityservice.service;

import org.example.ridehailing.identityservice.dto.request.LoginRequest;
import org.example.ridehailing.identityservice.dto.request.RegisterRequest;
import org.example.ridehailing.identityservice.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}