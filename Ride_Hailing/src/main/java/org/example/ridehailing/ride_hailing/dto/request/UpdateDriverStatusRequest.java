package org.example.ridehailing.ride_hailing.dto.request;

import org.example.ridehailing.ride_hailing.entity.DriverStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDriverStatusRequest {

    @NotNull(message = "ID tài xế không được để trống")
    private Long driverId;

    @NotNull(message = "Trạng thái mới (ONLINE/OFFLINE/BUSY) là bắt buộc")
    private DriverStatus driverStatus;

    private Double currentLat;
    private Double currentLng;
}