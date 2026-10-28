package org.example.ridehailing.ride_hailing.dto.request;

import org.example.ridehailing.ride_hailing.entity.TripStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTripStatusRequest {

    @NotNull(message = "ID tài xế không được để trống")
    private Long driverId;

    @NotNull(message = "Trạng thái chuyến xe mới là bắt buộc")
    private TripStatus status;
}