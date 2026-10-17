package org.example.ridehailing.ride_hailing.service;

import org.example.ridehailing.ride_hailing.dto.request.LoginRequest;
import org.example.ridehailing.ride_hailing.dto.request.RegisterRequest;
import org.example.ridehailing.ride_hailing.dto.response.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    UserResponse login(LoginRequest request);
}