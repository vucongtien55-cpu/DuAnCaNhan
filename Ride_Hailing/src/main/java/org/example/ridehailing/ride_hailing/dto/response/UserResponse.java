package org.example.ridehailing.ride_hailing.dto.response;

import org.example.ridehailing.ride_hailing.entity.DriverStatus;
import org.example.ridehailing.ride_hailing.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String phoneNumber;
    private String fullName;
    private String email;
    private UserRole role;
    private DriverStatus driverStatus;
    private Double currentLat;
    private Double currentLng;
    private LocalDateTime createdAt;
}